package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.DefaultDataSupplier
import com.atsuishio.superbwarfare.data.JsonPropertyModifier
import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.StringOrVec3
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.AMMO_CONSUMER
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.AMMO_COST_PER_SHOOT
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.AVAILABLE_FIRE_MODES
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.AVAILABLE_PERKS
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.BOLT_ACTION_TIME
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.DEFAULT_ZOOM
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.MAGAZINE
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.MELEE_DAMAGE
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.PROJECTILE_AMOUNT
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.SHOOT_POS
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.SHOOT_SHAKE
import com.atsuishio.superbwarfare.data.gun.subdata.*
import com.atsuishio.superbwarfare.data.gun.value.*
import com.atsuishio.superbwarfare.event.GunEventHandler
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.tools.InventoryTool
import com.atsuishio.superbwarfare.tools.sameWith
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.IEnergyStorage
import net.minecraftforge.items.IItemHandler
import org.jetbrains.annotations.ApiStatus
import java.util.*
import java.util.function.Function
import kotlin.math.max
import kotlin.math.min

fun ItemStack.isGunItem() = this.item is GunItem
fun ItemStack.toGunData() = if (isGunItem()) GunData.from(this) else null

class GunData private constructor(
    stack: ItemStack, initialDefaultDataSupplier: (() -> DefaultGunData)? = null
) : DefaultDataSupplier<DefaultGunData> {
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

    private var defaultDataSupplier: () -> DefaultGunData
    var lastTimeStack: ItemStack? = null

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
    }

    fun clearTempModifications() {
        tempModifications = null
    }

    private val jsonPropModifier = JsonPropertyModifier(GunProp.entries)

    private var cache: DefaultGunData? = null

    private var tempModifications: Function<DefaultGunData, DefaultGunData>? = null

    @JvmOverloads
    @Deprecated("Use get() instead")
    @ApiStatus.ScheduledForRemoval
    fun compute(useCache: Boolean = true): DefaultGunData {
        if (cache != null && useCache) return cache!!

        var rawData = getDefault().copy()
//
//        // property override tag
//        jsonPropModifier.update(propertyOverrideString.get())
//        rawData = jsonPropModifier.computeProperties(this, rawData)
//
//        // gun modifiers
//        rawData = item.computeProperties(this, rawData)
//
//        // FireMode
//        rawData = selectedFireModeInfo(rawData.availableFireModes()).computeProperties(this, rawData)
//
//        // AmmoConsumer
//        rawData = selectedAmmoConsumer(rawData.getProcessedAmmoConsumers()).computeProperties(this, rawData)
//
//        // perk
//        for (type in Perk.Type.entries.toTypedArray()) {
//            val instance = perk.get(type) ?: continue
//
//            rawData = instance.computeProperties(this, rawData)
//        }
//
//        // 临时属性修改
//        if (tempModifications != null) {
//            rawData = tempModifications!!.apply(rawData)
//        }
//
//        rawData.limit()
//        if (useCache) {
//            cache = rawData
//        }

        return rawData
    }

    private var pmc: PMC<GunData, DefaultGunData>? = null

    @Suppress("unchecked_cast")
    fun <T> get(prop: GunProp<*, T>): T {
        val currentStack = this.stack
        val pmc = if (this.pmc == null || !(currentStack sameWith lastTimeStack)) {
            PMC(this).also { this.pmc = it }
        } else {
            this.pmc!!
        }

        if (!(currentStack sameWith lastTimeStack)) {
            lastTimeStack = currentStack.copy()
        } else {
            // 务必在初始化之后再调用缓存
            return pmc[prop]
        }

        // property override tag
        jsonPropModifier.update(propertyOverrideString.get())
        jsonPropModifier.modifyProperty(pmc)

        // gun modifiers
        item.modifyProperty(pmc)

        // FireMode
        selectedFireModeInfo(pmc[AVAILABLE_FIRE_MODES]).modifyProperty(pmc)

        // AmmoConsumer
        selectedAmmoConsumer(pmc[AMMO_CONSUMER]).modifyProperty(pmc)

        // perk
        for (type in Perk.Type.entries.toTypedArray()) {
            val list = perk.getInstances(type)
            for (instance in list) {
                instance.perk.modifyProperty(pmc)
            }
        }

        // TODO 临时属性修改
//        if (tempModifications != null) {
//            rawData = tempModifications!!.apply(rawData)
//        }

        // limit
        GunProp.modifyProperty(pmc)

        return pmc[prop]
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
        return get(MAGAZINE) <= 0
    }

    // TODO 这什么b scope判断
    fun minZoom(): Double {
        val scopeType = this.attachment.get(AttachmentType.SCOPE)
        return if (scopeType == 3) max(getDefault().minZoom, 1.25) else 1.25
    }

    // TODO 这什么b scope判断
    fun maxZoom(): Double {
        val scopeType = this.attachment.get(AttachmentType.SCOPE)
        return if (scopeType == 3) getDefault().maxZoom else 114514.0
    }

    fun zoom(): Double {
        if (minZoom() >= maxZoom()) return get(DEFAULT_ZOOM)
        return Mth.clamp(get(DEFAULT_ZOOM), minZoom(), maxZoom())
    }

    @JvmOverloads
    fun selectedAmmoConsumer(consumers: List<AmmoConsumer>? = get(AMMO_CONSUMER)): AmmoConsumer {
        if (consumers.isNullOrEmpty()) {
            return AmmoConsumer.INVALID
        }
        return consumers[this.selectedAmmoType.get().coerceIn(consumers.indices)]
    }

    fun changeAmmoConsumer(index: Int, ammoSupplier: Entity?) {
        val consumers = get(AMMO_CONSUMER)
        val targetIndex = index.coerceIn(consumers.indices)
        if (targetIndex == selectedAmmoType.get()) return

        if (!(ammoSupplier is Player && ammoSupplier.isCreative)) {
            val currentConsumer = selectedAmmoConsumer()
            val targetConsumer = consumers[selectedAmmoType.get()]

            val currentSlot = currentConsumer.ammoSlot
            val targetSlot = targetConsumer.ammoSlot

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
            this.ammo.set(get(MAGAZINE))
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
    fun selectedFireModeInfo(fireModes: List<FireModeInfo>? = get(AVAILABLE_FIRE_MODES)): FireModeInfo {
        if (fireModes.isNullOrEmpty()) {
            return FireModeInfo()
        }
        return fireModes[this.selectedFireMode.get().coerceIn(fireModes.indices)]
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
        this.bolt.actionTimer.set(get(BOLT_ACTION_TIME) + 1)
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
            min(
                countBackupAmmoItem(entity).toLong() * this.selectedAmmoConsumer().loadAmount + this.virtualAmmo.get(),
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
            min(
                countBackupAmmoItem(handler).toLong() * this.selectedAmmoConsumer().loadAmount + this.virtualAmmo.get(),
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
        val ammoCost = get(AMMO_COST_PER_SHOOT)
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
        return get(AMMO_COST_PER_SHOOT) <= currentAvailableAmmo(entity)
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

        val mag = get(MAGAZINE)
        val ammo = this.ammo.get()
        val ammoNeeded = mag - ammo + (if (extraOne) 1 else 0)

        // 空仓换弹的栓动武器应该在换弹后取消待上膛标记
        if (ammo == 0 && get(BOLT_ACTION_TIME) > 0) {
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

    fun availablePerks() = get(AVAILABLE_PERKS)

    fun canApplyPerk(perk: Perk) = availablePerks().contains(perk)

    val rawDamageReduce: DamageReduce
        get() = getDefault().damageReduce

    val damageReduceRate: Double
        get() {
            for (type in Perk.Type.entries.toTypedArray()) {
                return this.perk.getInstances(type)
                    .minOfOrNull { it.perk.getModifiedDamageReduceRate(this.rawDamageReduce) } ?: continue
            }
            return this.rawDamageReduce.rate
        }

    val damageReduceMinDistance: Double
        get() {
            for (type in Perk.Type.entries.toTypedArray()) {
                return this.perk.getInstances(type)
                    .minOfOrNull { it.perk.getModifiedDamageReduceMinDistance(this.rawDamageReduce) } ?: continue
            }
            return this.rawDamageReduce.minDistance
        }

    fun meleeOnly(): Boolean {
        return get(PROJECTILE_AMOUNT) <= 0 && get(MELEE_DAMAGE) > 0
    }

    val isShotgun: Boolean
        get() = get(PROJECTILE_AMOUNT) > 1

    fun firePosition(): Vec3 {
        val list = get(SHOOT_POS).positions
        val size = list.size
        if (size == 0) {
            return Vec3.ZERO
        }

        return if (get(SHOOT_POS).boundUpWithAmmoAmount) {
            list.getOrNull(Mth.clamp(this.ammo.get() - 1, 0, size)) ?: Vec3.ZERO
        } else {
            list.getOrNull(this.fireIndex.get() % size) ?: Vec3.ZERO
        }
    }

    fun firePositionForHud(): Vec3 {
        return get(SHOOT_POS).shootPositionForHud ?: firePosition()
    }

    fun fireDirection(): StringOrVec3 {
        val list = get(SHOOT_POS).directions
        val size = list.size
        if (size == 0) {
            return StringOrVec3("Default")
        }

        return list.getOrNull(this.fireIndex.get() % size) ?: StringOrVec3("Default")
    }

    fun fireDirectionForHud(): StringOrVec3? {
        return get(SHOOT_POS).shootDirectionForHud
    }

    fun getEnergyProvider(ammoSupplier: Entity?): LazyOptional<IEnergyStorage> {
        return this.item.getEnergyProvider(this, ammoSupplier)
    }

    fun shakePlayers(source: Entity?) {
        if (source == null) return

        val shootShake = get(SHOOT_SHAKE) ?: return

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
    @JvmField
    val backupAmmoCount: IntValue

    @JvmField
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

    fun canAdjustZoom() = item.canAdjustZoom(this)

    fun canSwitchScope() = item.canSwitchScope(this)

    @JvmField
    val reload: Reload

    /**
     * 是否正在换弹
     */
    fun reloading() = reload.state() != ReloadState.NOT_RELOADING

    @JvmField
    val charge: Charge

    fun charging() = charge.time() > 0

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
    val perk: Perks

    @JvmField
    val weaponPitch: DoubleValue

    @JvmField
    val weaponYaw: DoubleValue

    fun save() {
        if (stack sameWith originalItemStack) return

        // TODO 实现正确的清理空tag的方式
//        var keysToRemove = new ArrayList<String>();
//        for (var key : perkTag.getAllKeys()) {
//            if (perkTag.get(key) instanceof CompoundTag compoundTag && compoundTag.isEmpty()) {
//                keysToRemove.add(key);
//            }
//        }
//        keysToRemove.forEach(perkTag::remove);
//
//        if (perkTag.isEmpty()) {
//            stack.removeTagKey("Perks");
//        }
//
//        if (attachmentTag.isEmpty()) {
//            stack.removeTagKey("Attachments");
//        }
//
//        if (gunDataTag.isEmpty()) {
//            stack.removeTagKey("GunData");
//        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is GunData) return false

        return other.stack sameWith this.stack
    }

    fun copy(): GunData {
        return GunData(this.stack.copy(), this.defaultDataSupplier)
    }

    // TODO 删了这个，这个是为了临时适配女仆mod用的
    @Deprecated("use selectedFireModeInfo() instead", ReplaceWith("selectedFireModeInfo()"))
    @Suppress("unused")
    @JvmField
    val fireMode: StringEnumValue<FireMode> = object : StringEnumValue<FireMode>(
        CompoundTag(),
        "DeprecatedFireMode",
        FireMode.SEMI,
        { _ -> FireMode.SEMI }) {

        override fun get(): FireMode {
            return this@GunData.selectedFireModeInfo().mode ?: FireMode.SEMI
        }
    }

    private val originalItemStack: ItemStack

    init {
        require(stack.item is GunItem) { "stack is not GunItem!" }

        originalItemStack = stack.copy()
        val gunItem = stack.item as GunItem
        this.item = gunItem
        this.stack = stack
        this.id = getRegistryId(stack.item)

        this.defaultDataSupplier = initialDefaultDataSupplier ?: { gunItem.getDefaultData(this) }

        this.tag = stack.getOrCreateTag()

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
        weaponPitch = DoubleValue(gunDataTag, "weaponPitch")
        weaponYaw = DoubleValue(gunDataTag, "weaponYaw")

        var defaultFireMode = get(GunProp.DEFAULT_FIRE_MODE)

        val fireModes = get(AVAILABLE_FIRE_MODES)
        for (i in fireModes.indices) {
            if (fireModes[i].name == defaultFireMode) {
                selectedFireMode.defaultValue = i
                break
            }
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
        @JvmOverloads
        fun from(stack: ItemStack, defaultDataSupplier: (() -> DefaultGunData)? = null): GunData {
            if (defaultDataSupplier != null) {
                return GunData(stack, defaultDataSupplier)
            }
            return DATA_CACHE.getUnchecked(stack)
        }

        @JvmOverloads
        @JvmStatic
        fun <T> get(stack: ItemStack, prop: GunProp<*, T>, useCache: Boolean = true): T {
            return from(stack).get(prop)
        }

        @JvmStatic
        fun getDefault(id: String): DefaultGunData {
            val isDefault = !com.atsuishio.superbwarfare.data.CustomData.GUN_DATA.containsKey(id)
            val data = com.atsuishio.superbwarfare.data.CustomData.GUN_DATA.getOrElseGet(id) { DefaultGunData() }
            data.isDefaultData = isDefault
            return data
        }

        fun getDefault(stack: ItemStack): DefaultGunData {
            return getDefault(stack.item)
        }

        fun getDefault(item: Item): DefaultGunData {
            return getDefault(getRegistryId(item))
        }

        fun getRegistryId(item: Item): String {
            var id = item.descriptionId
            id = id.substring(id.indexOf(".") + 1).replace('.', ':')
            return id
        }

        @JvmStatic
        @Suppress("unused")
        @Deprecated("use get() instead", level = DeprecationLevel.ERROR)
        @ApiStatus.ScheduledForRemoval
        fun compute(stack: ItemStack): DefaultGunData {
            error("use get() instead!")
        }

        fun getPerkPriority(s: String): Int {
            if (s.isEmpty()) return 2

            return when (s[0]) {
                '@' -> 0
                '!' -> 2
                else -> 1
            }
        }
    }

    override fun hashCode() = stack.hashCode()
}
