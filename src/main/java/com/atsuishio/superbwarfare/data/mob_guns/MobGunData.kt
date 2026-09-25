package com.atsuishio.superbwarfare.data.mob_guns

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.CustomData
import com.atsuishio.superbwarfare.data.DataLoader
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.mob_guns.MobGunData.Companion.grant
import com.atsuishio.superbwarfare.data.mob_guns.MobGunData.Companion.onDataReload
import com.atsuishio.superbwarfare.data.mob_guns.MobGunData.Companion.restore
import com.atsuishio.superbwarfare.entity.goal.GunShootGoal
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.tools.TagDataParser
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import kotlinx.serialization.json.JsonObject
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.item.ItemStack
import net.minecraftforge.server.ServerLifecycleHooks
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * 一只持枪生物的持枪数据（按生物实例缓存）。
 *
 * 生命周期：
 * 1. 生成时 [grant]：按 `sbw/mob_guns` 的概率与权重抽一条 [GunSpawnData]，装备枪、把备弹写进
 *    生物身上的弹药池（[MobGunState]），并把这条配置的**键**登记在生物身上；
 * 2. 读档 / 换维度 / 区块重载时 [restore]：goal 不参与序列化，所以用登记键从**当前**数据包
 *    重新解析参数并重建 goal；
 * 3. `/reload` 时 [onDataReload]：重建索引并让已加载的生物立刻换上新参数。
 *
 * 之所以登记的是「键」而不是 [GunSpawnData] 本身：`Guns` 允许写多条同枪不同属性的配置，
 * 只凭手上的枪无法还原出抽中的是哪一条；而把参数快照存进生物 NBT 又会让 `/reload` 失效。
 */
