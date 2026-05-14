package com.atsuishio.superbwarfare.item.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.api.event.ShootEvent
import com.atsuishio.superbwarfare.client.particle.BulletDecalOption
import com.atsuishio.superbwarfare.client.screens.WeaponEditScreen
import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent
import com.atsuishio.superbwarfare.data.CustomData
import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.PropertyModifier
import com.atsuishio.superbwarfare.data.gun.*
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.getDefault
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.data.launchable.LaunchableEntityTool
import com.atsuishio.superbwarfare.data.launchable.ShootData
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback
import com.atsuishio.superbwarfare.entity.projectile.*
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModPerks
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.EnergyStorageItem
import com.atsuishio.superbwarfare.item.ItemScreenProvider
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.tools.VectorTool.isInLiquid
import com.atsuishio.superbwarfare.world.phys.EntityResult
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage
import org.joml.Math
import software.bernie.geckolib.animatable.GeoItem
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

abstract class GunItem(properties: Properties) : Item(properties.stacksTo(1)), ItemScreenProvider,
    EnergyStorageItem, PropertyModifier<GunData, DefaultGunData> {

    protected val random: RandomSource = RandomSource.create()

    override fun getMaxEnergy(stack: ItemStack): Int {
        return if (stack.item is GunItem) GunData.get(stack, GunProp.MAX_ENERGY) else 0
    }

    override fun getMaxReceiveEnergy(stack: ItemStack): Int {
        return if (stack.item is GunItem) GunData.get(stack, GunProp.MAX_RECEIVE_ENERGY) else -1
    }

    override fun getMaxExtractEnergy(stack: ItemStack): Int {
        return if (stack.item is GunItem) GunData.get(stack, GunProp.MAX_EXTRACT_ENERGY) else -1
    }

    @JvmField
    val reloadTimeBehaviors = mutableMapOf<Int, Consumer<GunData>?>()

    @JvmField
    val boltTimeBehaviors = mutableMapOf<Int, Consumer<GunData>?>()

    init {
        addReloadTimeBehavior(this.reloadTimeBehaviors)
        addBoltTimeBehavior(this.boltTimeBehaviors)
    }

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) = with(GunProp) {
        val data = modifier.data

        modifier[DAMAGE] += getCustomDamage(data)
        modifier[HEADSHOT] += getCustomHeadshot(data)
        modifier[BYPASSES_ARMOR] += getCustomBypassArmor(data)
        modifier[MAGAZINE] += getCustomMagazine(data)
        modifier[DEFAULT_ZOOM] += getCustomZoom(data)
        modifier[RPM] += getCustomRPM(data)
        modifier[WEIGHT] += getCustomWeight(data)
        modifier[VELOCITY] += getCustomVelocity(data)
        modifier[SOUND_RADIUS] += getCustomSoundRadius(data)
        modifier[BOLT_ACTION_TIME] += getCustomBoltActionTime(data)
    }

    override fun isBarVisible(stack: ItemStack): Boolean {
        val data = from(stack)
        if (data.get(GunProp.MAX_DURABILITY) > 0) return super.isBarVisible(stack)

        val cap = stack.getCapability(Capabilities.EnergyStorage.ITEM)
        return cap != null && cap.energyStored > 0 && cap.maxEnergyStored > 0
    }

    override fun getBarWidth(stack: ItemStack): Int {
        val data = from(stack)
        if (data.get(GunProp.MAX_DURABILITY) > 0) {
            return super.getBarWidth(stack)
        }

        if (data.get(GunProp.MAX_ENERGY) > 0) {
            val cap = stack.getCapability(Capabilities.EnergyStorage.ITEM)
            return Math.round((cap?.energyStored ?: 0).toFloat() * 13f / GunData.get(stack, GunProp.MAX_ENERGY))
        }

        return super.getBarWidth(stack)
    }

    override fun getBarColor(stack: ItemStack): Int {
        val data = from(stack)
        if (data.get(GunProp.MAX_DURABILITY) > 0) {
            return super.getBarColor(stack)
        }

        val resource = GunResource.from(stack)
        if (data.get(GunProp.MAX_ENERGY) > 0) {
            return this.getEnergyBarColor(resource)
        }

        return super.getBarColor(stack)
    }

    open fun getEnergyBarColor(resource: GunResource): Int {
        return resource.compute().energyBarColor.get()
    }

    open fun init(data: GunData) {
        if (isInitialized(data)) return

        data.gunDataTag.putUUID("UUID", UUID.randomUUID())
    }

    open fun isInitialized(data: GunData) = data.gunDataTag.hasUUID("UUID")

    override fun canAttackBlock(pState: BlockState, pLevel: Level, pPos: BlockPos, pPlayer: Player) = false

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slot: Int, selected: Boolean) {
        if (stack.item !is GunItem || level.isClientSide) return

        if (level is ServerLevel) {
            GeoItem.getOrAssignId(stack, level)
        }

        val data = from(stack)

        val inMainHand = entity is LivingEntity && entity.mainHandItem == stack
        data.tick(entity, inMainHand)
    }

    override fun shouldCauseReequipAnimation(oldStack: ItemStack, newStack: ItemStack, slotChanged: Boolean) = false

    override fun getDefaultAttributeModifiers(stack: ItemStack): ItemAttributeModifiers {
        val list = ArrayList<ItemAttributeModifiers.Entry?>(super.getDefaultAttributeModifiers(stack).modifiers())
        val data = from(stack)

        // 移速
        list.add(
            ItemAttributeModifiers.Entry(
                Attributes.MOVEMENT_SPEED,
                AttributeModifier(
                    SPEED_ID,
                    -0.01f - 0.005f * data.get(GunProp.WEIGHT),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ),
                EquipmentSlotGroup.MAINHAND
            )
        )

        // 近战伤害
        if (data.get(GunProp.MELEE_DAMAGE) > 0) {
            list.add(
                ItemAttributeModifiers.Entry(
                    Attributes.ATTACK_DAMAGE,
                    AttributeModifier(
                        BASE_ATTACK_DAMAGE_ID,
                        data.get(GunProp.MELEE_DAMAGE),
                        AttributeModifier.Operation.ADD_VALUE
                    ),
                    EquipmentSlotGroup.MAINHAND
                )
            )
        }

        return ItemAttributeModifiers(list, true)
    }

    override fun getTooltipImage(pStack: ItemStack): Optional<TooltipComponent> {
        return Optional.of(GunImageComponent(pStack))
    }

    open fun getGunIcon(stack: ItemStack) = getGunIcon(from(stack))

    open fun getGunIcon(data: GunData) = data.get(GunProp.ICON)

    override fun isFoil(stack: ItemStack) = false

    override fun isEnchantable(stack: ItemStack) = false

    override fun supportsEnchantment(stack: ItemStack, enchantment: Holder<Enchantment?>) = false

    override fun getMaxDamage(stack: ItemStack): Int {
        val maxDurability = from(stack).get(GunProp.MAX_DURABILITY)

        if (maxDurability > 0) {
            if (!stack.has(DataComponents.MAX_DAMAGE) || !stack.has(DataComponents.DAMAGE)) {
                stack.set(DataComponents.MAX_DAMAGE, maxDurability)
                stack.set(DataComponents.DAMAGE, 0)
            }
        } else {
            stack.remove(DataComponents.MAX_DAMAGE)
        }
        return maxDurability
    }

    /**
     * 开膛待击
     */
    open fun isOpenBolt(data: GunData) = false

    /**
     * 是否允许额外往枪管里塞入一发子弹
     */
    open fun hasBulletInBarrel(data: GunData) = false

    /**
     * 武器是否能更换枪管配件
     */
    open fun hasCustomBarrel(data: GunData) = false

    open val validBarrels: IntArray
        get() = intArrayOf(0, 1, 2)

    /**
     * 武器是否能更换枪托配件
     */
    open fun hasCustomGrip(data: GunData) = false

    open val validGrips: IntArray
        get() = intArrayOf(0, 1, 2, 3)

    /**
     * 武器是否能更换弹匣配件
     */
    open fun hasCustomMagazine(data: GunData) = false

    open val validMagazines: IntArray
        get() = intArrayOf(0, 1, 2)

    /**
     * 武器是否能更换瞄具配件
     */
    open fun hasCustomScope(data: GunData) = false

    open val validScopes: IntArray
        get() = intArrayOf(0, 1, 2, 3)

    /**
     * 武器是否能更换枪托配件
     */
    open fun hasCustomStock(data: GunData) = false

    open val validStocks: IntArray
        get() = intArrayOf(0, 1, 2)

    /**
     * 武器是否有脚架
     */
    open fun hasBipod(data: GunData) = false

    /**
     * 武器是否能进行近战攻击
     */
    open fun hasMeleeAttack(data: GunData) = data.get(GunProp.MELEE_DAMAGE) > 0

    /**
     * 获取额外伤害加成
     */
    open fun getCustomDamage(data: GunData) = 0.0

    /**
     * 获取额外爆头伤害加成
     */
    open fun getCustomHeadshot(data: GunData) = 0.0

    /**
     * 获取额外护甲穿透加成
     */
    open fun getCustomBypassArmor(data: GunData) = 0.0

    /**
     * 获取额外弹匣容量加成
     */
    open fun getCustomMagazine(data: GunData) = 0

    /**
     * 获取额外拉栓时间加成
     */
    open fun getCustomBoltActionTime(data: GunData) = 0

    /**
     * 获取额外缩放倍率加成
     */
    open fun getCustomZoom(data: GunData) = 0.0

    /**
     * 获取额外RPM加成
     */
    open fun getCustomRPM(data: GunData) = 0

    /**
     * 获取额外总重量加成
     */
    open fun getCustomWeight(data: GunData): Double {
        val attachment = data.attachment

        val scopeWeight = when (attachment.get(AttachmentType.SCOPE)) {
            1 -> 0.5
            2 -> 1.0
            3 -> 1.5
            else -> 0.0
        }

        val barrelWeight = when (attachment.get(AttachmentType.BARREL)) {
            1 -> 0.5
            2 -> 1.0
            else -> 0.0
        }

        val magazineWeight = when (attachment.get(AttachmentType.MAGAZINE)) {
            1 -> 1.0
            2 -> 2.0
            else -> 0.0
        }

        val stockWeight = when (attachment.get(AttachmentType.STOCK)) {
            1 -> -2.0
            2 -> 1.5
            else -> 0.0
        }

        val gripWeight = when (attachment.get(AttachmentType.GRIP)) {
            1, 2 -> 0.25
            3 -> 1.0
            else -> 0.0
        }

        return scopeWeight + barrelWeight + magazineWeight + stockWeight + gripWeight
    }

    /**
     * 获取额外弹速加成
     */
    open fun getCustomVelocity(data: GunData) = 0.0

    /**
     * 获取额外音效半径加成
     */
    open fun getCustomSoundRadius(data: GunData) = if (data.attachment.get(AttachmentType.BARREL) == 2) 0.6 else 1.0

    /**
     * 是否允许缩放
     */
    open fun canAdjustZoom(data: GunData) = false

    /**
     * 是否允许切换瞄具
     */
    open fun canSwitchScope(data: GunData) = false

    /**
     * 添加达到指定换弹时间时的额外行为
     */
    open fun addReloadTimeBehavior(behaviors: MutableMap<Int, Consumer<GunData>?>?) {}

    /**
     * 添加达到指定拉栓/泵动时间时的额外行为
     */
    open fun addBoltTimeBehavior(behaviors: MutableMap<Int, Consumer<GunData>?>?) {}

    /**
     * 判断武器能否开火
     */
    open fun canShoot(data: GunData, shooter: Entity?): Boolean {
        return data.get(GunProp.PROJECTILE_AMOUNT) > 0
                && !data.overHeat.get()
                && data.get(GunProp.HEAT_PER_SHOOT) <= (100 + data.get(GunProp.HEAT_PER_SHOOT) - data.heat.get())
                && !data.reloading()
                && !data.charging()
                && !data.bolt.needed.get()
                && data.hasEnoughAmmoToShoot(shooter)
    }

    open fun useSpecialFireProcedure(data: GunData) = false
    open fun hideBulletChainBelowShots() = -1
    open fun whenNoAmmo(data: GunData) {}

    /**
     * 服务端在开火前的额外行为
     */
    open fun beforeShoot(parameters: ShootParameters) {
        val data = parameters.data
        val ammoSupplier = parameters.ammoSupplier

        postEvent(ShootEvent.Pre(parameters))

        // 判断是否为栓动武器（BoltActionTime > 0），并在开火后给一个需要上膛的状态
        if (data.get(GunProp.BOLT_ACTION_TIME) > 0 && data.hasEnoughAmmoToShoot(ammoSupplier)) {
            data.bolt.needed.set(true)
        }

        if (data.currentAvailableShots(ammoSupplier) <= hideBulletChainBelowShots()) {
            data.hideBulletChain.set(true)
        }
    }

    @Deprecated("")
    @Suppress("unused")
    fun beforeShoot(
        shooter: Entity?,
        level: ServerLevel,
        shootPosition: Vec3,
        shootDirection: Vec3,
        data: GunData,
        spread: Double,
        zoom: Boolean
    ) {
    }

    /**
     * 服务端在开火后的额外行为
     */
    open fun afterShoot(parameters: ShootParameters) {
        val data = parameters.data
        val shooter = parameters.shooter
        val ammoSupplier = parameters.ammoSupplier
        val level = parameters.level

        postEvent(ShootEvent.Post(parameters))

        if (!data.useBackpackAmmo()) {
            data.ammo.set(data.ammo.get() - data.get(GunProp.AMMO_COST_PER_SHOOT))
            //            data.item.whenNoAmmo(data);
        } else {
            data.consumeBackupAmmo(ammoSupplier, data.get(GunProp.AMMO_COST_PER_SHOOT))
        }

        if (!data.hasEnoughAmmoToShoot(ammoSupplier)) {
            data.burstAmount.reset()
        }

        val stack = data.stack()
        if (this.getMaxDamage(stack) > 0) {
            if (shooter is LivingEntity) {
                stack.hurtAndBreak(data.get(GunProp.DURABILITY_PER_SHOOT), shooter, EquipmentSlot.MAINHAND)
            } else {
                stack.hurtAndBreak(
                    data.get(GunProp.DURABILITY_PER_SHOOT),
                    level,
                    null as LivingEntity?
                ) { }
            }
        }

        data.closeStrike.set(true)

        // 真实后坐（
        if (shooter != null && data.get(GunProp.RECOIL) != 0.0) {
            shooter.deltaMovement =
                shooter.deltaMovement.add(shooter.getViewVector(1f).scale(-data.get(GunProp.RECOIL)))
        }

        val size = data.get(GunProp.SHOOT_POS).positions.size
        if (size > 0 && !data.get(GunProp.SHOOT_POS).boundUpWithAmmoAmount) {
            data.fireIndex.set((data.fireIndex.get() + 1) % size)
        } else {
            data.fireIndex.reset()
        }

        // TODO 这样搞会在远程遥控火炮的时候，无论隔多远都会摇晃屏幕（恼
//        data.shakePlayers(shooter);
        data.clearTempModifications()
    }

    @Deprecated("")
    @Suppress("unused")
    fun afterShoot(
        shooter: Entity?,
        level: ServerLevel,
        shootPosition: Vec3,
        shootDirection: Vec3,
        data: GunData,
        spread: Double,
        zoom: Boolean,
        uuid: UUID?
    ) {
    }

    fun shoot(
        level: ServerLevel,
        shootPosition: Vec3,
        shootDirection: Vec3,
        data: GunData,
        spread: Double,
        zoom: Boolean,
        uuid: UUID?
    ) {
        shoot(ShootParameters(null, null, level, shootPosition, shootDirection, data, spread, zoom, uuid, null))
    }

    fun shoot(data: GunData, shooter: Entity, spread: Double, zoom: Boolean, uuid: UUID?) {
        val server = shooter.level() as? ServerLevel ?: return

        shoot(
            ShootParameters(
                shooter,
                shooter,
                server,
                Vec3(shooter.x, shooter.eyeY, shooter.z),
                shooter.lookAngle,
                data,
                spread,
                zoom,
                uuid,
                null
            )
        )
    }

    fun shoot(data: GunData, shooter: Entity, spread: Double, zoom: Boolean, uuid: UUID?, pos: Vec3?) {
        val server = shooter.level() as? ServerLevel ?: return

        shoot(
            ShootParameters(
                shooter,
                shooter,
                server,
                Vec3(shooter.x, shooter.eyeY, shooter.z),
                shooter.lookAngle,
                data,
                spread,
                zoom,
                uuid,
                pos
            )
        )
    }

    /**
     * 服务端处理单次开火
     *
     * @param parameters 开火参数
     */
    open fun shoot(parameters: ShootParameters) {
        val data = parameters.data
        val shooter = parameters.shooter
        val ammoSupplier = parameters.ammoSupplier
        val zoom = parameters.zoom

        if (!data.canShoot(ammoSupplier)) return

        // 开火前事件
        data.item.beforeShoot(parameters)

        val projectileAmount = data.get(GunProp.PROJECTILE_AMOUNT)

        // 生成所有子弹
        repeat(projectileAmount) {
            if (!shootBullet(parameters)) return
        }

        // n连发模式开火数据设置
        if (data.selectedFireModeInfo().mode == FireMode.BURST) {
            val amount = data.burstAmount.get()
            data.burstAmount.set(if (amount == 0) data.get(GunProp.BURST_AMOUNT) - 1 else Math.max(0, amount - 1))
        }

        // 添加热量
        data.heat.set(Math.max(data.heat.get() + data.get(GunProp.HEAT_PER_SHOOT), 0.0))

        if (data.item.enableShootTimer()) {
            // 射击动画时长
            data.shootAnimationTimer.set(data.get(GunProp.SHOOT_ANIMATION_TIME))
            // 载具射击后的一个特殊记时器
            data.shootTimer.set(Math.min(data.shootTimer.get() + 3, 5))
        }

        // 过热
        if (data.heat.get() >= 100 && !data.overHeat.get()) {
            data.overHeat.set(true)
            if (shooter is ServerPlayer) {
                shooter.playLocalSound(ModSounds.OVERHEAT.get(), 2f, 1f)
            }
        }

        playFireSounds(data, shooter, zoom)

        // 开火后事件
        data.item.afterShoot(parameters)

        data.save()
    }

    @Deprecated("")
    @Suppress("unused")
    fun shoot(
        shooter: Entity?,
        level: ServerLevel,
        shootPosition: Vec3,
        shootDirection: Vec3,
        data: GunData,
        spread: Double,
        zoom: Boolean,
        uuid: UUID?
    ) {
    }

    /**
     * 播放开火音效
     */
    open fun playFireSounds(data: GunData, shooter: Entity?, zoom: Boolean) {
        if (shooter == null) return

        val pitch = if (data.heat.get() <= 75) 1f else (1 - 0.02 * Math.abs(75 - data.heat.get())).toFloat()

        val perk = data.perk.get(Perk.Type.AMMO)
        if (perk === ModPerks.BEAST_BULLET.get()) {
            shooter.playSound(ModSounds.HENG.get(), 4f, pitch)
        }

        val soundRadius = data.get(GunProp.SOUND_RADIUS).toFloat()
        val soundInfo = data.get(GunProp.SOUND_INFO)
        val isSilent = data.attachment.get(AttachmentType.BARREL) == 2

        val sound3p = if (isSilent) soundInfo.fire3PSilent else soundInfo.fire3P
        if (sound3p != null) {
            shooter.playSound(sound3p, soundRadius * 0.4f, pitch)
        }

        val soundFar = if (isSilent) soundInfo.fire3PFarSilent else soundInfo.fire3PFar
        if (soundFar != null) {
            shooter.playSound(soundFar, soundRadius * 0.7f, pitch)
        }

        val soundVeryFar = if (isSilent) soundInfo.fire3PVeryFarSilent else soundInfo.fire3PVeryFar
        if (soundVeryFar != null) {
            shooter.playSound(soundVeryFar, soundRadius, pitch)
        }
    }

    /**
     * 服务端处理按下开火按键时的额外行为
     */
    fun onFireKeyPress(data: GunData, player: Player, zoom: Boolean) {
        if (data.reload.prepareTimer.get() == 0 && data.reloading() && data.hasEnoughAmmoToShoot(player)) {
            data.forceStop.set(true)
        }
        if (player is ServerPlayer && data.stack.`is`(ModItems.QL_1031.get()) && data.selectedFireModeInfo().name == "Hold") {
            player.connection.send(ClientboundStopSoundPacket(loc("ql_1031_discharge"), SoundSource.PLAYERS))
        }
    }

    /**
     * 服务端处理松开开火按键时的额外行为
     */
    open fun onFireKeyRelease(data: GunData, player: Player, power: Double, zoom: Boolean) {
        if (player is ServerPlayer && data.get(GunProp.SEEK_TYPE) == SeekType.HOLD_FIRE) {
            val stack = data.stack
            val origin = stack.item.descriptionId
            val name = origin.substring(origin.lastIndexOf(".") + 1)
            player.connection.send(ClientboundStopSoundPacket(loc(name + "_lock"), SoundSource.PLAYERS))
        }
        if (player is ServerPlayer && data.stack.`is`(ModItems.QL_1031.get()) && data.selectedFireModeInfo().name == "Hold") {
            player.connection.send(ClientboundStopSoundPacket(loc("ql_1031_charge"), SoundSource.PLAYERS))
        }
    }

    /**
     * 服务端发射单发子弹
     */
    open fun shootBullet(parameters: ShootParameters): Boolean {
        val data = parameters.data
        val level = parameters.level
        val shootPosition = parameters.shootPosition
        val shootDirection = parameters.shootDirection
        val shooter = parameters.shooter
        val zoom = parameters.zoom
        val spread = parameters.spread
        val uuid = parameters.targetEntityUUID
        val targetPos = parameters.targetPos

        val stack = data.stack

        val projectileInfo = data.get(GunProp.PROJECTILE)
        val projectileType = projectileInfo.itemId
        val projectileTypeStr = projectileType.trim { it <= ' ' }.lowercase()

        if (projectileTypeStr == "empty") {
            return true
        } else if (projectileTypeStr == "ray") {
            return this.shootRay(parameters)
        }

        val headshot = data.get(GunProp.HEADSHOT)
        val damage = data.get(GunProp.DAMAGE)
        var velocity = data.get(GunProp.VELOCITY).toFloat()
        val bypassArmorRate = data.get(GunProp.BYPASSES_ARMOR)

        if (isInLiquid(level, shootPosition)) {
            velocity = 2 + 0.05f * velocity
        }

        val finalVelocity = velocity

        val entityHolder = AtomicReference<Entity?>()

        EntityType.byString(projectileType).ifPresent { entityType ->
            val entity = entityType.create(level)
            if (entity == null) {
                Mod.LOGGER.warn("Failed to create projectile entity {}", projectileType)
                return@ifPresent
            }

            if (entity is Projectile) {
                entity.owner = shooter
            }

            // SBW子弹弹射物专属属性
            if (entity is ProjectileEntity) {
                entity.shooter(shooter)
                    .damage(damage.toFloat())
                    .headShot(headshot.toFloat())
                    .zoom(zoom)
                    .bypassArmorRate(bypassArmorRate.toFloat())
                    .setGunItemId(stack)
                    .velocity(finalVelocity)
            }

            // SBW弹射物专属属性
            if (entity is CustomDamageProjectile) {
                entity.setDamage(damage.toFloat())
            }

            if (entity is CustomGravityEntity) {
                entity.setGravity(data.get(GunProp.GRAVITY).toFloat())
            }

            if (entity is ExplosiveProjectile) {
                entity.setExplosionDamage(data.get(GunProp.EXPLOSION_DAMAGE).toFloat())
                entity.setExplosionRadius(data.get(GunProp.EXPLOSION_RADIUS).toFloat())
                entity.setLife(data.get(GunProp.PROJECTILE_LIFE))
            }

            if (entity is WireGuideMissileEntity && shooter != null && shooter.vehicle != null) {
                entity.setLauncherVehicle(shooter.vehicle!!.getUUID())
            }

            if (entity is SmallCannonShellEntity && data.get(GunProp.SHELL_TYPE) == "AA") {
                entity.antiAir(true)
            }

            if (entity is CannonShellEntity) {
                val type = data.get(GunProp.SHELL_TYPE)
                when (type) {
                    "AP" -> {
                        entity.setType(CannonShellEntity.Type.AP)
                        entity.durability(data.get(GunProp.AP_DURABILITY))
                    }

                    "HE" -> {
                        entity.setType(CannonShellEntity.Type.HE)
                    }

                    "CM" -> {
                        entity.setType(CannonShellEntity.Type.CM)
                        entity.setSpreadAmount(data.get(GunProp.SPREAD_AMOUNT))
                        entity.setSpreadAngle(data.get(GunProp.SPREAD_ANGLE))
                    }

                    "WP" -> {
                        entity.setType(CannonShellEntity.Type.WP)
                        entity.setSpreadAmount(data.get(GunProp.SPREAD_AMOUNT))
                        entity.setSpreadAngle(data.get(GunProp.SPREAD_ANGLE))
                    }
                }
            }

            if (entity is MediumRocketEntity) {
                val type = data.get(GunProp.SHELL_TYPE)
                when (type) {
                    "AP" -> {
                        entity.setType(MediumRocketEntity.Type.AP)
                        entity.durability(data.get(GunProp.AP_DURABILITY))
                    }

                    "HE" -> {
                        entity.setType(MediumRocketEntity.Type.HE)
                    }

                    "CM" -> {
                        entity.setType(MediumRocketEntity.Type.CM)
                        entity.setSpreadAmount(data.get(GunProp.SPREAD_AMOUNT))
                        entity.setSpreadAngle(data.get(GunProp.SPREAD_ANGLE))
                    }
                }
            }

            if (entity is MissileProjectile && shooter != null) {
                val target = EntityFindUtil.findEntity(shooter.level(), uuid.toString())
                if (target != null) {
                    entity.setGuideType(0)
                    entity.setTargetUuid(uuid.toString())
                } else if (targetPos != null) {
                    entity.setGuideType(1)
                    entity.setTargetVec(targetPos)
                }
            }

            val vehicle = shooter?.vehicle as? VehicleEntity
            if (entity is SwarmDroneEntity && vehicle != null) {
                entity.setRotate(vehicle.getTurretVector(1f))
            }

            // 填充其他自定义NBT数据
            if (projectileInfo.data != null) {
                val tag = LaunchableEntityTool.getModifiedTag(
                    projectileInfo,
                    ShootData(
                        shooter?.getUUID(),
                        damage,
                        data.get(GunProp.EXPLOSION_DAMAGE),
                        data.get(GunProp.EXPLOSION_RADIUS),
                        data.get(GunProp.SPREAD)
                    )
                )
                if (tag != null) {
                    entity.load(tag)
                }
            } else if (CustomData.LAUNCHABLE_ENTITY.containsKey(projectileType)) {
                val newInfo = ProjectileInfo()
                newInfo.data = CustomData.LAUNCHABLE_ENTITY[projectileType]!!.data
                newInfo.itemId = projectileType

                val tag = LaunchableEntityTool.getModifiedTag(
                    newInfo,
                    ShootData(
                        shooter?.uuid,
                        damage,
                        data.get(GunProp.EXPLOSION_DAMAGE),
                        data.get(GunProp.EXPLOSION_RADIUS),
                        data.get(GunProp.SPREAD)
                    )
                )
                if (tag != null) {
                    entity.load(tag)
                }
            }
            entityHolder.set(entity)
        }

        val entity = entityHolder.get()
        if (entity == null) {
            Mod.LOGGER.warn("Failed to create projectile entity {}", projectileType)
            return false
        }

        for (type in Perk.Type.entries.toTypedArray()) {
            val instance = data.perk.getInstances(type)
            instance.forEach {
                it.perk.modifyProjectile(data, it, entity)
            }
        }

        val vehicle = shooter?.rootVehicle
        if (vehicle != null && data.get(GunProp.ADD_SHOOTER_DELTA_MOVEMENT)) {
            velocity = (vehicle.deltaMovement.length() * data.get(GunProp.VELOCITY)).toFloat()
        }

        // 发射任意实体
        entity.setPos(
            shootPosition.x,
            shootPosition.y,
            shootPosition.z
        )

        val x = shootDirection.x
        val y = shootDirection.y
        val z = shootDirection.z

        if (entity is Projectile) {
            entity.shoot(x, y, z, velocity, spread.toFloat())
        } else {
            val random = RandomSource.create()
            val vec3 = Vec3(x, y, z)
                .normalize()
                .add(
                    random.triangle(0.0, 0.0172275 * spread),
                    random.triangle(0.0, 0.0172275 * spread),
                    random.triangle(0.0, 0.0172275 * spread)
                )
                .scale(velocity.toDouble())

            entity.deltaMovement = vec3
            entity.hasImpulse = true
            val d0 = vec3.horizontalDistance()
            entity.yRot = (Mth.atan2(vec3.x, vec3.z) * 180f / Math.PI.toFloat()).toFloat()
            entity.xRot = (Mth.atan2(vec3.y, d0) * 180f / Math.PI.toFloat()).toFloat()
            entity.yRotO = entity.yRot
            entity.xRotO = entity.xRot
        }

        level.addFreshEntity(entity)
        return true
    }

    @Deprecated("")
    @Suppress("unused")
    fun shootBullet(
        shooter: Entity?,
        level: ServerLevel,
        shootPosition: Vec3,
        shootDirection: Vec3,
        data: GunData,
        spread: Double,
        zoom: Boolean,
        uuid: UUID?
    ): Boolean {
        return false
    }

    open fun shootRay(parameters: ShootParameters): Boolean {
        val shooter = parameters.shooter
        val level = parameters.level
        val data = parameters.data
        val shootPosition = parameters.shootPosition
        val shootDirection = parameters.shootDirection

        if (shooter == null) {
            return false
        }

        val range = data.get(GunProp.RANGE)

        var target: Entity? = null

        val distance = (range * range).toDouble()

        val blockHitResult = shooter.level().clip(
            ClipContext(
                shootPosition, shootPosition.add(shootDirection.scale(range.toDouble())),
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, shooter
            )
        )

        val blockPos = blockHitResult.blockPos
        val state = level.getBlockState(blockPos)

        var pos: Vec3? = null

        if (state.canOcclude()) {
            pos = blockHitResult.getLocation()
        }

        val toVec = shootPosition.add(shootDirection.x * range, shootDirection.y * range, shootDirection.z * range)
        val aabb = shooter.boundingBox.expandTowards(shootDirection.scale(range.toDouble())).inflate(1.0)
        val entityHitResult = ProjectileUtil.getEntityHitResult(
            shooter,
            shootPosition,
            toVec,
            aabb,
            { p -> !p.isSpectator && p.isAlive },
            distance
        )

        var hitPos: Vec3? = null

        if (entityHitResult != null) {
            hitPos = entityHitResult.getLocation()
            target = entityHitResult.entity
        }

        if (pos != null && hitPos != null) {
            if (shootPosition.distanceToSqr(pos) < shootPosition.distanceToSqr(hitPos)) {
                this.onRayHitBlock(shooter, level, target, data, shootDirection, blockHitResult, pos)
            } else {
                this.rayHitEntity(shooter, target, level, data, hitPos, shootPosition, shootDirection)
            }
            return true
        } else {
            val vehicle = shooter.vehicle as? VehicleEntity
            if (vehicle != null) {
                vehicle.laserLength = range.toFloat()
                vehicle.laserScale = data.get(GunProp.SHOOT_ANIMATION_TIME).toFloat()
            }
        }

        if (hitPos != null) {
            this.rayHitEntity(shooter, target, level, data, hitPos, shootPosition, shootDirection)
            return true
        }

        if (pos != null) {
            this.onRayHitBlock(shooter, level, target, data, shootDirection, blockHitResult, pos)
            return true
        }

        return true
    }

    protected fun rayHitEntity(
        shooter: Entity?,
        target: Entity?,
        level: ServerLevel,
        data: GunData,
        hitPos: Vec3,
        shootPosition: Vec3?,
        shootDirection: Vec3?
    ) {
        if (target != null && target.isAlive) {
            val hitBoxPos = hitPos.subtract(target.position())
            val res: EntityResult = getEntityResult(target, hitBoxPos, hitPos)
            this.onRayHitEntity(shooter, level, data, res, shootPosition, shootDirection)
        }
    }

    open fun onRayHitBlock(
        shooter: Entity?,
        level: ServerLevel,
        target: Entity?,
        data: GunData,
        shootDirection: Vec3?,
        result: BlockHitResult,
        pos: Vec3
    ) {
        val blockPos = result.blockPos
        if (target == null) {
            val bulletDecalOption = BulletDecalOption(result.direction, blockPos)
            ParticleTool.sendParticle(
                level,
                bulletDecalOption,
                pos.x,
                pos.y,
                pos.z,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                true
            )
        }
        level.playSound(
            null,
            pos.x,
            pos.y,
            pos.z,
            this.getRayHitBlockSound(data),
            SoundSource.BLOCKS,
            0.7f,
            ((2 * Math.random() - 1) * 0.05f + 1.0f).toFloat()
        )
    }

    open fun getRayHitBlockSound(data: GunData): SoundEvent = SoundEvents.EMPTY

    open fun getRayHitEntitySound(data: GunData): SoundEvent = SoundEvents.EMPTY

    open fun onRayHitEntity(
        shooter: Entity?,
        level: ServerLevel,
        data: GunData,
        result: EntityResult,
        shootPosition: Vec3?,
        shootDirection: Vec3?
    ) {
        val target = result.entity

        val damage = data.get(GunProp.DAMAGE).toFloat()
        val headshot = data.get(GunProp.HEADSHOT).toFloat()

        var type = 0

        if (target is LivingEntity) {
            val iCustomKnockback = ICustomKnockback.getInstance(target)
            iCustomKnockback.`superbWarfare$setKnockbackStrength`(0.0)

            if (result.headshot) {
                target.forceHurt(
                    ModDamageTypes.causeLaserHeadshotDamage(level.registryAccess(), null, shooter),
                    damage * headshot
                )
                type = 1
            } else if (result.legShot) {
                target.forceHurt(
                    ModDamageTypes.causeLaserDamage(level.registryAccess(), null, shooter),
                    damage * 0.5f
                )
            } else {
                target.forceHurt(
                    ModDamageTypes.causeLaserDamage(level.registryAccess(), null, shooter),
                    damage
                )
            }

            target.invulnerableTime = 0

            iCustomKnockback.`superbWarfare$resetKnockbackStrength`()
        } else {
            if (result.headshot) {
                target.forceHurt(
                    ModDamageTypes.causeLaserHeadshotDamage(level.registryAccess(), null, shooter),
                    damage * headshot
                )
                type = 1
            } else if (result.legShot) {
                target.forceHurt(
                    ModDamageTypes.causeLaserDamage(level.registryAccess(), null, shooter),
                    damage * 0.5f
                )
            } else {
                target.forceHurt(
                    ModDamageTypes.causeLaserDamage(level.registryAccess(), null, shooter),
                    damage
                )
            }

            if (target is VehicleEntity) {
                type = 3
            }
        }

        if (shooter is ServerPlayer) {
            shooter.level().playSound(
                null,
                shooter.blockPosition(),
                if (result.headshot) ModSounds.HEADSHOT.get() else ModSounds.INDICATION.get(),
                SoundSource.VOICE,
                0.1f,
                1f
            )
            shooter.sendPacket(ClientIndicatorMessage(type, 5))
        }

        level.playSound(
            null,
            result.hitVec.x,
            result.hitVec.y,
            result.hitVec.z,
            this.getRayHitEntitySound(data),
            SoundSource.PLAYERS,
            0.7f,
            ((2 * Math.random() - 1) * 0.05f + 1.0f).toFloat()
        )
    }

    protected fun randomVec(vec3: Vec3, spread: Double): Vec3 {
        return vec3.normalize().add(
            random.triangle(0.0, 0.0172275 * spread),
            this.random.triangle(0.0, 0.0172275 * spread),
            this.random.triangle(0.0, 0.0172275 * spread)
        )
    }

    open fun canEditAttachments(data: GunData) = data.get(GunProp.AMMO_CONSUMER).size > 1

    open fun enableShootTimer() = false

    /**
     * 在切枪之后触发
     */
    open fun onChangeSlot(data: GunData, ammoSupplier: Entity) {
        if (data.get(GunProp.WITHDRAW_AMMO_WHEN_CHANGE_SLOT)) {
            data.withdrawAmmo(ammoSupplier)
        }
    }

    @OnlyIn(Dist.CLIENT)
    override fun getItemScreen(stack: ItemStack, player: Player, hand: InteractionHand): Screen? {
        if (ClientEventHandler.canOpenEditScreen(stack, hand)
            && stack.item is GunItem
            && canEditAttachments(from(stack))
        ) {
            return WeaponEditScreen(stack)
        }
        return null
    }

    open fun tick(shooter: Entity?, data: GunData, inMainHand: Boolean) {}

    open fun getDefaultData(data: GunData) = getDefault(data.id)

    open fun getEnergyProvider(data: GunData, ammoSupplier: Entity?): IEnergyStorage? {
        return data.stack.getCapability(Capabilities.EnergyStorage.ITEM)
    }

    companion object {
        private val SPEED_ID = loc("gun_movement_speed")

        protected fun getEntityResult(target: Entity, hitBoxPos: Vec3, hitPos: Vec3): EntityResult {
            var headshot = false
            var legShot = false
            val eyeHeight = target.eyeHeight
            val bodyHeight = target.bbHeight

            if (target is LivingEntity) {
                if (eyeHeight - 0.25 < hitBoxPos.y && hitBoxPos.y < eyeHeight + 0.3) {
                    headshot = true
                }
                if (hitBoxPos.y < 0.33 * bodyHeight) {
                    legShot = true
                }
            }

            return EntityResult(target, hitPos, headshot, legShot)
        }
    }
}
