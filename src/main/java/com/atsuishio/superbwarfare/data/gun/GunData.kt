package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.DefaultDataSupplier
import com.atsuishio.superbwarfare.data.JsonPropertyModifier
import com.atsuishio.superbwarfare.data.StringOrVec3
import com.atsuishio.superbwarfare.data.gun.subdata.*
import com.atsuishio.superbwarfare.data.gun.value.*
import com.atsuishio.superbwarfare.event.GunEventHandler
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModPerks
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.tools.InventoryTool
import com.atsuishio.superbwarfare.tools.invoke
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.energy.IEnergyStorage
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.registries.DeferredHolder
import java.util.*
import java.util.function.Function
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.min

class GunData private constructor(stack: ItemStack) : DefaultDataSupplier<DefaultGunData> {
    @JvmField
    val stack: ItemStack

    @JvmField
    val item: GunItem

    @JvmField
    val tag: CompoundTag

    @JvmField
    val gunDataTag: CompoundTag

    @JvmField
    val perkTag: CompoundTag

    @JvmField
    val attachmentTag: CompoundTag

    @JvmField
    val propertyOverrideString: StringValue

    @JvmField
    val id: String

    var defaultDataSupplier: Supplier<DefaultGunData>

    private fun getOrPut(name: String): CompoundTag {
        if (!this.tag.contains(name)) {
            this.tag.put(name, CompoundTag())
        }
        return this.tag.getCompound(name)
    }

    fun initialized(): Boolean {
        return item.isInitialized(this)
    }

    fun initialize() {
        item.init(this)
    }

    fun item() = item
    fun stack() = stack

    fun tag() = tag
    fun data() = gunDataTag
    fun perk() = perkTag
    fun attachment() = attachmentTag

    override fun getDefault() = this.defaultDataSupplier()

    fun setTempModifications(modification: Function<DefaultGunData, DefaultGunData>) {
        tempModifications = modification
        this.update()
    }

    fun clearTempModifications() {
        tempModifications = null
    }

    private val jsonPropModifier = JsonPropertyModifier<GunData, DefaultGunData>()

    private var cache: DefaultGunData? = null

    private var tempModifications: Function<DefaultGunData, DefaultGunData>? = null

    @JvmOverloads
    fun compute(useCache: Boolean = true): DefaultGunData {
        if (cache != null && useCache) return cache!!

        var rawData = default.copy()

        // property override tag
        jsonPropModifier.update(propertyOverrideString.get())
        rawData = jsonPropModifier.computeProperties(this, rawData)

        // gun modifiers
        rawData = item.computeProperties(this, rawData)

        // FireMode
        rawData = selectedFireModeInfo(rawData.availableFireModes()).computeProperties(this, rawData)

        // AmmoConsumer
        rawData = selectedAmmoConsumer(rawData.getAmmoConsumers()).computeProperties(this, rawData)

        // perk
        if (perk != null) {
            for (type in Perk.Type.entries.toTypedArray()) {
                val instance: Perk = perk.get(type) ?: continue

                rawData = instance.computeProperties(this, rawData)
            }
        }

        // 临时属性修改
        if (tempModifications != null) {
            rawData = tempModifications!!.apply(rawData)
        }

        rawData.limit()
        if (useCache) {
            cache = rawData
        }

        return rawData
    }

    fun update() {
        this.cache = null
    }

    /**
     * use compute() instead
     */
    @Deprecated("use compute() to get properties", ReplaceWith("compute()"))
    fun <T> get(prop: GunProp<T>): T {
        return prop.asModifier(this).compute(compute())
    }

    fun hasInfiniteBackupAmmo(shooter: Entity?): Boolean {
        return shooter is Player && shooter.isCreative
                || selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.INFINITE
                || meleeOnly()
                || InventoryTool.hasCreativeAmmoBox(shooter)
    }

    /**
     * 武器是否直接使用背包内弹药
     */
    fun useBackpackAmmo(): Boolean {
        return compute().magazine <= 0
    }

    // TODO 这什么b scope判断
    fun minZoom(): Double {
        val scopeType = this.attachment.get(AttachmentType.SCOPE)
        return if (scopeType == 3) max(default.minZoom, 1.25) else 1.25
    }

    // TODO 这什么b scope判断
    fun maxZoom(): Double {
        val scopeType = this.attachment.get(AttachmentType.SCOPE)
        return if (scopeType == 3) default.maxZoom else 114514.0
    }