class MobGunData private constructor(
    @JvmField val mob: Mob,
    @JvmField val data: DefaultMobGunData,
) {

    /**
     * 抽中（或还原出来）的一条配置。
     *
     * @param index 它在 `Guns` 列表中的下标
     * @param key 稳定标识，登记在生物身上用于还原
     * @param spawn 配置本体
     */
    data class Selection(
        @JvmField val index: Int,
        @JvmField val key: String,
        @JvmField val spawn: GunSpawnData,
    )

    /** 当前生效的配置；`null` 表示该生物没有可用配置（此时不应该有枪战 goal） */
    var selection: Selection? = null
        private set

    private var boundStack: ItemStack? = null
    private var boundGunData: GunData? = null

    /** 没有抽中配置时读取参数用的兜底值，避免到处判空 */
    private val fallback = GunSpawnData()

    private val spawn: GunSpawnData
        get() = selection?.spawn ?: fallback

    /** `Guns` 列表里每条配置的稳定身份，顺序与数据包一致 */
    private val entries: List<Selection> by lazy { buildEntries() }

    // ---------------------------------------------------------------- 参数

    val backupAmmo: Int get() = max(0, spawn.backupAmmo)
    val aimTime: Int get() = max(0, spawn.aimTime)
    val shootDistance: Double get() = spawn.shootDistance
    val semiFireInterval: Long get() = spawn.semiFireInterval.coerceAtLeast(0)
    val clearAimTimeWhenLostSight: Boolean get() = spawn.clearAimTimeWhenLostSight
    val zoom: Boolean get() = spawn.zoom
    val spread: Double get() = spawn.spread

    fun probability(): Double = Mth.clamp(data.probability, 0.0, 1.0)

    // ---------------------------------------------------------------- 抽取 / 还原

    /** 按权重抽一条配置；权重全部为 0 或列表为空时返回 `null` */
    fun roll(): Selection? {
        val list = entries
        val total = list.sumOf { max(0, it.spawn.weight) }
        if (total <= 0) return null

        // 求和与累加同口径（都按 >= 0 处理），否则负权重会让抽取落空
        var roll = mob.level().random.nextInt(total)
        for (entry in list) {
            roll -= max(0, entry.spawn.weight)
            if (roll < 0) return entry
        }
        return null
    }

    /** 生成时：登记配置键、初始化弹药池、装备枪械并重建 goal */
    fun equipNew(selection: Selection) {
        this.selection = selection
        MobGunState.setSelectionKey(mob, selection.key)
        MobGunState.setAmmo(mob, max(0, selection.spawn.backupAmmo))

        equip(selection)
        refreshGoal()
    }

    /**
     * 读档 / 传送后按登记键还原参数并重建 goal。
     *
     * 键失效时（数据包改动了 Guns 列表）退化为「按手持枪 id 找回唯一一条同类配置」，
     * 因此某个文件里只有一条该枪配置时，即使重排列表也不会错位。
     *
     * 这里**不会**给生物换枪：它可能已经被缴械、或者捡了别的东西，重新发一把会覆盖掉那些状态。
     * 数据包把某个键指向了另一把枪时，只有新生成的生物会拿到新枪。
     *
     * @param reapplyOverride 数据包重载时重写一遍属性覆写
     */
    fun bind(reapplyOverride: Boolean): Boolean {
        val key = MobGunState.selectionKey(mob)
        val heldId = gunIdOf(mob.mainHandItem)

        var resolved = entries.firstOrNull { it.key == key }
        if (resolved == null && heldId != null) {
            val sameGun = entries.filter { it.spawn.id == heldId }
            if (sameGun.size == 1) {
                resolved = sameGun.first()
                Mod.LOGGER.debug("Mob gun selection {} of {} is gone, matched {} by gun id", key, mob, resolved.key)
            }
        }

        if (resolved == null) {
            selection = null
            removeGunGoal(mob)
            return false
        }

        selection = resolved
        MobGunState.setSelectionKey(mob, resolved.key)

        // 只重写「确实是发出去的那把枪」的属性覆写，避免动到生物捡来的别人的枪
        if (reapplyOverride && heldId == resolved.spawn.id) {
            val gunData = GunData.from(mob.mainHandItem)
            applyOverride(gunData, resolved)
            gunData.save()
        }

        refreshGoal()
        return true
    }

    // ---------------------------------------------------------------- 枪械

    /**
     * 当前**手持物品**对应的 [GunData]。
     *
     * goal 一律通过它读写枪械状态，而不是用生成时缓存的另一份：生物中途换枪 / 被缴械后，
     * 参数与实际开火的物品仍然是同一个来源。
     */
    fun gunData(): GunData? {
        val stack = mob.mainHandItem
        if (stack.item !is GunItem) {
            boundStack = null
            boundGunData = null
            return null
        }

        if (boundGunData == null || boundStack !== stack) {
            boundStack = stack
            boundGunData = GunData.from(stack)
        }
        return boundGunData
    }

    private fun equip(selection: Selection) {
        val stack = buildStack(selection) ?: return

        // 先把物品 NBT 写完整（属性覆写、上膛）再入手：入手之后装备包就可能同步到客户端了
        val gunData = GunData.from(stack)
        applyOverride(gunData, selection)
        warnIfPoolTooSmall(selection, gunData)

        // 上膛的子弹从生物弹药池里扣：池子在前面已经写好了
        if (selection.spawn.spawnWithLoadedAmmo) {
            gunData.reloadAmmo(mob)
        }
        gunData.save()

        mob.setItemInHand(InteractionHand.MAIN_HAND, stack)
    }

    /**
     * 备弹池的口径和「主来源」一致：有弹匣的枪，池子是**发数**；没有弹匣的背包型武器
     * （典型是能量武器，如 `ql_1031` 每发 1000 FE）每发直接从池子里扣 `AmmoCostPerShoot`，
     * 量级会差好几个数量级；附加来源（如泰瑟枪的 `400 fe`）同样由池子代付。
     *
     * 池子不够一发时生物一枪都打不出来，看起来就像"备弹不识别"，所以这里提前把配置问题写进日志。
     */
    private fun warnIfPoolTooSmall(selection: Selection, gunData: GunData) {
        val pool = max(0, selection.spawn.backupAmmo)

        val perShot = (if (gunData.useBackpackAmmo()) gunData.primaryAmmoCostPerShoot() else 0) +
                gunData.selectedAmmoConsumer().extraAmmoCost()

        if (perShot > 0 && pool < perShot) {
            Mod.LOGGER.warn(
                "Mob gun '{}' for {}: BackupAmmo ({}) is smaller than what one shot needs ({}), the mob will never be able to shoot",
                selection.spawn.id, EntityType.getKey(mob.type), pool, perShot
            )
        }
    }

    private fun buildStack(selection: Selection): ItemStack? {
        val item = selection.spawn.gunItem()
        if (item == null) {
            Mod.LOGGER.warn(
                "Invalid mob gun '{}' configured for {}",
                selection.spawn.id, EntityType.getKey(mob.type)
            )
            return null
        }

        val stack = ItemStack(item)
        selection.spawn.data?.let {
            val tag = stack.getOrCreateTag()
            tag.merge(TagDataParser.parseObject(it))
        }
        return stack
    }

    /**
     * 写入属性覆写。
     *
     * 这里只写数据包里声明的内容：生物弹药池**不是**通过改弹种实现的（那样会让掉出去的枪
     * 带着一个只对生物有效的弹药来源，玩家捡到就打不响），而是作为「备弹」参与
     * [GunData.countBackupAmmo]，见 `MobGunState`。
     */
    private fun applyOverride(gunData: GunData, selection: Selection) {
        val json = selection.spawn.override?.let {
            DataLoader.JSON.encodeToString(JsonObject.serializer(), it)
        } ?: ""

        gunData.propertyOverrideString.set(json)
    }

    // ---------------------------------------------------------------- goal

    private fun refreshGoal() {
        removeGunGoal(mob)
        if (selection == null) return
        mob.goalSelector.addGoal(effectiveGoalPriority(), GunShootGoal(mob, this))
    }

    /**
     * 与生物原有 goal 抢同一个控制位时，谁先注册谁先跑，所以这里把优先级主动压到那些
     * 冲突 goal 之前，保证该开枪时不会被近战 goal 抢走移动 / 朝向控制。
     */
    private fun effectiveGoalPriority(): Int {
        val conflicting = mob.goalSelector.availableGoals
            .filter { it.goal !is GunShootGoal<*> && it.flags.any { flag -> flag in GUN_GOAL_FLAGS } }
            .minOfOrNull { it.priority }

        return min(data.goalPriority, (conflicting ?: Int.MAX_VALUE) - 1).coerceAtLeast(0)
    }

    private fun buildEntries(): List<Selection> {
        val list = ArrayList<Selection>()
        val ordinals = HashMap<String, Int>()

        data.guns.list.forEachIndexed { index, wrapper ->
            val spawn = wrapper.value

            // 同一种枪可以配多条（属性不同），因此默认键里带上同 id 条目的序号
            val ordinal = ordinals.getOrDefault(spawn.id, 0)
            ordinals[spawn.id] = ordinal + 1

            list += Selection(index, spawn.key.ifEmpty { "${spawn.id}#$ordinal" }, spawn)
        }

        return list
    }

    companion object {
        /** 持枪 goal 占用的控制位：与其它 MOVE/LOOK goal 互斥，避免一边举枪一边贴脸砍 */
        private val GUN_GOAL_FLAGS: EnumSet<Goal.Flag> = EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)

        private val dataCache: Cache<Mob, MobGunData> = CacheBuilder.newBuilder()
            .weakKeys()
            .weakValues()
            .build()

        /** 实体 id / 实体 tag -> 数据文件，随数据包重载重建 */
        private val byEntity = HashMap<String, DefaultMobGunData>()
        private val byTag = HashMap<String, DefaultMobGunData>()

        /** 取得该生物的持枪数据；没有任何数据文件命中时返回 `null` */
        @JvmStatic
        fun from(mob: Mob): MobGunData? {
            dataCache.getIfPresent(mob)?.let { return it }

            val data = resolveData(mob) ?: return null
            return MobGunData(mob, data).also { dataCache.put(mob, it) }
        }

        /**
         * 生物生成时抽取枪械并登记配置键。
         *
         * @return 是否真的发了枪
         */
        @JvmStatic
        fun grant(mob: Mob): Boolean {
            val data = from(mob) ?: return false

            val probability = data.probability()
            if (probability <= 0 || probability < mob.level().random.nextDouble()) return false

            val selection = data.roll() ?: return false
            data.equipNew(selection)
            return true
        }

        /**
         * 读档 / 传送 / 区块重载后按登记键还原参数与 goal。
         *
         * 数据文件已经不存在时**不动**这把枪（登记键保留，数据包改回来还能恢复），只移除 goal。
         */
        @JvmStatic
        @JvmOverloads
        fun restore(mob: Mob, reapplyOverride: Boolean = false): Boolean {
            if (MobGunState.selectionKey(mob) == null) return false

            val data = from(mob)
            if (data == null) {
                removeGunGoal(mob)
                return false
            }

            return data.bind(reapplyOverride)
        }

        /** 数据包重载：重建索引、失效缓存，并让已加载的生物立刻用上新数据 */
        @JvmStatic
        fun onDataReload(loaded: Map<String, Any>) {
            dataCache.invalidateAll()
            rebuildIndex(loaded)
            reapplyLoadedMobs()
        }

        private fun rebuildIndex(loaded: Map<String, Any>) {
            byEntity.clear()
            byTag.clear()

            for (value in loaded.values) {
                val data = value as? DefaultMobGunData ?: continue

                for (raw in data.entities.list) {
                    val key = raw.trim()
                    if (key.isEmpty()) continue

                    if (key.startsWith('#')) {
                        putIndex(byTag, key.substring(1).trim(), data)
                    } else {
                        putIndex(byEntity, key, data)
                    }
                }
            }
        }

        /**
         * 同一个键被多份文件声明时取 id 字典序最小的一份：`MOB_GUNS` 底下是 HashMap，
         * 迭代顺序不可靠，取「先到的」会让结果随加载顺序变化。
         */
        private fun putIndex(index: MutableMap<String, DefaultMobGunData>, key: String, data: DefaultMobGunData) {
            val previous = index[key]
            if (previous == null) {
                index[key] = data
                return
            }
            if (previous === data) return

            val kept = minOf(previous.getId(), data.getId())
            Mod.LOGGER.warn(
                "Duplicate mob gun key '{}' declared by {} and {}, keeping {}",
                key, previous.getId(), data.getId(), kept
            )
            if (data.getId() < previous.getId()) index[key] = data
        }

        private fun resolveData(mob: Mob): DefaultMobGunData? {
            val typeId = EntityType.getKey(mob.type).toString()
            byEntity[typeId]?.let { return it }

            // 实体 tag：多个 tag 都命中时按 tag 名排序取第一个，结果与数据包加载顺序无关
            val tags = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(mob.type).tags().iterator().asSequence()
                .map { it.location.toString() }
                .sorted()
                .toList()
            for (tag in tags) byTag[tag]?.let { return it }

            // 未声明 Entities 的旧写法：文件路径推导出的 id，只有 mod 自己的生物（superbwarfare:senpai）能这样命中
            return CustomData.MOB_GUNS[typeId]
        }

        /** `/reload` 后已加载的生物也要换上新参数：逐只重建 goal 并重写属性覆写 */
        private fun reapplyLoadedMobs() {
            val server = ServerLifecycleHooks.getCurrentServer() ?: return

            // 首次加载数据包时服务器可能还没建好世界，这里整体兜一下，别把重载带崩
            runCatching {
                for (level in server.allLevels) {
                    for (entity in level.allEntities) {
                        if (entity !is Mob || !entity.isAlive) continue
                        if (MobGunState.selectionKey(entity) == null) continue

                        runCatching { restore(entity, reapplyOverride = true) }
                            .onFailure { Mod.LOGGER.error("Failed to re-apply mob gun data to {}", entity, it) }
                    }
                }
            }.onFailure { Mod.LOGGER.error("Failed to re-apply mob gun data to loaded mobs", it) }
        }

        /** 移除该生物身上所有枪战 goal（重复注册会让同一生物出现多份开火逻辑） */
        @JvmStatic
        fun removeGunGoal(mob: Mob) {
            val goals = mob.goalSelector.availableGoals
                .filter { it.goal is GunShootGoal<*> }
                .map { it.goal }

            goals.forEach { mob.goalSelector.removeGoal(it) }
        }

        private fun gunIdOf(stack: ItemStack): String? {
            if (stack.item !is GunItem) return null
            return BuiltInRegistries.ITEM.getKey(stack.item).toString()
        }
    }
}
