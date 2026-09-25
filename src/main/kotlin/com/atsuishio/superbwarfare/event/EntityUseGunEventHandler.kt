package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.config.server.SpawnConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.mob_guns.*
import com.atsuishio.superbwarfare.item.gun.GunItem
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameRules
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.event.entity.living.LivingDropsEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = com.atsuishio.superbwarfare.Mod.MODID)
object EntityUseGunEventHandler {

    /**
     * 生物加入世界时决定「发枪 / 还原」。
     *
     * 三条分支：
     * 1. 身上已登记过配置键（读档、换维度、区块重载）：goal 不参与序列化，按登记的键还原参数并重建 goal；
     * 2. 从存档读出来的旧生物：不补发枪；
     * 3. 新生成的生物：按 `sbw/mob_guns` 抽取。
     */
    @SubscribeEvent
    fun entityJoin(event: EntityJoinLevelEvent) {
        val mob = event.entity as? Mob ?: return

        // 这个事件双端都会触发，而客户端的 loadedFromDisk() 恒为 false：
        // 不挡掉的话客户端会用本地随机数再抽一把枪、写一份自己的物品 NBT
        if (mob.level().isClientSide) return

        if (MobGunState.selectionKey(mob) != null) {
            // 注意这里不看 SPAWN_MOB_WITH_GUNS：那个开关只决定「要不要发新枪」，
            // 已经拿到枪的生物翻了开关也不该变成一个举着枪不会开火的呆子
            MobGunData.restore(mob)
            return
        }

        if (event.loadedFromDisk()) return
        if (!SpawnConfig.SPAWN_MOB_WITH_GUNS.get()) return

        MobGunData.grant(mob)
    }

    /**
     * 持枪生物的掉落由数据包逐条策略配置（[GunDropData]），服务器配置只作为兜底默认值。
     *
     * 优先级必须最高——`PowerfulAttraction` 与载具拾取同样在 `LivingDropsEvent` 里直接吞吐
     * `event.drops`，排在它们之后清洗等于没洗（弹药已经进玩家背包了）。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onLivingDrops(event: LivingDropsEvent) {
        val mob = event.entity as? Mob ?: return
        if (mob.level().isClientSide) return

        // 只有持枪生物（登记过配置键）才特殊处理，玩家的枪按原样掉落
        if (MobGunState.selectionKey(mob) == null) return

        // 登记过的生物手上的枪一律算它的配枪。枪的物品 NBT 里没有生物专用数据
        // （备弹在生物身上），所以这里不需要按具体枪 id 过滤
        fun isMobGun(stack: ItemStack) = stack.item is GunItem

        // 1. 原版那 8.5% 掉出来的枪先摘掉：内容与概率都改由配置决定
        val vanillaDrops = event.drops.filter { isMobGun(it.item) }
        if (vanillaDrops.isNotEmpty()) event.drops.removeAll(vanillaDrops.toSet())

        // 2. 掉落规则取这只生物实际抽中的那条策略；对应数据被删掉时用服务器配置兜底
        val drop = MobGunData.from(mob)?.selection?.spawn?.drop

        // 3. 是否必须玩家击杀
        if (drop.effectivePlayerKillOnly && !event.isRecentlyHit) return

        // 4. 关掉生物掉落时也不掉枪（原版装备掉落同样受这条规则约束）
        val level = mob.level() as? ServerLevel ?: return
        if (!level.gameRules.getBoolean(GameRules.RULE_DOMOBLOOT)) return

        // 5. 掉率
        val chance = drop.effectiveChance
        if (chance <= 0.0 || mob.level().random.nextDouble() >= chance) return

        // 原版掉落成功时枪已在 drops 里（就是同一份 stack），失败时还留在手上
        val stack = vanillaDrops.firstOrNull()?.item
            ?: mob.mainHandItem.takeIf { isMobGun(it) }
            ?: return

        sanitizeDrop(stack, drop)

        event.drops += ItemEntity(mob.level(), mob.x, mob.y + 0.5, mob.z, stack)
    }

    /**
     * 按这条策略的规则清洗掉落物。
     *
     * 生物的备弹保存在生物身上（[MobGunState]），所以枪里只剩弹匣内那点弹药；
     * `ClearAmmo` 打开时再把弹匣与溢出备弹一并清空，掉出来的就是一把空枪。
     */
    private fun sanitizeDrop(stack: ItemStack, drop: GunDropData?) {
        val gunData = GunData.from(stack)

        if (drop.effectiveStripOverride) {
            // 生物平衡用的属性覆写（例如 Damage: 1）不该跟着掉落物流到玩家手里
            gunData.propertyOverrideString.set("")
        }

        if (drop.effectiveClearAmmo) {
            gunData.ammo.set(0)
            gunData.virtualAmmo.set(0)
        }

        gunData.save()
    }
}