    fun zoom(): Double {
        if (minZoom() >= maxZoom()) return compute().defaultZoom
        return Mth.clamp(compute().defaultZoom, minZoom(), maxZoom())
    }

    @JvmOverloads
    fun selectedAmmoConsumer(consumers: MutableList<AmmoConsumer>? = compute().getAmmoConsumers()): AmmoConsumer {
        if (consumers.isNullOrEmpty()) {
            return AmmoConsumer.INVALID
        }
        return consumers[this.selectedAmmoType.get().coerceIn(consumers.indices)]
    }

    fun changeAmmoConsumer(index: Int, ammoSupplier: Entity?) {
        val consumers = this.compute().getAmmoConsumers()
        val targetIndex = index.coerceIn(consumers.indices)
        if (targetIndex == selectedAmmoType.get()) return

        if (!(ammoSupplier is Player && ammoSupplier.isCreative)) {
            val currentConsumer = selectedAmmoConsumer()
            val targetConsumer = consumers[selectedAmmoType.get()]

            val currentSlot = currentConsumer.ammoSlot ?: "Default"
            val targetSlot = targetConsumer.ammoSlot ?: "Default"

            if (currentSlot == targetSlot && ammoSupplier != null && targetConsumer.shouldUnload) {
                this.withdrawAmmo(ammoSupplier)
            } else {
                val ammo = this.ammo.get()
                val virtualAmmo = this.virtualAmmo.get()
                this.ammoSlot.set(currentSlot, ammo, virtualAmmo)

                this.ammo.set(this.ammoSlot.getAmmo(targetSlot))
                this.virtualAmmo.set(this.ammoSlot.getVirtualAmmo(targetSlot))
                this.ammoSlot.reset(targetSlot)
            }
        }

        this.selectedAmmoType.set(targetIndex)

        if (ammoSupplier is Player && ammoSupplier.isCreative) {
            this.ammo.set(this.compute().magazine)
        }

        this.item.whenNoAmmo(this)
        this.closeHammer.set(false)
        this.fireIndex.reset()

        resetStatus()
    }

    fun resetStatus() {
        this.reload.stage.reset()
        this.reload.setState(ReloadState.NOT_RELOADING)
        this.reload.iterativeLoadTimer.reset()
        this.reload.reloadTimer.reset()
        this.reload.finishTimer.reset()
        this.reload.prepareTimer.reset()
        this.reload.prepareLoadTimer.reset()
        this.reload.reloadStarter.finish()
        this.reload.singleReloadStarter.finish()
        this.reload.singleReloadStarter.finish()
        this.bolt.actionTimer.reset()
        this.bolt.needed.reset()
        this.charge.starter.finish()
        this.charge.timer.reset()
    }

    @JvmOverloads
    fun selectedFireModeInfo(fireModes: MutableList<FireModeInfo>? = compute().availableFireModes()): FireModeInfo {
        if (fireModes.isNullOrEmpty()) {
            return FireModeInfo()
        }
        return fireModes[Mth.clamp(this.selectedFireMode.get(), 0, fireModes.size - 1)]
    }

    // 开火相关流程开始
    /*
     * 开火相关流程描述
     * 1. 调用shouldStartReloading和shouldStartBolt查看当前状态是否应该开始换弹或拉栓，是则调用startReloading或startBolt开始换弹/拉栓流程
     * 2. 调用canShoot(@Nullable Entity shooter)查看当前状态是否能够开火，如果能够开火则调用shootBullet进行开火
     * 3. 调用tick(@Nullable Entity shooter)执行枪械tick任务，包括换弹流程、过热计算、拉栓等
     *
     * 可选项：
     * 1. 使用GunData.virtualAmmo.set来设置虚拟弹药数量
     * 2. 传入带有IItemHandler能力的任意Entity来提供额外弹药
     *
     */
    /**
     * 是否应该开始换弹
     */
    fun shouldStartReloading(entity: Entity?): Boolean {
        return !reloading() && !useBackpackAmmo() && !hasEnoughAmmoToShoot(entity) && hasBackupAmmo(entity)
    }

    /**
     * 是否应该开始换弹
     */
    fun shouldStartBolt(): Boolean {
        return this.bolt.actionTimer.get() == 0 && this.bolt.needed.get()
    }

    /**
     * 开始换弹流程，换弹将在tick内被执行
     */
    fun startReload() {
        this.reload.reloadStarter.markStart()
    }

    /**
     * 开始拉栓流程，换弹将在tick内被执行
     */
    fun startBolt() {
        this.bolt.actionTimer.set(this.compute().boltActionTime + 1)
    }

    /**
     * 是否还有剩余弹药（不考虑枪内弹药）
     */
    fun hasBackupAmmo(entity: Entity?): Boolean {
        return countBackupAmmo(entity) > 0
    }

    /**
     * 计算剩余弹药数量（不考虑枪内弹药）
     */
    fun countBackupAmmo(entity: Entity?): Int {
        if (entity == null) return virtualAmmo.get()
        if (entity is Player && entity.isCreative || InventoryTool.hasCreativeAmmoBox(entity)) return Int.MAX_VALUE

        return Math.toIntExact(
            Mth.clamp(
                countBackupAmmoItem(entity).toLong() * this.selectedAmmoConsumer().loadAmount + this.virtualAmmo.get(),
                0,
                Int.MAX_VALUE.toLong()
            )
        )
    }

    /**
     * 计算剩余弹药数量（不考虑枪内弹药）
     */
    fun countBackupAmmo(handler: IItemHandler?): Int {
        if (handler == null) return virtualAmmo.get()
        if (InventoryTool.hasCreativeAmmoBox(handler)) return Int.MAX_VALUE

        return Math.toIntExact(
            Mth.clamp(
                countBackupAmmoItem(handler).toLong() * this.selectedAmmoConsumer().loadAmount + this.virtualAmmo.get(),
                0,
                Int.MAX_VALUE.toLong()
            )
        )
    }

    fun countBackupAmmoItem(entity: Entity?): Int {
        return this.selectedAmmoConsumer().count(this, entity)
    }

    fun countBackupAmmoItem(handler: IItemHandler?): Int {
        return this.selectedAmmoConsumer().count(this, handler)
    }

    /**
     * 消耗额外弹药（不影响枪内弹药）
     */
    fun consumeBackupAmmo(entity: Entity?, count: Int) {
        var count = count
        if (count <= 0 || entity is Player && entity.isCreative || InventoryTool.hasCreativeAmmoBox(entity)) return

        if (virtualAmmo.get() > 0) {
            val consumed = min(virtualAmmo.get(), count)
            virtualAmmo.add(-consumed)
            count -= consumed
            save()
        }
        if (count <= 0 || entity == null) return

        val consumer = this.selectedAmmoConsumer()
        val loadAmount = consumer.loadAmount
        if (count % loadAmount != 0) {
            val required = (count / loadAmount) + 1
            val consumed = consumer.consume(this, entity, required)
            count -= consumed * loadAmount

            // 迫真过载装填
            if (count <= 0) {
                this.virtualAmmo.add(-count)
            }
        } else {
            consumer.consume(this, entity, count / loadAmount)
        }
    }

    /**
     * 消耗额外弹药（不影响枪内弹药）
     */
    fun consumeBackupAmmo(handler: IItemHandler?, count: Int) {
        var count = count
        if (count <= 0 || InventoryTool.hasCreativeAmmoBox(handler)) return

        if (virtualAmmo.get() > 0) {
            val consumed = min(virtualAmmo.get(), count)
            virtualAmmo.add(-consumed)
            count -= consumed
            save()
        }
        if (count <= 0 || handler == null) return

        val consumer = selectedAmmoConsumer()
        val loadAmount = consumer.loadAmount

        if (count % loadAmount != 0) {
            val required = (count / loadAmount) + 1
            val consumed = consumer.consume(this, handler, required)
            count -= consumed * loadAmount

            // 迫真过载装填
            if (count <= 0) {
                this.virtualAmmo.add(-count)
            }
        } else {
            consumer.consume(this, handler, count / loadAmount)
        }
    }

    /**
     * 当前状态在换弹前的可用射击次数
     */
    fun currentAvailableShots(entity: Entity?): Int {
        val ammoCost = compute().ammoCostPerShoot
        if (ammoCost <= 0) return Int.MAX_VALUE

        return currentAvailableAmmo(entity) / ammoCost
    }

    /**
     * 当前枪内可用弹药数量
     */
    fun currentAvailableAmmo(entity: Entity?): Int {
        return if (useBackpackAmmo()) countBackupAmmo(entity) else this.ammo.get()
    }

    /**
     * 当前状态枪内是否拥有足够的弹药进行开火
     */
    fun hasEnoughAmmoToShoot(entity: Entity?): Boolean {
        return compute().ammoCostPerShoot <= currentAvailableAmmo(entity)
    }

    /**
     * 换弹完成后装填弹药，在换弹流程完成后调用
     */
    /**
     * 换弹完成后装填弹药，在换弹流程完成后调用
     */
    @JvmOverloads
    fun reloadAmmo(entity: Entity?, extraOne: Boolean = false) {
        if (useBackpackAmmo()) return

        val mag = compute().magazine
        val ammo = this.ammo.get()
        val ammoNeeded = mag - ammo + (if (extraOne) 1 else 0)

        // 空仓换弹的栓动武器应该在换弹后取消待上膛标记
        if (ammo == 0 && compute().boltActionTime > 0) {
            bolt.needed.set(false)
        }

        val available = countBackupAmmo(entity)
        val ammoToAdd = min(ammoNeeded, available)

        consumeBackupAmmo(entity, ammoToAdd)
        this.ammo.set(ammo + ammoToAdd)

        reload.setState(ReloadState.NOT_RELOADING)
        this.fireIndex.reset()
    }

    /**
     * 当前状态能否开火
     */
    fun canShoot(shooter: Entity?): Boolean {
        return item.canShoot(this, shooter)
    }

    /**
     * 无实体情况下开火
     */
    fun shoot(level: ServerLevel, shootPosition: Vec3, shootDirection: Vec3, spread: Double, zoom: Boolean) {
        this.item.shoot(level, shootPosition, shootDirection, this, spread, zoom, null)
    }

    /**
     * 有实体情况下开火
     */
    fun shoot(entity: Entity, spread: Double, zoom: Boolean, uuid: UUID?) {
        this.item.shoot(this, entity, spread, zoom, uuid)
    }

    fun shoot(entity: Entity, spread: Double, zoom: Boolean, uuid: UUID?, targetPos: Vec3?) {
        this.item.shoot(this, entity, spread, zoom, uuid, targetPos)
    }

    fun shoot(parameters: ShootParameters) {
        this.item.shoot(parameters)
    }

    /**
     * 执行tick更新枪械数据
     * <br></br>
     * 在玩家背包里时会使用GunItem.inventoryTick自动执行
     * <br></br>
     * 若需要在其他地方使用，请手动调用该方法
     *
     * @param inMainHand 枪械是否在主手上，用于控制部分tick流程是否执行
     */
    fun tick(shooter: Entity?, inMainHand: Boolean) {
        GunEventHandler.gunTick(shooter, this, inMainHand)
    }

    // 开火相关流程结束
    /**
     * 返还弹匣内弹药，在换弹和切换弹匣配件时调用
     */
    fun withdrawAmmo(ammoSupplier: Entity) {
        val itemAmount = withdrawAmmoCount()

        this.virtualAmmo.reset()
        this.ammo.reset()

        selectedAmmoConsumer().withdraw(ammoSupplier, itemAmount)
    }

    fun withdrawAmmoCount(): Int {
        return (this.virtualAmmo.get() + this.ammo.get()) / selectedAmmoConsumer().loadAmount
    }

    /**
     * 返还弹匣内弹药，在换弹和切换弹匣配件时调用
     */
    fun withdrawAmmo(handler: IItemHandler) {
        val itemAmount = withdrawAmmoCount()

        this.virtualAmmo.reset()
        this.ammo.reset()

        // 直接丢弃余数（恼）
        selectedAmmoConsumer().withdraw(handler, itemAmount)
    }

    fun availablePerks(): MutableList<Perk> {
        val availablePerks = mutableListOf<Perk>()
        val perkNames = compute().availablePerks()
        if (perkNames == null || perkNames.isEmpty()) return availablePerks

        val sortedNames: MutableList<String> = ArrayList<String>(perkNames)

        sortedNames.sortWith { s1, s2 ->
            val p1: Int = getPerkPriority(s1)
            val p2: Int = getPerkPriority(s2)
            if (p1 != p2) {
                return@sortWith p1.compareTo(p2)
            } else {
                return@sortWith s1!!.compareTo(s2!!)
            }
        }

        // TODO 正确实现注册项读取
        val perks = ArrayList<DeferredHolder<Perk, out Perk>>()
        perks.addAll(ModPerks.AMMO_PERKS.getEntries())
        perks.addAll(ModPerks.DAMAGE_PERKS.getEntries())
        perks.addAll(ModPerks.FUNC_PERKS.getEntries())

        val perkValues = perks.stream().map { obj -> obj!!.get() }.toList()
        val perkKeys = perks.stream()
            .map { perk -> perk!!.getKey()!!.location().toString() }
            .toList()

        for (name in sortedNames) {
            if (name.startsWith("@")) {
                when (name.substring(1)) {
                    "Ammo" -> availablePerks.addAll(
                        perkValues.stream().filter { perk -> perk!!.type == Perk.Type.AMMO }.toList()
                    )

                    "Functional" -> availablePerks.addAll(
                        perkValues.stream().filter { perk -> perk!!.type == Perk.Type.FUNCTIONAL }.toList()
                    )

                    "Damage" -> availablePerks.addAll(
                        perkValues.stream().filter { perk -> perk!!.type == Perk.Type.DAMAGE }.toList()
                    )
                }
            } else if (name.startsWith("!")) {
                val n = name.substring(1)
                val index = perkKeys.indexOf(n)
                if (index != -1) {
                    availablePerks.remove(perkValues[index])
                } else {
                    Mod.LOGGER.info("Perk {} not found", n)
                }
            } else {
                val index = perkKeys.indexOf(name)
                if (index != -1) {
                    availablePerks.add(perkValues[index])
                } else {
                    Mod.LOGGER.info("Perk {} not found", name)
                }
            }
        }
        return availablePerks
    }

    fun canApplyPerk(perk: Perk): Boolean {
        return availablePerks().contains(perk)
    }

    val rawDamageReduce: DamageReduce?
        get() = default.damageReduce

    val damageReduceRate: Double
        get() {
            for (type in Perk.Type.entries.toTypedArray()) {
                val instance = this.perk?.getInstance(type)
                if (instance != null) {
                    return instance.perk.getModifiedDamageReduceRate(this.rawDamageReduce)
                }
            }
            return this.rawDamageReduce!!.rate
        }

    val damageReduceMinDistance: Double
        get() {
            for (type in Perk.Type.entries.toTypedArray()) {
                val instance: PerkInstance? = this.perk?.getInstance(type)
                if (instance != null) {
                    return instance.perk.getModifiedDamageReduceMinDistance(this.rawDamageReduce)
                }
            }
            return this.rawDamageReduce!!.minDistance
        }

    fun meleeOnly(): Boolean {
        return compute().projectileAmount <= 0 && compute().meleeDamage > 0
    }

    fun isShotgun(gunData: DefaultGunData): Boolean {
        return gunData.projectileAmount > 1
    }

    val isShotgun: Boolean
        get() = isShotgun(compute())

    fun firePosition(): Vec3 {
        val list = this.compute().shootPos.positions
        val size = list.size
        if (size == 0) {
            return Vec3.ZERO
        }

        return if (this.compute().shootPos.boundUpWithAmmoAmount) {
            list.getOrNull(Mth.clamp(this.ammo.get() - 1, 0, size)) ?: Vec3.ZERO
        } else {
            list.getOrNull(this.fireIndex.get() % size) ?: Vec3.ZERO
        }
    }

    fun firePositionForHud(): Vec3 {
        return this.compute().shootPos.shootPositionForHud ?: firePosition()
    }

    fun fireDirection(): StringOrVec3 {
        val list = this.compute().shootPos.directions
        val size = list.size
        if (size == 0) {
            return StringOrVec3("Default")
        }

        return list.getOrNull(this.fireIndex.get() % size) ?: StringOrVec3("Default")
    }

    fun fireDirectionForHud(): StringOrVec3? {
        return this.compute().shootPos.shootDirectionForHud
    }

    fun getEnergyProvider(ammoSupplier: Entity?): IEnergyStorage? {
        return this.item.getEnergyProvider(this, ammoSupplier)
    }

    fun shakePlayers(source: Entity?) {
        if (source == null) return

        val shootShake = compute().shootShake ?: return

        ShakeClientMessage.sendToNearbyPlayers(source, shootShake.x, shootShake.y, shootShake.z)
    }

    // 可持久化属性开始
    @JvmField
    val selectedAmmoType: IntValue

    @JvmField
    val ammo: IntValue

    @JvmField
    val virtualAmmo: IntValue

    // backup ammo count override
    val backupAmmoCount: IntValue

    val ammoSlot: AmmoSlot

    @JvmField
    val burstAmount: IntValue

    @JvmField
    val selectedFireMode: IntValue

    @JvmField
    val fireIndex: IntValue

    @JvmField
    val level: IntValue

    @JvmField
    val exp: DoubleValue

    // Max: 100
    @JvmField
    val heat: DoubleValue

    @JvmField
    val shootAnimationTimer: IntValue

    @JvmField
    val shootTimer: IntValue

    @JvmField
    val overHeat: BooleanValue

    fun canAdjustZoom(): Boolean {
        return item.canAdjustZoom(this)
    }

    fun canSwitchScope(): Boolean {
        return item.canSwitchScope(this)
    }

    @JvmField
    val reload: Reload

    /**
     * 是否正在换弹
     */
    fun reloading(): Boolean {
        return reload.state() != ReloadState.NOT_RELOADING
    }

    @JvmField
    val charge: Charge

    fun charging(): Boolean {
        return charge.time() > 0
    }

    @JvmField
    val isEmpty: BooleanValue

    @JvmField
    val closeHammer: BooleanValue

    @JvmField
    val closeStrike: BooleanValue

    @JvmField
    val stopped: BooleanValue

    @JvmField
    val forceStop: BooleanValue

    @JvmField
    val loadIndex: IntValue

    @JvmField
    val holdOpen: BooleanValue

    @JvmField
    val hideBulletChain: BooleanValue

    @JvmField
    val sensitivity: IntValue

    @JvmField
    val zooming: BooleanValue

    // 其他子级属性
    @JvmField
    val bolt: Bolt

    @JvmField
    val attachment: Attachment

    @JvmField
    val perk: Perks?

    fun save() {
        val keysToRemove = mutableListOf<String>()
        for (key in perkTag.allKeys) {
            val compoundTag = perkTag.get(key) as? CompoundTag
            if (compoundTag?.isEmpty ?: false) {
                keysToRemove.add(key)
            }
        }
        keysToRemove.forEach { key -> perkTag.remove(key) }

        val cleanedTag = tag.copy()

        if (perkTag.isEmpty) {
            cleanedTag.remove("Perks")
        }

        if (attachmentTag.isEmpty) {
            cleanedTag.remove("Attachments")
        }

        if (gunDataTag.isEmpty) {
            cleanedTag.remove("GunData")
        }

        if (!tag.isEmpty) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(cleanedTag))
        } else {
            stack.remove(DataComponents.CUSTOM_DATA)
        }

        update()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is GunData) return false

        return ItemStack.isSameItemSameComponents(other.stack, this.stack)
    }

    fun copy(): GunData {
        val data: GunData = from(this.stack.copy())
        data.defaultDataSupplier = this.defaultDataSupplier
        return data
    }

    // TODO 删了这个，这个是为了临时适配女仆mod用的
    @Deprecated("use selectedFireModeInfo() instead", ReplaceWith("selectedFireModeInfo()"))
    @Suppress("unused")
    val fireMode: StringEnumValue<FireMode> = FireModeGetter()

    init {
        require(stack.item is GunItem) { "stack is not GunItem!" }

        val gunItem = stack.item as GunItem
        this.item = gunItem
        this.stack = stack
        this.id = getRegistryId(stack.item)

        this.defaultDataSupplier = Supplier { gunItem.getDefaultData(this) }

        val customData = stack.get(DataComponents.CUSTOM_DATA)
        this.tag = if (customData != null) customData.copyTag() else CompoundTag()

        gunDataTag = getOrPut("GunData")
        perkTag = getOrPut("Perks")
        attachmentTag = getOrPut("Attachments")
        propertyOverrideString = StringValue(this.gunDataTag, "Override")

        selectedAmmoType = IntValue(gunDataTag, "SelectedAmmoType")
        selectedFireMode = IntValue(gunDataTag, "SelectedFireMode", 0)
        fireIndex = IntValue(gunDataTag, "FireIndex", 0)

        // 可持久化属性
        reload = Reload(this)
        charge = Charge(this)
        bolt = Bolt(this)
        attachment = Attachment(this)
        perk = Perks(this)

        ammo = IntValue(gunDataTag, "Ammo")
        virtualAmmo = IntValue(gunDataTag, "VirtualAmmo")
        backupAmmoCount = IntValue(gunDataTag, "BackupAmmoCount")
        ammoSlot = AmmoSlot(gunDataTag)
        burstAmount = IntValue(gunDataTag, "BurstAmount")

        level = IntValue(gunDataTag, "Level")
        exp = DoubleValue(gunDataTag, "Exp")

        isEmpty = BooleanValue(gunDataTag, "IsEmpty")
        closeHammer = BooleanValue(gunDataTag, "CloseHammer")
        closeStrike = BooleanValue(gunDataTag, "CloseStrike")
        stopped = BooleanValue(gunDataTag, "Stopped")
        forceStop = BooleanValue(gunDataTag, "ForceStop")
        loadIndex = IntValue(gunDataTag, "LoadIndex")
        holdOpen = BooleanValue(gunDataTag, "HoldOpen")
        hideBulletChain = BooleanValue(gunDataTag, "HideBulletChain")
        sensitivity = IntValue(gunDataTag, "Sensitivity")
        heat = DoubleValue(gunDataTag, "Heat")
        shootAnimationTimer = IntValue(gunDataTag, "ShootAnimationTimer")
        shootTimer = IntValue(gunDataTag, "ShootTimer")
        overHeat = BooleanValue(gunDataTag, "OverHeat")
        zooming = BooleanValue(gunDataTag, "Zooming")

        var defaultFireMode = compute(false).defaultFireMode
        if (defaultFireMode == null) {
            defaultFireMode = FireMode.SEMI.name
        }

        val fireModes = compute(false).availableFireModes()
        for (i in fireModes.indices) {
            if (fireModes[i]!!.name == defaultFireMode) {
                selectedFireMode.defaultValue = i
                break
            }
        }
    }

    @Deprecated("")
    inner class FireModeGetter : StringEnumValue<FireMode>(
        CompoundTag(),
        "DeprecatedFireMode",
        FireMode.SEMI,
        { _ -> FireMode.SEMI }) {
        override fun get(): FireMode {
            return this@GunData.selectedFireModeInfo().mode ?: FireMode.SEMI
        }
    }

    companion object {
        @JvmField
        val DATA_CACHE: LoadingCache<ItemStack, GunData> = CacheBuilder.newBuilder()
            .weakKeys()
            .weakValues()
            .build(object : CacheLoader<ItemStack, GunData>() {
                override fun load(stack: ItemStack): GunData {
                    return GunData(stack)
                }
            })

        fun create(item: Item): GunData {
            return from(ItemStack(item))
        }

        @JvmStatic
        fun from(stack: ItemStack): GunData {
            return DATA_CACHE.getUnchecked(stack)
        }

        @JvmStatic
        fun getDefault(id: String): DefaultGunData {
            val isDefault = !com.atsuishio.superbwarfare.data.CustomData.GUN_DATA.containsKey(id)
            val data = com.atsuishio.superbwarfare.data.CustomData.GUN_DATA.getOrElseGet(id) { DefaultGunData() }
            data.isDefaultData = isDefault
            return data
        }

        fun getDefault(stack: ItemStack) = getDefault(stack.item)

        fun getDefault(item: Item) = getDefault(getRegistryId(item))

        fun getRegistryId(item: Item): String {
            var id = item.descriptionId
            id = id.substring(id.indexOf(".") + 1).replace('.', ':')
            return id
        }

        @JvmStatic
        fun compute(stack: ItemStack): DefaultGunData {
            return from(stack).compute()
        }

        private fun getPerkPriority(s: String): Int {
            if (s.isEmpty()) return 2

            return when (s[0]) {
                '@' -> 0
                '!' -> 2
                else -> 1
            }
        }

        @JvmField
        var VEHICLE_GUN_STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, GunData> =
            object : StreamCodec<RegistryFriendlyByteBuf, GunData> {
                override fun decode(buf: RegistryFriendlyByteBuf): GunData {
                    return from(ItemStack(ModItems.VEHICLE_GUN, 1, DataComponentPatch.STREAM_CODEC.decode(buf)))
                }

                override fun encode(buf: RegistryFriendlyByteBuf, data: GunData) {
                    val newData = data.copy()
                    newData.save()
                    DataComponentPatch.STREAM_CODEC.encode(buf, newData.stack.componentsPatch)
                }
            }
    }

    override fun hashCode() = stack.hashCode()
}
