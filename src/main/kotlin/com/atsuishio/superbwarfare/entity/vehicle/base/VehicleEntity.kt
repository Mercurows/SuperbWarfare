package com.atsuishio.superbwarfare.entity.vehicle.base

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.queueServerWork
import com.atsuishio.superbwarfare.capability.energy.SyncedEntityEnergyStorage
import com.atsuishio.superbwarfare.capability.energy.VehicleEnergyStorage
import com.atsuishio.superbwarfare.client.particle.CannonMuzzleFlareOption
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.data.DataLoader
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.ShootParameters
import com.atsuishio.superbwarfare.data.loot.WreckageLootData
import com.atsuishio.superbwarfare.data.loot.WreckageLootDataManager
import com.atsuishio.superbwarfare.data.vehicle.VehicleData
import com.atsuishio.superbwarfare.data.vehicle.VehiclePropertyModifier
import com.atsuishio.superbwarfare.data.vehicle.subdata.*
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo.*
import com.atsuishio.superbwarfare.entity.OBBEntity
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.mixin.OBBHitter
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity
import com.atsuishio.superbwarfare.entity.vehicle.Tom6Entity
import com.atsuishio.superbwarfare.entity.vehicle.TurretWreckEntity
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleMiscUtils
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleMotionUtils
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getXRotFromVector
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getYRotFromVector
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleWeaponUtils
import com.atsuishio.superbwarfare.event.ClientMouseHandler
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.inventory.handler.VehicleContainerHandler
import com.atsuishio.superbwarfare.inventory.menu.*
import com.atsuishio.superbwarfare.item.container.ContainerBlockItem
import com.atsuishio.superbwarfare.item.curio.DogTagItem
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.network.message.receive.ClientVehicleItemMessage
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.tools.OBB.Part.*
import com.atsuishio.superbwarfare.tools.RangeTool.calculateFiringSolution
import com.atsuishio.superbwarfare.tools.VectorTool.combineRotationsTurret
import com.atsuishio.superbwarfare.world.saveddata.TDMSavedData
import com.google.common.collect.ImmutableList
import com.mojang.math.Axis
import net.minecraft.ChatFormatting
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.core.NonNullList
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.TicketType
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.ContainerHelper
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.AbstractArrow
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.entity.vehicle.DismountHelper
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.NameTagItem
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.common.util.FakePlayer
import net.neoforged.neoforge.energy.IEnergyStorage
import net.neoforged.neoforge.items.ItemHandlerHelper
import org.joml.*
import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import javax.annotation.ParametersAreNonnullByDefault
import kotlin.math.*
import kotlin.random.Random

abstract class VehicleEntity(pEntityType: EntityType<*>, pLevel: Level) : Entity(pEntityType, pLevel),
    VehiclePropertyModifier, HasCustomInventoryScreen, OBBEntity {

    open var gunDataMap: Map<String, GunData>
        get() {
            val rawMap = entityData.get(GUN_DATA_MAP)
            val newMap = mutableMapOf<String, GunData>()
            val weapons = computed().weapons()

            for (kv in weapons.entries) {
                val oldData = rawMap[kv.key]
                val stack = oldData?.stack?.copy() ?: ItemStack(ModItems.VEHICLE_GUN.get())
                val data = GunData.from(stack) { kv.value }

                newMap[kv.key] = data
            }

            return newMap.toMap()
        }
        set(value) = this.entityData.set(GUN_DATA_MAP, value.toMap())

    open fun getSeat(seatIndex: Int) =
        computed().seats().getOrNull(seatIndex)

    open fun getSeat(passenger: Entity?): SeatInfo? {
        return getSeat(getSeatIndex(passenger))
    }

    /**
     * 获取载具座位上选中的武器
     *
     * @param seatIndex 座位号
     * @return 武器数据
     */
    open fun getGunData(seatIndex: Int): GunData? {
        return getGunData(seatIndex, selectedWeapon.getOrNull(seatIndex) ?: return null)
    }

    /**
     * 获取载具座位上指定编号的武器
     *
     * @param seatIndex   座位号
     * @param weaponIndex 武器号
     * @return 武器数据
     */
    open fun getGunData(seatIndex: Int, weaponIndex: Int): GunData? {
        val seat = getSeat(seatIndex) ?: return null

        val name = seat.weapons().getOrNull(weaponIndex) ?: return null

        return getGunData(name)
    }

    /**
     * 获取载具的乘客座位上指定编号的武器
     *
     * @param passenger   乘客
     * @param weaponIndex 武器号
     * @return 武器数据
     */
    open fun getGunData(passenger: Entity?, weaponIndex: Int) = getGunData(getSeatIndex(passenger), weaponIndex)

    /**
     * 获取载具的乘客座位上选中的武器
     *
     * @param passenger 乘客
     * @return 武器数据
     */
    open fun getGunData(passenger: Entity?) =
        getGunData(passenger, this.getSelectedWeapon(this.getSeatIndex(passenger)))

    /**
     * 根据名称获取武器
     *
     * @param name 武器名称
     * @return 武器数据
     */
    open fun getGunData(name: String) = this.gunDataMap[name]

    open fun getGunName(seatIndex: Int): String? {
        if (seatIndex < 0) return null
        val seat = getSeat(seatIndex) ?: return null

        val weaponIndex = selectedWeapon.getOrNull(seatIndex) ?: return null
        if (weaponIndex < 0) return null

        val weapons = seat.weapons()
        if (weaponIndex >= weapons.size) return null

        return getGunName(seatIndex, weaponIndex)
    }

    open fun getGunName(seatIndex: Int, weaponIndex: Int): String? {
        return getSeat(seatIndex)?.weapons()?.getOrNull(weaponIndex)
    }

    open fun modifyGunData(seatIndex: Int, weaponIndex: Int, consumer: Consumer<GunData>) {
        modifyGunData(getGunName(seatIndex, weaponIndex), consumer)
    }

    open fun modifyGunData(seatIndex: Int, consumer: Consumer<GunData>) {
        modifyGunData(getGunName(seatIndex), consumer)
    }

    open fun modifyGunData(name: String?, consumer: Consumer<GunData>) {
        if (name == null) return

        val map = this.gunDataMap.toMutableMap()
        var data = getGunData(name) ?: return

        data = data.copy()
        consumer.accept(data)
        data.save()
        map[name] = data

        gunDataMap = map
    }

    private var obbCache: MutableList<OBB>? = null
    open var obb = listOf<OBBInfo>()
        protected set

    open var engineInfo: EngineInfo? = null

    protected var interpolationSteps = 0
    protected var xO = 0.0
    protected var yO = 0.0
    protected var zO = 0.0

    open var roll = 0f

    open var prevRoll = 0f
    open var repairCoolDown = maxRepairCoolDown()

    open var crash = false

    open var turretYRot = 0f
    open var turretXRot = 0f
    open var turretYRotO = 0f
    open var turretXRotO = 0f
    open var turretYRotLock = 0f

    open var gunYRot = 0f
    open var gunXRot = 0f
    open var gunYRotO = 0f
    open var gunXRotO = 0f

    protected var noPassengerTime = 0
    protected var damageDebugResultReceiver: Player? = null

    open var decoyReloadCoolDown = 0

    open var lastTickSpeed = 0.0
    protected var lastTickVerticalSpeed = 0.0

    open var collisionCoolDown = 0

    private var wasEngineRunning = false
    private var wasHornWorking = false
    private var wasStuka = false
    private var wasHeliCrash = false
    private var wasVehicleSkip = false

    //    private var wasInCarMusicPlaying = false;
    private var wasFiring = false

    open var targetSpeed = 0.0

    open var rudderRot = 0f
    open var rudderRotO = 0f
    open var leftWheelRot = 0f
    open var rightWheelRot = 0f
    open var leftWheelRotO = 0f
    open var rightWheelRotO = 0f

    open var leftTrackO = 0f
    open var rightTrackO = 0f
    open var leftTrack = 0f
    open var rightTrack = 0f

    open var propellerRot = 0f
    open var propellerRotO = 0f

    open var recoilShake = 0.0
    open var recoilShakeO = 0.0

    open var flap1LRot = 0f
    open var flap1LRotO = 0f
    open var flap1RRot = 0f
    open var flap1RRotO = 0f
    open var flap1L2Rot = 0f
    open var flap1L2RotO = 0f
    open var flap1R2Rot = 0f
    open var flap1R2RotO = 0f
    open var flap2LRot = 0f
    open var flap2LRotO = 0f
    open var flap2RRot = 0f
    open var flap2RRotO = 0f
    open var flap3Rot = 0f
    open var flap3RotO = 0f
    private var gearRotO = 0f

    open var gearRot = 0f

    open var engineStart = false
    open var engineStartOver = false
    open var holdTick = 0
    open var holdPowerTick = 0

    open var destroyRot = 0f

    open var jumpCoolDown = 0
    open var deltaMovementO: Vec3 = deltaMovement
    open var positionO: Vec3 = Vec3.ZERO

    open var absoluteSpeed = 0.0
    open var absoluteSpeedO = 0.0
    open var absoluteSpeedLerp = 0.0

    var pitchAngle = 0f
    var pitchVelocity = 0f
    var prevPitchAngle = 0f
    var rollAngle = 0f
    var rollVelocity = 0f
    var prevRollAngle = 0f
    var prevMotion: Vec3? = null

    open var lastDamageSource: DamageSource? = null
        get() {
            if (this.level().gameTime - this.lastDamageStamp > 40L) {
                this.lastDamageSource = null
            }
            return field
        }
    open var lastDamageStamp: Long = 0

    private fun initOBB() {
        this.obb = data().getDefault().copy().obb.toList()
    }

    override fun onSyncedDataUpdated(dataValues: MutableList<DataValue<*>>) {
        super.onSyncedDataUpdated(dataValues)

        data().update()
    }

    open fun processInput(keys: Short) {
        leftInputDown =
            (keys.toInt() and 0b00000001) > 0
        rightInputDown =
            (keys.toInt() and 0b00000010) > 0
        forwardInputDown =
            (keys.toInt() and 0b00000100) > 0
        backInputDown =
            (keys.toInt() and 0b00001000) > 0
        upInputDown =
            (keys.toInt() and 0b00010000) > 0
        downInputDown =
            (keys.toInt() and 0b00100000) > 0
        decoyInputDown =
            (keys.toInt() and 0b01000000) > 0
        fireInputDown =
            (keys.toInt() and 0b10000000) > 0
        sprintInputDown =
            (keys.toInt() and 256) > 0
    }

    @get:JvmName("forwardInputDown")
    open var forwardInputDown by FORWARD_INPUT_DOWN

    @get:JvmName("backInputDown")
    open var backInputDown by BACK_INPUT_DOWN

    @get:JvmName("leftInputDown")
    open var leftInputDown by LEFT_INPUT_DOWN

    @get:JvmName("rightInputDown")
    open var rightInputDown by RIGHT_INPUT_DOWN

    @get:JvmName("upInputDown")
    open var upInputDown by UP_INPUT_DOWN

    @get:JvmName("downInputDown")
    open var downInputDown by DOWN_INPUT_DOWN

    @get:JvmName("fireInputDown")
    open var fireInputDown by FIRE_INPUT_DOWN

    @get:JvmName("decoyInputDown")
    open var decoyInputDown by DECOY_INPUT_DOWN

    @get:JvmName("sprintInputDown")
    open var sprintInputDown by SPRINT_INPUT_DOWN

    open fun mouseInput(x: Double, y: Double) {
        mouseMoveSpeedX = x.toFloat()
        mouseMoveSpeedY = y.toFloat()
    }

    open var mouseMoveSpeedX by MOUSE_SPEED_X
    open var mouseMoveSpeedY by MOUSE_SPEED_Y

    // container start
    val inventory = VehicleContainerHandler(6 * 17, this)

    open fun getItems() = this.inventory.getItems()

    protected fun resizeItems() {
        val newSize = this.getContainerSize()
        val oldSize = inventory.slots
        if (newSize == oldSize) return

        val oldStacks = NonNullList.withSize(oldSize, ItemStack.EMPTY)
        for (i in 0 until oldSize) {
            oldStacks[i] = inventory.getStackInSlot(i)
        }

        inventory.setSize(newSize)

        for (i in 0 until minOf(oldSize, newSize)) {
            inventory.setStackInSlot(i, oldStacks[i])
        }

        if (newSize < oldSize) {
            for (i in newSize until oldSize) {
                val stack = oldStacks[i]
                if (!stack.isEmpty) {
                    spawnAtLocation(stack, 0.5f)
                }
            }
        }
    }

    open fun getContainerSize(): Int {
        return computed().vehicleContainerType.size
    }

    fun getItem(slot: Int): ItemStack {
        if (!this.hasContainer() || slot >= this.getContainerSize() || slot < 0) return ItemStack.EMPTY
        return this.inventory.getStackInSlot(slot)
    }

    fun removeItem(slot: Int, pAmount: Int): ItemStack {
        if (!this.hasContainer() || slot >= this.getContainerSize() || slot < 0) return ItemStack.EMPTY
        return this.inventory.extractItem(slot, pAmount, false)
    }

    open var maxStackSize: Int = 64

    fun setItem(slot: Int, pStack: ItemStack) {
        if (!this.hasContainer() || slot >= this.getContainerSize() || slot < 0) return

        val limit = min(this.maxStackSize, pStack.maxStackSize)
        if (!pStack.isEmpty && pStack.count > limit) {
            Mod.LOGGER.warn(
                "try inserting ItemStack {} exceeding the maximum stack size: {}, clamped to {}",
                pStack.item,
                limit,
                limit
            )
            pStack.count = limit
        }
        this.inventory.setStackInSlot(slot, pStack)
    }

    open fun setChanged() {
        if (!this.level().isClientSide) {
            sendPacketToTrackingThis(
                ClientVehicleItemMessage(
                    this.id,
                    inventory.serializeNBT(this.level().registryAccess())
                )
            )
        }
    }

    fun clearContent() {
        this.inventory.clear()
    }

    open fun hasContainer() = this.getContainerSize() > 0

    open fun canPlaceItem(slot: Int, stack: ItemStack): Boolean {
        if (!this.hasContainer() || slot >= this.getContainerSize() || slot < 0) return false

        val currentStack = this.inventory.getStackInSlot(slot)
        if (!currentStack.isEmpty && currentStack.item !== stack.item) return false

        val currentCount = currentStack.count
        val stackCount = stack.count
        val combinedCount = currentCount + stackCount
        return !(combinedCount > this.maxStackSize || combinedCount > stack.maxStackSize)
    }

    fun canTakeItem(slot: Int): Boolean {
        return this.hasContainer() && slot in 0 until this.getContainerSize()
    }

    override fun remove(reason: RemovalReason) {
        if (!this.level().isClientSide && reason != RemovalReason.DISCARDED && reason != RemovalReason.UNLOADED_WITH_PLAYER) {
            for (i in 0 until inventory.slots) {
                val stack = inventory.getStackInSlot(i)
                if (!stack.isEmpty) {
                    spawnAtLocation(stack, 0.5f)
                }
            }
        }
        super.remove(reason)
    }

    override fun openCustomInventoryScreen(player: Player) {
        if (player is ServerPlayer) {
            this.openMenu(player)
        }
    }

    open fun hasMenu() = computed().vehicleContainerType.hasMenu()

    open fun openMenu(player: Player) {
        if (player is ServerPlayer) {
            player.openMenu(
                SimpleMenuProvider(
                    { containerId, inv, player -> createMenu(containerId, inv, player) },
                    Component.translatable(this.type.descriptionId)
                )
            ) { buf -> buf.writeInt(this.id) }
        }
    }

    open fun createMenu(
        pContainerId: Int,
        pPlayerInventory: Inventory,
        pPlayer: Player
    ): AbstractContainerMenu? {
        if (!pPlayer.isSpectator && this.hasMenu()) {
            val computed = computed()
            val type = computed.vehicleContainerType
            if (!type.hasMenu()) return null
            return when (type) {
                VehicleContainerType.MINI -> MiniVehicleContainerMenu(pContainerId, pPlayerInventory, this.id)
                VehicleContainerType.SMALL -> SmallVehicleContainerMenu(pContainerId, pPlayerInventory, this.id)
                VehicleContainerType.MEDIUM -> MediumVehicleContainerMenu(pContainerId, pPlayerInventory, this.id)
                VehicleContainerType.LARGE -> LargeVehicleContainerMenu(pContainerId, pPlayerInventory, this.id)
                VehicleContainerType.HUGE -> HugeVehicleContainerMenu(pContainerId, pPlayerInventory, this.id)
                else -> null
            }
        }
        return null
    }

    // container end
    // 自定义骑乘
    private val orderedPassengers: MutableList<Entity?> = generatePassengersList()

    private fun generatePassengersList() = MutableList(maxPassengers) { null as Entity? }

    protected fun initSeatData(targetSize: Int) {
        padList(orderedPassengers, targetSize, null, null)
    }

    protected fun <T> padList(list: MutableList<T?>, targetSize: Int, defaultValue: T?, onRemove: Consumer<T>?) {
        while (targetSize != list.size) {
            if (targetSize > list.size) {
                list.add(defaultValue)
            } else {
                val last = list.removeLast()
                if (last != null && onRemove != null) {
                    onRemove.accept(last)
                }
            }
        }
    }

    protected fun checkSeatsSize() {
        val targetSize = computed().seats().size
        if (targetSize == orderedPassengers.size) return

        initSeatData(targetSize)
    }

    /**
     * 获取按顺序排列的成员列表
     *
     * @return 按顺序排列的成员列表
     */
    open fun getOrderedPassengers(): MutableList<Entity?> {
        checkSeatsSize()
        return orderedPassengers
    }

    // 仅在客户端存在的实体顺序获取，用于在客户端正确同步实体座位顺序
    open var entityIndexOverride: Function<Entity, Int>? = null

    override fun addPassenger(pPassenger: Entity) {
        check(pPassenger.vehicle === this) { "Use x.startRiding(y), not y.addPassenger(x)" }
        checkSeatsSize()

        var index: Int

        val indexOverride = entityIndexOverride
        if (indexOverride != null && indexOverride.apply(pPassenger) != -1) {
            index = indexOverride.apply(pPassenger)
        } else {
            index = 0
            for (passenger in orderedPassengers) {
                if (passenger == null) {
                    break
                }
                index++
            }
        }
        if (index >= this.maxPassengers || index < 0) return

        orderedPassengers[index] = pPassenger

        pPassenger.persistentData.putInt(TAG_SEAT_INDEX, index)

        this.passengers =
            ImmutableList.copyOf(orderedPassengers.stream().filter { obj: Entity? -> Objects.nonNull(obj) }.toList())
        this.gameEvent(GameEvent.ENTITY_MOUNT, pPassenger)

        this.setChanged()
    }

    override fun removePassenger(pPassenger: Entity) {
        check(pPassenger.vehicle !== this) { "Use x.stopRiding(y), not y.removePassenger(x)" }
        checkSeatsSize()

        val index = getSeatIndex(pPassenger)
        if (index == -1) return

        orderedPassengers[index] = null
        this.passengers =
            ImmutableList.copyOf(orderedPassengers.stream().filter { obj: Entity? -> Objects.nonNull(obj) }
                .toList())

        pPassenger.boardingCooldown = 60
        this.gameEvent(GameEvent.ENTITY_DISMOUNT, pPassenger)
    }

    open fun data() = VehicleData.from(this)
    open fun computed() = VehicleData.compute(this)

    override fun maxUpStep() = computed().upStep

    override fun getFirstPassenger(): Entity? {
        checkSeatsSize()
        if (orderedPassengers.isEmpty()) {
            return null
        }
        return orderedPassengers.firstOrNull()
    }

    /**
     * 获取第index个乘客
     *
     * @param index 目标座位
     * @return 目标座位的乘客
     */
    open fun getNthEntity(index: Int): Entity? {
        checkSeatsSize()
        if (index >= orderedPassengers.size || index < 0) {
            return null
        }
        return orderedPassengers[index]
    }

    /**
     * 尝试切换座位
     *
     * @param entity 乘客
     * @param index  目标座位
     * @return 是否切换成功
     */
    open fun changeSeat(entity: Entity, index: Int): Boolean {
        if (index < 0 || index >= this.maxPassengers) return false
        checkSeatsSize()
        if (orderedPassengers[index] != null) return false
        if (!orderedPassengers.contains(entity)) return false

        orderedPassengers[orderedPassengers.indexOf(entity)] = null
        orderedPassengers[index] = entity

        entity.persistentData.putInt(TAG_SEAT_INDEX, index)

        // 在服务端运行时，向所有玩家同步载具座位信息
        val level = this.level()
        if (level is ServerLevel) {
            level.getPlayers { true }
                .forEach { p -> p!!.connection.send(ClientboundSetPassengersPacket(this)) }
        }

        return true
    }

    /**
     * 获取乘客所在座位索引
     *
     * @param entity 乘客
     * @return 座位索引
     */
    open fun getSeatIndex(entity: Entity?): Int {
        checkSeatsSize()
        return orderedPassengers.indexOf(entity)
    }

    /**
     * 获取乘客所在座位索引，用于下车时的位置判定
     * 下车前会先移除载具，因此 [VehicleEntity.getSeatIndex] 会返回-1
     *
     * @param entity 乘客
     * @return 座位索引
     */
    open fun getTagSeatIndex(entity: Entity) = entity.persistentData.getInt(TAG_SEAT_INDEX)

    open val thirdPersonCameraPosition: Vec3
        get() {
            val pos = computed().thirdPersonCameraPos
            return Vec3(pos.z + ClientMouseHandler.custom3pDistanceLerp, pos.y, pos.x)
        }

    open fun getRoll(tickDelta: Float) = Mth.lerp(tickDelta, prevRoll, roll)
    open fun getYaw(tickDelta: Float) = Mth.lerp(tickDelta, yRotO, yRot)
    open fun getPitch(tickDelta: Float) = Mth.lerp(tickDelta, xRotO, xRot)

    open fun setZRot(rot: Float) {
        roll = rot
    }

    open fun turretTurnSound(diffX: Float, diffY: Float, pitch: Float) {
        if (this is MortarEntity) return
        if (level().isClientSide && (Math.abs(diffY) > 0.5 || Math.abs(diffX) > 0.5)) {
            level().playLocalSound(
                this.x,
                this.y + this.bbHeight * 0.5,
                this.z,
                ModSounds.TURRET_TURN.get(),
                this.soundSource,
                min(0.15 * (max(Mth.abs(diffX), Mth.abs(diffY))), 0.75).toFloat(),
                (random.nextFloat() * 0.05f + pitch),
                false
            )
        }
    }

    /**
     * 受击时是否出现粒子效果
     */
    open fun shouldSendHitParticles() = computed().sendHitParticles

    /**
     * 受击时是否出现音效
     */
    open fun shouldSendHitSounds() = true

    private var energyStorage: IEnergyStorage? = null

    var isInitialized: Boolean
        protected set

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        with(builder) {
            define(OVERRIDE, "")
            define(HEALTH, getMaxHealth())
            define(LAST_ATTACKER_UUID, "undefined")
            define(LAST_DRIVER_UUID, "undefined")
            define(DOG_TAG_ICON, List(16) { List(16) { -1 } })
            define(GUN_DATA_MAP, mapOf())

            define(AI_TURRET_TARGET_UUID, "undefined")
            define(AI_PASSENGER_WEAPON_TARGET_UUID, "undefined")

            define(DELTA_ROT, 0f)
            define(MOUSE_SPEED_X, 0f)
            define(MOUSE_SPEED_Y, 0f)

            define(TURRET_HEALTH, getTurretMaxHealth())
            define(L_WHEEL_HEALTH, getWheelMaxHealth())
            define(R_WHEEL_HEALTH, getWheelMaxHealth())
            define(MAIN_ENGINE_HEALTH, getEngineMaxHealth())
            define(SUB_ENGINE_HEALTH, getEngineMaxHealth())

            define(TURRET_DAMAGED, false)
            define(L_WHEEL_DAMAGED, false)
            define(R_WHEEL_DAMAGED, false)
            define(MAIN_ENGINE_DAMAGED, false)
            define(SUB_ENGINE_DAMAGED, false)

            define(CANNON_RECOIL_TIME, 0)
            define(CANNON_RECOIL_FORCE, 0f)
            define(POWER, 0f)
            define(YAW_WHILE_SHOOT, 0f)
            define(SERVER_YAW, yRot)
            define(SERVER_PITCH, xRot)
            define(DECOY_READY, false)
            define(SYNCHED_GEAR_ROT, 0f)
            define(GEAR_UP, false)
            define(FORWARD_INPUT_DOWN, false)
            define(BACK_INPUT_DOWN, false)
            define(LEFT_INPUT_DOWN, false)
            define(RIGHT_INPUT_DOWN, false)
            define(UP_INPUT_DOWN, false)
            define(DOWN_INPUT_DOWN, false)
            define(FIRE_INPUT_DOWN, false)
            define(DECOY_INPUT_DOWN, false)
            define(SPRINT_INPUT_DOWN, false)

            define(PLANE_BREAK, 0f)
            define(SELECTED_WEAPON, List(maxPassengers) { 0 })
            define(ENERGY, 0)
            define(SYNCHED_PROPELLER_ROT, 0f)

            define(HORN_VOLUME, 0f)
            define(LASER_LENGTH, 0f)
            define(LASER_SCALE, 0f)
            define(LASER_SCALE_O, 0f)
            define(CHARGE_PROGRESS, 0f)
            define(IS_WRECK, false)
            define(SYMPATHETIC_DETONATED, false)
            define(TURRET_BURNED, false)
            define(HOVER_MODE, false)
            define(TURRET_BURN_TIMER, 0)
        }
    }

    // energy start
    /**
     * 消耗指定电量
     *
     * @param amount 要消耗的电量
     */
    open fun consumeEnergy(amount: Int) {
        if (!this.hasEnergyStorage()) {
            Mod.LOGGER.warn("Trying to consume energy of vehicle {}, but it has no energy storage", this.name)
            return
        }
        if (this.level() is ServerLevel) {
            this.energyStorage!!.extractEnergy(amount, false)
        }
    }

    protected fun canConsume(amount: Int): Boolean {
        if (!this.hasEnergyStorage()) {
            Mod.LOGGER.warn(
                "Trying to check if can consume energy of vehicle {}, but it has no energy storage",
                this.name
            )
            return false
        }
        return this.energy >= amount
    }

    open var energy: Int
        get() {
            if (!this.hasEnergyStorage()) {
                Mod.LOGGER.warn(
                    "Trying to get energy of vehicle {}, but it has no energy storage",
                    this.name
                )
                return Int.MAX_VALUE
            }
            return this.energyStorage!!.energyStored
        }
        set(pEnergy) {
            if (!this.hasEnergyStorage()) {
                Mod.LOGGER.warn(
                    "Trying to set energy of vehicle {}, but it has no energy storage",
                    this.name
                )
                return
            }
            val targetEnergy = Mth.clamp(pEnergy, 0, this.maxEnergy)

            if (targetEnergy > energyStorage!!.energyStored) {
                energyStorage!!.receiveEnergy(targetEnergy - energyStorage!!.energyStored, false)
            } else {
                energyStorage!!.extractEnergy(energyStorage!!.energyStored - targetEnergy, false)
            }
        }

    open fun getEnergyStorage(): IEnergyStorage? {
        if (!this.hasEnergyStorage()) {
            Mod.LOGGER.warn("Trying to get energy storage of vehicle {}, but it has no energy storage", this.name)
        }
        return this.energyStorage
    }

    open val maxEnergy: Int
        get() = if (!this.hasEnergyStorage()) {
            Mod.LOGGER.warn(
                "Trying to get max energy of vehicle {}, but it has no energy storage",
                this.name
            )
            Int.MAX_VALUE
        } else computed().maxEnergy


    open fun hasEnergyStorage() = this.computed().maxEnergy > 0

    // energy end
    /**
     * 当前情况载具是否可以开火
     *
     * @param living 玩家
     * @return 是否可以开火
     */
    open fun canShoot(living: LivingEntity?): Boolean {
        val gunData = getGunData(getSeatIndex(living))
        return gunData != null && gunData.canShoot(this.ammoSupplier)
    }

    /**
     * 主武器射速
     *
     * @return 射速
     */
    open fun vehicleWeaponRpm(living: LivingEntity?): Int {
        val data = getGunData(getSeatIndex(living))
        if (data == null || data.get(GunProp.RPM) <= 0) return 60
        return data.get(GunProp.RPM)
    }

    open fun vehicleWeaponRpm(seatIndex: Int): Int {
        val data = getGunData(seatIndex)
        if (data == null || data.get(GunProp.RPM) <= 0) return 60
        return data.get(GunProp.RPM)
    }

    open fun vehicleWeaponRpm(weaponName: String): Int {
        val data = getGunData(weaponName) ?: return 1
        return data.get(GunProp.RPM).coerceAtLeast(1)
    }

    open fun getWeaponHeat(living: LivingEntity?): Int {
        val gunData = getGunData(getSeatIndex(living)) ?: return 0
        return Math.round(gunData.heat.get()).toInt()
    }

    open fun getWeaponHeat(seatIndex: Int): Int {
        val gunData = getGunData(seatIndex) ?: return 0
        return Math.round(gunData.heat.get()).toInt()
    }

    open fun getWeaponHeat(weaponName: String): Int {
        val gunData = getGunData(weaponName) ?: return 0
        return Math.round(gunData.heat.get()).toInt()
    }

    open fun getWeaponHeat(seatIndex: Int, weaponIndex: Int): Int {
        val gunData = getGunData(seatIndex, weaponIndex) ?: return 0
        return Math.round(gunData.heat.get()).toInt()
    }

    open fun getShootAnimationTimer(weaponName: String): Int {
        val gunData = getGunData(weaponName) ?: return 0
        return gunData.shootAnimationTimer.get()
    }

    open fun getShootAnimationTimer(seatIndex: Int, weaponIndex: Int): Int {
        val gunData = getGunData(seatIndex, weaponIndex) ?: return 0
        return gunData.shootAnimationTimer.get()
    }

    open fun vehicleShoot(living: LivingEntity?, weaponName: String) {
        if (isWreck) return
        modifyGunData(weaponName) { data ->
            if (!data.canShoot(this.ammoSupplier)) return@modifyGunData
            data.shoot(
                ShootParameters(
                    this.ammoSupplier,
                    living,
                    this.level() as ServerLevel,
                    getShootPos(weaponName, 1f),
                    getShootVec(weaponName, 1f),
                    data,
                    data.get(GunProp.SPREAD),
                    true,
                    null,
                    null
                )
            )
        }

        val gunData = getGunData(weaponName)
        afterShoot(gunData, getShootVec(weaponName, 1f))
        playShootSound3p(living, weaponName)
    }

    open fun vehicleShoot(living: LivingEntity?, uuid: UUID?, targetPos: Vec3?) {
        if (isWreck) return
        val seatIndex = getSeatIndex(living)
        modifyGunData(seatIndex) { data ->
            if (!data.canShoot(this.ammoSupplier)) return@modifyGunData
            data.shoot(
                ShootParameters(
                    this.ammoSupplier,
                    living,
                    this.level() as ServerLevel,
                    getShootPos(living, 1f),
                    getShootVec(living, 1f),
                    data,
                    data.get(GunProp.SPREAD),
                    true,
                    uuid,
                    targetPos
                )
            )
        }

        val gunData = getGunData(seatIndex)
        afterShoot(gunData, getShootVec(living, 1f))
        playShootSound3p(living, seatIndex)
    }

    open fun afterShoot(gunData: GunData?, shootVec: Vec3) {
        if (gunData != null) {
            val recoilTime = gunData.get(GunProp.RECOIL_TIME)
            if (recoilTime > 0) {
                if (recoilTime > cannonRecoilTime) {
                    cannonRecoilTime = recoilTime
                }

                val angle = Mth.wrapDegrees(
                    -getYRotFromVector(getViewVector(1f)) + getYRotFromVector(shootVec)
                ).toFloat()

                val vo = Vec3(0.0, 0.0, 1.0)
                val f =
                    0.3 * cannonRecoilForce * (cannonRecoilTime / recoilTime).toDouble()
                val v1 = vo.yRot(yawWhileShoot * Mth.DEG_TO_RAD).scale(f)
                val v2 = vo.yRot(angle * Mth.DEG_TO_RAD).scale(gunData.get(GunProp.RECOIL_FORCE).toDouble())
                val v3 = v1.add(v2)

                yawWhileShoot =
                    Mth.wrapDegrees(-getYRotFromVector(vo) + getYRotFromVector(v3))
                        .toFloat()
                cannonRecoilForce = v3.length().toFloat()

                gunData.shakePlayers(this)
            }
        }
    }

    open fun playShootSound3p(living: LivingEntity?, weaponName: String) {
        val gunData = this.getGunData(weaponName) ?: return
        val pos = getShootPos(weaponName, 1f)

        playShootSound3p(living, gunData, pos)
    }

    open fun playShootSound3p(living: LivingEntity?, seatIndex: Int) {
        val gunData = this.getGunData(seatIndex) ?: return
        val pos = getShootPos(living, 1f)

        playShootSound3p(living, gunData, pos)
    }

    open fun playShootSound3p(living: LivingEntity?, gunData: GunData?, pos: Vec3?) {
        val serverLevel = this.level() as? ServerLevel ?: return

        if (gunData == null) return

        val soundInfo = gunData.get(GunProp.SOUND_INFO)
        val pitch = if (getWeaponHeat(living) <= 60) 1f else (1 - 0.011 * abs(60 - getWeaponHeat(living))).toFloat()

        val listener: Entity?

        if (living != null && (living.vehicle !== this || living.vehicle == null)) {
            listener = null
        } else {
            val shootGunData = getGunData(living)
            listener = if (shootGunData != null && shootGunData === gunData) {
                living
            } else {
                null
            }
        }

        val soundRadius = gunData.get(GunProp.SOUND_RADIUS)
        val fire3P = soundInfo.fire3P
        if (fire3P != null) {
            pos?.let {
                SoundTool.playDistantSound(
                    serverLevel,
                    fire3P,
                    it,
                    (soundRadius * 0.4f).toFloat(),
                    pitch,
                    listener
                )
            }
        }

        val fire3PFar = soundInfo.fire3PFar
        if (fire3PFar != null) {
            pos?.let {
                SoundTool.playDistantSound(
                    serverLevel,
                    fire3PFar,
                    it,
                    (soundRadius * 0.7f).toFloat(),
                    pitch,
                    listener
                )
            }
        }

        val fire3PVeryFar = soundInfo.fire3PVeryFar
        if (fire3PVeryFar != null) {
            pos?.let {
                SoundTool.playDistantSound(
                    serverLevel,
                    fire3PVeryFar,
                    it,
                    soundRadius.toFloat(),
                    pitch,
                    listener
                )
            }
        }
    }

    /**
     * 获取该槽位当前的武器编号，返回-1则表示该位置没有可用武器
     *
     * @param seatIndex 槽位
     * @return 武器类型
     */
    open fun getWeaponIndex(seatIndex: Int) =
        selectedWeapon.getOrElse(seatIndex) { -1 }

    /**
     * 检测载具是否有武器
     *
     * @return 是否有武器
     */
    open fun hasWeapon(): Boolean {
        return this.computed().seats().stream()
            .filter { seat: SeatInfo? -> seat!!.weapons().isNotEmpty() }
            .flatMap { seat: SeatInfo? -> seat!!.weapons().stream() }
            .filter { name: String? -> !name.isNullOrEmpty() }
            .anyMatch { name -> this.getGunData(name) != null }
    }

    /**
     * 检测该槽位是否有可用武器
     *
     * @param seatIndex 武器槽位
     * @return 武器是否可用
     */
    open fun hasWeapon(seatIndex: Int): Boolean {
        if (seatIndex < 0 || seatIndex >= this.maxPassengers) return false
        return this.getGunData(seatIndex) != null
    }

    /**
     * 设置该槽位当前的武器编号
     *
     * @param seatIndex      武器槽位
     * @param selectedWeaponIndex 武器类型
     */
    open fun setWeaponIndex(seatIndex: Int, selectedWeaponIndex: Int) {
        val oldIndex = selectedWeapon.getOrNull(seatIndex) ?: return
        if (oldIndex == selectedWeaponIndex) return

        this.modifyGunData(seatIndex, oldIndex) { gunData ->
            if (gunData.get(GunProp.WITHDRAW_AMMO_WHEN_CHANGE_SLOT)) {
                gunData.withdrawAmmo(this.ammoSupplier)
            }
        }

        val newList = selectedWeapon.toMutableList()
        newList[seatIndex] = selectedWeaponIndex
        selectedWeapon = newList
    }

    /**
     * 切换武器事件
     *
     * @param seatIndex 武器槽位
     * @param value     数值（可能为-1~1之间的滚动，或绝对数值）
     * @param isScroll  是否是滚动事件
     */
    open fun changeWeapon(seatIndex: Int, value: Int, isScroll: Boolean) {
        if (seatIndex < 0 || seatIndex >= this.maxPassengers) return

        val weapons = this.computed().seats()[seatIndex].weapons()
        if (weapons.isEmpty()) return
        val count = weapons.size

        val currentIndex = this.getWeaponIndex(seatIndex)
        val typeIndex = Mth.clamp(if (isScroll) (value + currentIndex + count) % count else value, 0, count - 1)
        if (typeIndex == currentIndex) return

        val weapon = this.getGunData(weapons[typeIndex]) ?: return

        // 修改该槽位选择的武器
        this.setWeaponIndex(seatIndex, typeIndex)

        // 播放武器切换音效
        val sound = weapon.get(GunProp.SOUND_INFO).change
        if (sound != null) {
            this.level().playSound(null, this, sound, this.soundSource, 1f, 1f)
        }
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        VehicleData.from(this).update()
        override = compound.getString("Override")

        // GunData
        val state = compound.getCompound("WeaponState")
        val newMap = mutableMapOf<String, GunData>()
        for (key in state.allKeys) {
            var tag = state.getCompound(key)

            tag = tag.copy()
            tag.putString("id", "superbwarfare:vehicle_gun")
            tag.putInt("count", 1)

            ItemStack.parse(this.level().registryAccess(), tag)
                .ifPresent { stack -> newMap[key] = GunData.from(stack) }
        }
        gunDataMap = newMap

        health = if (compound.contains("Health")) {
            compound.getFloat("Health")
        } else {
            this.getMaxHealth()
        }

        turretHealth = compound.getFloat("TurretHealth")
        leftWheelHealth = compound.getFloat("LeftWheelHealth")
        rightWheelHealth = compound.getFloat("RightWheelHealth")
        mainEngineHealth = compound.getFloat("MainEngineHealth")
        subEngineHealth = compound.getFloat("SubEngineHealth")

        turretDamaged = compound.getBoolean("TurretDamaged")
        leftWheelDamaged = compound.getBoolean("LeftWheelDamaged")
        rightWheelDamaged = compound.getBoolean("RightWheelDamaged")
        mainEngineDamaged = compound.getBoolean("MainEngineDamaged")
        subEngineDamaged = compound.getBoolean("SubEngineDamaged")

        power = compound.getFloat("Power")
        decoyReady = compound.getBoolean("DecoyReady")
        synchedGearRot = compound.getFloat("GearRot")
        gearUp = compound.getBoolean("GearUp")
        synchedPropellerRot = compound.getFloat("PropellerRot")
        chargeProgress = compound.getFloat("ChargeProgress")
        lastAttackerUUID = compound.getString("LastAttacker")
        lastDriverUUID = compound.getString("LastDriver")

        val dogTagTag = compound.get("DogTagIcon")
        val list = mutableListOf<List<Short>>()
        if (dogTagTag is ListTag) {
            dogTagTag.forEach {
                val sl = mutableListOf<Short>()
                if (it is IntArrayTag) {
                    sl.addAll(it.asIntArray.map { num -> num.toShort() })
                }
                list.add(sl)
            }
        }
        dogTagIcon = list

        serverYaw = compound.getFloat("ServerYaw")
        serverPitch = compound.getFloat("ServerPitch")

        isWreck = compound.getBoolean("IsWreck")
        sympatheticDetonated = compound.getBoolean("SympatheticDetonated")
        turretBurned = compound.getBoolean("TurretBurned")
        turretBurnTimer = compound.getInt("TurretBurnTimer")

        val selectedWeaponTag = compound.get("SelectedWeapon")
        val selected = if (selectedWeaponTag is IntArrayTag) {
            selectedWeaponTag.asIntArray
        } else {
            IntArray(this.maxPassengers)
        }

        selectedWeapon = if (selected.size != this.maxPassengers) {
            // 数量不符时（可能是更新或遇到损坏数据），重新初始化已选择武器
            MutableList(maxPassengers) { 0 }
        } else {
            selected.toMutableList()
        }

        val energyNBT = compound.get("Energy")
        if (this.hasEnergyStorage() && energyNBT is IntTag) {
            (energyStorage as SyncedEntityEnergyStorage).deserializeNBT(level().registryAccess(), energyNBT)
        }

        this.resizeItems()
        if (compound.contains("Inventory")) {
            this.inventory.deserializeNBT(level().registryAccess(), compound.getCompound("Inventory"))
        } else {
            val items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY)
            ContainerHelper.loadAllItems(compound, items, level().registryAccess())
            this.inventory.setItems(items)
        }
    }

    public override fun addAdditionalSaveData(compound: CompoundTag) {
        checkSeatsSize()

        compound.putFloat("Health", health)

        val overrideString = override
        if (!overrideString.isBlank()) {
            compound.putString("Override", overrideString)
        }

        compound.putString("LastAttacker", lastAttackerUUID)
        compound.putString("LastDriver", lastDriverUUID)

        val listTag = ListTag()
        dogTagIcon.forEach {
            listTag.add(IntArrayTag(it.toShortArray().map { num -> num.toInt() }))
        }
        compound.put("DogTagIcon", listTag)

        val tag = CompoundTag()
        for (kv in gunDataMap.entries) {
            val data = GunData.from(kv.value.stack.copy())
            data.backupAmmoCount.reset()
            data.save()

            val stackTag = data.stack.save(this.level().registryAccess())
            if (stackTag is CompoundTag) {
                stackTag.remove("id")
                stackTag.remove("count")
                if (stackTag.isEmpty) continue
            }

            tag.put(kv.key, stackTag)
        }

        if (!tag.isEmpty) {
            compound.put("WeaponState", tag)
        }

        compound.putFloat("TurretHealth", turretHealth)
        compound.putFloat("LeftWheelHealth", leftWheelHealth)
        compound.putFloat("RightWheelHealth", rightWheelHealth)
        compound.putFloat("MainEngineHealth", mainEngineHealth)
        compound.putFloat("SubEngineHealth", subEngineHealth)

        compound.putBoolean("TurretDamaged", turretDamaged)
        compound.putBoolean("LeftWheelDamaged", leftWheelDamaged)
        compound.putBoolean("RightWheelDamaged", rightWheelDamaged)
        compound.putBoolean("MainEngineDamaged", mainEngineDamaged)
        compound.putBoolean("SubEngineDamaged", subEngineDamaged)

        compound.putFloat("Power", power)
        compound.putBoolean("DecoyReady", decoyReady)
        compound.putFloat("GearRot", synchedGearRot)
        compound.putBoolean("GearUp", gearUp)
        compound.putFloat("PropellerRot", synchedPropellerRot)
        compound.putFloat("ChargeProgress", chargeProgress)

        compound.putFloat("ServerYaw", serverYaw)
        compound.putFloat("ServerPitch", serverPitch)

        if (this.maxPassengers > 0) {
            compound.putIntArray("SelectedWeapon", selectedWeapon)
        }

        if (this.hasEnergyStorage()) {
            compound.put("Energy", (energyStorage as SyncedEntityEnergyStorage).serializeNBT(level().registryAccess()))
        }

        compound.putBoolean("IsWreck", isWreck)
        compound.putBoolean("SympatheticDetonated", sympatheticDetonated)
        compound.putBoolean("TurretBurned", turretBurned)
        compound.putInt("TurretBurnTimer", turretBurnTimer)

        this.resizeItems()
        compound.put("Inventory", this.inventory.serializeNBT(level().registryAccess()))
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        if (player.vehicle === this) return InteractionResult.PASS

        val stack = player.mainHandItem
        if (player.isShiftKeyDown && stack.`is`(ModItems.DOG_TAG.get())) {
            this.dogTagIcon = DogTagItem.getColors(stack).map { it.toList() }.toList()
            return InteractionResult.SUCCESS
        }

        if (stack.item is NameTagItem && stack.hasCustomHoverName()) {
            this.customName = stack.hoverName
            stack.shrink(1)
            return InteractionResult.sidedSuccess(this.level().isClientSide())
        }

        if (this.hasMenu() && player.isShiftKeyDown && !stack.`is`(ModTags.Items.TOOLS_CROWBAR)) {
            this.openMenu(player)
            return InteractionResult.sidedSuccess(player.level().isClientSide)
        }

        if (stack.`is`(ModItems.VEHICLE_DAMAGE_ANALYZER.get())) {
            if (!level().isClientSide) {
                if (this.damageDebugResultReceiver != null) {
                    this.damageDebugResultReceiver = null
                    player.displayClientMessage(
                        Component.translatable(
                            "des.superbwarfare.vehicle_damage_analyzer.unbind",
                            this.displayName
                        ), true
                    )
                } else {
                    this.damageDebugResultReceiver = player
                    player.displayClientMessage(
                        Component.translatable(
                            "des.superbwarfare.vehicle_damage_analyzer.bind",
                            this.displayName
                        ), true
                    )
                }
            }
            return InteractionResult.SUCCESS
        }

        if (player.isShiftKeyDown && stack.`is`(ModTags.Items.TOOLS_CROWBAR) && this.getPassengers().isEmpty()) {
            if (isWreck) {
                return InteractionResult.PASS
            } else {
                for (item in this.getRetrieveItems()) {
                    ItemHandlerHelper.giveItemToPlayer(player, item)
                }
                this.remove(RemovalReason.DISCARDED)
                this.discard()
                return InteractionResult.SUCCESS
            }
        } else if (!player.isShiftKeyDown && this.maxPassengers > 0) {
            if (VehicleConfig.SAME_TEAM_ENTER_VEHICLE.get()) {
                for (passenger in this.getPassengers()) {
                    if (passenger.team != null && (TDMSavedData.enabledTDM(passenger) || passenger.team !== player.team)) {
                        return InteractionResult.PASS
                    }
                }

                if (this.lastDriver != null
                    && !SeekTool.IN_SAME_TEAM.test(player, this.lastDriver)
                    && this.lastDriver?.team != null
                ) {
                    return InteractionResult.PASS
                }
            }

            if (isWreck) {
                return InteractionResult.PASS
            }

            if (this.getFirstPassenger() == null) {
                if (player is FakePlayer) return InteractionResult.PASS
                VehicleVecUtils.setDriverAngle(this, player)
                player.isSprinting = false
                if (player.level() is ServerLevel) {
                    return if (player.startRiding(this)) InteractionResult.CONSUME else InteractionResult.PASS
                }
            } else if (this.getFirstPassenger() !is Player) {
                if (player is FakePlayer) return InteractionResult.PASS
                this.getFirstPassenger()!!.stopRiding()
                VehicleVecUtils.setDriverAngle(this, player)
                player.isSprinting = false
                if (player.level() is ServerLevel) {
                    return if (player.startRiding(this)) InteractionResult.CONSUME else InteractionResult.PASS
                }
            }
            if (this.canAddPassenger(player)) {
                if (player is FakePlayer) return InteractionResult.PASS
                player.isSprinting = false
                if (player.level() is ServerLevel) {
                    return if (player.startRiding(this)) InteractionResult.CONSUME else InteractionResult.PASS
                }
            }
        }
        return InteractionResult.PASS
    }

    open val lastDriver: Entity?
        get() = EntityFindUtil.findEntity(level(), lastDriverUUID)

    @Deprecated("")
    open fun setDriverAngle(player: Player) {
        VehicleVecUtils.setDriverAngle(this, player)
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (source.`is`(ModTags.DamageTypes.VEHICLE_IMMUNE)) return false

        if (DamageTypeTool.isGunDamage(source) && source.entity != null && source.entity!!
                .vehicle === this
        ) {
            return false
        }

        val lastDriver = this.lastDriver
        if (source.entity != null && lastDriver != null && SeekTool.IS_FRIENDLY.test(
                lastDriver,
                source.entity
            )
            && lastDriver.team != null && source.entity!!.team != null && source.entity!!
                .team === lastDriver.team && !source.entity!!.team!!
                .isAllowFriendlyFire && (source.entity === lastDriver && !source.`is`(ModDamageTypes.VEHICLE_STRIKE))
        ) {
            return false
        }

        if (this.damageDebugResultReceiver != null) {
            this.damageDebugResultReceiver!!.sendSystemMessage(DamageHandler.getDamageInfo(this, source, amount))
        }

        // 计算减伤后的伤害
        var computedAmount = amount
        if (!source.`is`(ModTags.DamageTypes.BYPASSES_VEHICLE)) {
            computedAmount = this.getDamageModifier().compute(source, amount)
        }

        this.crash = source.`is`(ModDamageTypes.VEHICLE_STRIKE)

        if (source.entity != null) {
            lastAttackerUUID = source.entity!!.getStringUUID()
        }

        val projectile = source.directEntity
        if (projectile is Projectile) {
            val accessor = OBBHitter.getInstance(projectile)
            val part = accessor.`sbw$getCurrentHitPart`()

            if (part != null) {
                when (part) {
                    TURRET -> turretHealth -= computedAmount
                    WHEEL_LEFT -> leftWheelHealth -= computedAmount
                    WHEEL_RIGHT -> rightWheelHealth -= computedAmount
                    MAIN_ENGINE -> mainEngineHealth -= computedAmount
                    SUB_ENGINE -> subEngineHealth -= computedAmount

                    else -> {}
                }
            }
        }

        this.lastDamageSource = source
        this.lastDamageStamp = level().gameTime

        this.onHurt(computedAmount, source.entity, true)
        return super.hurt(source, computedAmount)
    }

    /**
     * 控制载具伤害免疫
     *
     * @return DamageModifier
     */
    open fun getDamageModifier(): DamageModifier = data().damageModifier()

    open fun getSourceAngle(source: DamageSource, multiplier: Float): Float {
        return VehicleVecUtils.getDamageSourceAngle(this, source, multiplier)
    }

    open fun heal(pHealAmount: Float) {
        if (this.level() is ServerLevel) {
            if (health > 0) {
                this.health += pHealAmount
            }
        }
    }

    open fun onHurt(pHealAmount: Float, attacker: Entity?, send: Boolean) {
        if (this.level() is ServerLevel) {
            val holder = Holder.direct(ModSounds.INDICATION_VEHICLE.get())
            for (player in server!!.playerList.players) {
                if (player == attacker && pHealAmount > 0 && this.health > 0 && send && (this !is DroneEntity)) {
                    player.connection.send(
                        ClientboundSoundPacket(
                            holder,
                            SoundSource.PLAYERS,
                            player.x,
                            player.eyeY,
                            player.z,
                            0.25f + (2.75f * pHealAmount / this.getMaxHealth()),
                            random.nextFloat() * 0.1f + 0.9f,
                            player.level().random.nextLong()
                        )
                    )
                    player.sendPacket(ClientIndicatorMessage(3, 5))
                }
            }

            if (pHealAmount > 0 && send) {
                repairCoolDown = maxRepairCoolDown()
                val passengers = this.getPassengers()
                for (entity in passengers) {
                    if (entity is ServerPlayer) {
                        entity.connection.send(
                            ClientboundSoundPacket(
                                holder,
                                SoundSource.PLAYERS,
                                entity.x,
                                entity.eyeY,
                                entity.z,
                                0.25f + (4.75f * pHealAmount / this.getMaxHealth()),
                                random.nextFloat() * 0.1f + 0.6f,
                                entity.level().random.nextLong()
                            )
                        )
                    }
                }
            }

            this.health -= Math.min(pHealAmount, getMaxHealth() + 1)
        }
    }

    open var health: Float
        get() = this.entityData.get(HEALTH)
        set(value) {
            this.entityData.set(HEALTH, value.coerceIn(-this.getMaxHealth() - 10, this.getMaxHealth()))
        }

    open fun getMaxHealth() = computed().maxHealth

    open fun getTurretMaxHealth() = 50f
    open fun getWheelMaxHealth() = 50f
    open fun getEngineMaxHealth() = 50f

    override fun lavaHurt() {
        if (tickCount % 10 == 0) {
            this.hurt(this.damageSources().lava(), 4.0f)
        }
    }

    override fun makeStuckInBlock(pState: BlockState, pMotionMultiplier: Vec3) {
        //留空
    }

    @ParametersAreNonnullByDefault
    override fun playStepSound(pPos: BlockPos, pState: BlockState) {
        this.playSound(
            ModSounds.WHEEL_VEHICLE_STEP.get(),
            (deltaMovement.length() * 0.1).toFloat(),
            random.nextFloat() * 0.15f + 1.05f
        )
    }

    override fun canBeCollidedWith(): Boolean {
        return this.enableAABB()
    }

    override fun isPickable(): Boolean {
        return !this.isRemoved
    }

    override fun skipAttackInteraction(attacker: Entity): Boolean {
        return hasPassenger(attacker) || super.skipAttackInteraction(attacker)
    }

    override fun canAddPassenger(pPassenger: Entity): Boolean {
        return this.getPassengers().size < this.maxPassengers
    }

    open val maxPassengers: Int
        get() = computed().seats().size

    /**
     * 呼吸回血冷却时长(单位:tick)，设为小于0的值以禁用呼吸回血
     */
    open fun maxRepairCoolDown(): Int {
        return computed().repairCooldown
    }

    /**
     * 呼吸回血回血量
     */
    open fun repairAmount(): Float {
        return computed().repairAmount
    }

    override fun baseTick() {
        if (prevMotion == null) {
            prevMotion = this.deltaMovement
        }

        this.prevPitchAngle = this.pitchAngle
        this.prevRollAngle = this.rollAngle


        val computed = computed()
        if (this.level().isClientSide) {
            if (!this.wasEngineRunning && this.engineRunning()) {
                playEngineSound.accept(this)
                playSwimSound.accept(this)
                if (computed.engineType == EngineType.TRACK) {
                    playTrackSound.accept(this)
                }
            }

            if (!this.wasHornWorking && this.hornWorking()) {
                playHornSound.accept(this)
            }

            if (!this.wasStuka && this.stuka() && engineInfo is Aircraft && (engineInfo as Aircraft).hasStukaSound) {
                playStukaSound.accept(this)
            }

            if (!this.wasHeliCrash && this.heliCrash()) {
                playHeliCrashSound.accept(this)
            }

            if (!this.wasVehicleSkip && this.vehicleSkip()) {
                playVehicleSkipSound.accept(this)
            }

            //            if (!this.wasInCarMusicPlaying && this.inCarMusicPlaying()) {
//                playInCarMusic.accept(this);
//            }
            if (playFireSound != null && !this.wasFiring && this.isFiring) {
                playFireSound!!.accept(this)
            }

            this.wasFiring = this.isFiring

        } else {
            // 枪数据处理
            val newMap = mutableMapOf<String, GunData>()

            for (kv in gunDataMap.entries) {
                val newData = kv.value.copy()
                newData.tick(this, true)
                newMap[kv.key] = newData
            }
            gunDataMap = newMap
        }

        this.wasEngineRunning = this.engineRunning()
        this.wasHornWorking = this.hornWorking()
        this.wasStuka = this.stuka()
        this.wasHeliCrash = this.heliCrash()
        this.wasVehicleSkip = this.vehicleSkip()

        this.prevRoll = this.roll

        //        this.wasInCarMusicPlaying = this.inCarMusicPlaying();
        turretYRotO = this.turretYRot
        turretXRotO = this.turretXRot

        gunYRotO = this.gunYRot
        gunXRotO = this.gunXRot

        leftWheelRotO = this.leftWheelRot
        rightWheelRotO = this.rightWheelRot

        leftTrackO = this.leftTrack
        rightTrackO = this.rightTrack

        rudderRotO = this.rudderRot
        propellerRotO = this.propellerRot
        recoilShakeO = this.recoilShake

        if (jumpCoolDown > 0 && onGround()) {
            jumpCoolDown--
        }

        lastTickSpeed =
            Vec3(this.deltaMovement.x, this.deltaMovement.y + 0.06, this.deltaMovement.z).length()
        lastTickVerticalSpeed = this.deltaMovement.y + 0.06
        if (collisionCoolDown > 0) {
            collisionCoolDown--
        }

        laserScaleO = laserScale

        flap1LRotO = this.flap1LRot
        flap1RRotO = this.flap1RRot
        flap1L2RotO = this.flap1L2Rot
        flap1R2RotO = this.flap1R2Rot
        flap2LRotO = this.flap2LRot
        flap2RRotO = this.flap2RRot
        flap3RotO = this.flap3Rot
        gearRotO = this.gearRot
        deltaMovementO = deltaMovement
        positionO = position()
        absoluteSpeedO = absoluteSpeed

        super.baseTick()

        if (laserScale > 0) {
            laserScale = Math.max(laserScale - 0.1f, 0f)
            laserScale *= 0.9f
        }

        if (laserScale == 0f) {
            laserLength = 0f
        }

        if (repairCoolDown > 0) {
            repairCoolDown--
        }

        if (this.health >= this.getMaxHealth()) {
            repairCoolDown = maxRepairCoolDown()
        }

        val delta = Math.abs(yRot - yRotO)
        while (yRot > 180f) {
            yRot -= 360f
            yRotO = yRot - delta
        }
        while (yRot <= -180f) {
            yRot += 360f
            yRotO = delta + yRot
        }

        val deltaX = Math.abs(xRot - xRotO)
        while (xRot > 180f) {
            xRot -= 360f
            xRotO = xRot - deltaX
        }
        while (xRot <= -180f) {
            xRot += 360f
            xRotO = deltaX + xRot
        }

        val deltaZ = Math.abs(this.roll - prevRoll)
        while (this.roll > 180f) {
            setZRot(this.roll - 360f)
            prevRoll = this.roll - deltaZ
        }
        while (this.roll <= -180f) {
            setZRot(this.roll + 360f)
            prevRoll = deltaZ + this.roll
        }

        this.handleClientSync()

        if (this.level() is ServerLevel && this.health <= 0 && !isWreck) {
            isWreck = true
            destroy()
        }

        if (isWreck) {
            if ((vehicleType == VehicleType.AIRPLANE || vehicleType == VehicleType.HELICOPTER) && (onGround() || isInFluidType) && !sympatheticDetonated) {
                sympatheticDetonated = true
                val destroyInfo = computed().destroyInfo
                if (destroyInfo.explodePassengers) {
                    if (this.crash && destroyInfo.crashPassengers) {
                        crashPassengers()
                    } else {
                        explodePassengers()
                    }
                }
                vehicleExplosion(destroyInfo)
                ejectPassengers()
            }

            if (health <= -getMaxHealth()) {
                this.discard()
                createCustomExplosion()
                    .radius(0f)
                    .damage(0f)
                    .withParticleType(ParticleTool.ParticleType.SMALL)
                    .keepBlock()
                    .explode()

                this.generateWreckageLoot()
            }

            if (vehicleType != VehicleType.AIRPLANE && vehicleType != VehicleType.HELICOPTER) {
                ejectPassengers()
            }
        }

        this.travel()

        if (this.health <= computed.selfHurtPercent * this.getMaxHealth()) {
            // 血量过低时自动扣血
            this.onHurt(computed.selfHurtAmount, this.lastAttacker, false)
        } else {
            // 呼吸回血
            if (repairCoolDown == 0 && health > 0) {
                this.heal(repairAmount())
            }
        }

        if (this.maxPassengers > 0 && getFirstPassenger() != null) {
            lastDriverUUID = getFirstPassenger()!!.getStringUUID()
        }

        if (getPassengers().isEmpty()) {
            noPassengerTime++
            if (noPassengerTime > 200) {
                lastDriverUUID = "undefined"
            }
        } else {
            noPassengerTime = 0
        }

        mouseMoveSpeedX *= 0.95f
        mouseMoveSpeedY *= 0.95f

        if (hasTurret()) {
            val turretController = getNthEntity(this.turretControllerIndex)
            if (turretController is Player) {
                this.adjustTurretAngle()
            } else if (turretController is Mob) {
                this.turretAutoAimFromUuid(aiTurretTargetUUID, turretController)
            }

            if (turretController == null) {
                turretYRotLock = 0f
            }
        }

        if (hasPassengerWeaponStation()) {
            val passengerWeaponStationController = getNthEntity(this.passengerWeaponStationControllerIndex)
            if (passengerWeaponStationController is Player || passengerWeaponStationController == null) {
                this.adjustWeaponControllerAngle()
            } else if (passengerWeaponStationController is Mob) {
                this.passengerWeaponAutoAimFormUuid(aiPassengerWeaponTargetUUID, passengerWeaponStationController)
            }
        }

        for (i in data().getDefault().seats().indices) {
            val mob = getNthEntity(i)
            if (mob is Mob && canShoot(mob) && mob.target != null && getGunData(mob) != null && mob.level() is ServerLevel) {
                val target = mob.target!!
                mob.lookAt(target, 30f, 30f)
                val rpm = Math.ceil(20f / (vehicleWeaponRpm(mob).toFloat() / 60)).toInt()
                if (tickCount % rpm == 0 && canShoot(mob) &&
                    getShootDirectionForHud(mob, 1f).angleTo(
                        getShootPos(mob, 1f).vectorTo(VectorTool.lerpGetEntityBoundingBoxCenter(target, 1f))
                    ) < 4
                ) {
                    vehicleShoot(mob, target.getUUID(), null)
                }
            }
            if (mob is Player && level() is ServerLevel) {
                if (tickCount % 20 == 0) {
                    val gunData: GunData? = getGunData(mob)
                    if (gunData != null) {
                        if (gunData.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.ENERGY) {
                            if (!canConsume(gunData.get(GunProp.AMMO_COST_PER_SHOOT))) {
                                mob.displayClientMessage(
                                    Component.translatable("tips.superbwarfare.not.enough.energy"),
                                    true
                                )
                            }
                        } else if (getAmmoCount(mob) < gunData.get(GunProp.AMMO_COST_PER_SHOOT)) {
                            val stack = gunData.selectedAmmoConsumer().stack()
                            if (stack != ItemStack.EMPTY && !InventoryTool.hasCreativeAmmoBox(this) && !gunData.reloading()) {
                                mob.displayClientMessage(
                                    Component.translatable("tips.superbwarfare.need.ammo")
                                        .append(
                                            Component.literal("[").append(stack.hoverName).append("]")
                                                .withStyle(ChatFormatting.YELLOW)
                                        ), true
                                )
                            }
                        }
                    }
                }

                val index: Int = getSeatIndex(mob)
                val seat: SeatInfo = computed().seats()[index]
                if (mob.getData(ModAttachments.PLAYER_VARIABLE).activeThermalImaging && seat.hasThermalImaging) {
                    mob.addEffect(MobEffectInstance(MobEffects.NIGHT_VISION, 5, 0, false, false))
                }

                vehicleRadar(mob)
            }
        }

        val deltaT = abs(this.turretYRot - turretYRotO)
        while (this.turretYRot > 180f) {
            this.turretYRot -= 360f
            turretYRotO = this.turretYRot - deltaT
        }
        while (this.turretYRot <= -180f) {
            this.turretYRot += 360f
            turretYRotO = deltaT + this.turretYRot
        }

        val deltaG = abs(this.gunYRot - gunYRotO)
        while (this.gunYRot > 180f) {
            this.gunYRot -= 360f
            gunYRotO = this.gunYRot - deltaT
        }
        while (this.gunYRot <= -180f) {
            this.gunYRot += 360f
            gunYRotO = deltaG + this.gunYRot
        }

        if (decoyReloadCoolDown > 0) {
            decoyReloadCoolDown--
        }

        if (this.cannonRecoilTime > 0) {
            cannonRecoilTime -= 1
        }

        this.recoilShake = Mth.abs(cannonRecoilForce) * 0.0000007 * cannonRecoilTime.toDouble()
            .pow(4.0) * sin(0.2 * Math.PI * (cannonRecoilTime - 2.5))
        cannonRecoilForce *= 0.93f

        this.supportEntities()
        this.crushEntities()
        this.setDeltaMovement(this.deltaMovement.add(0.0, -this.computed().gravity, 0.0))
        this.move(MoverType.SELF, this.deltaMovement)


        if (tickCount % 4 == 0) {
            this.clearArrow()
            this.preventStacking()
            this.moveOnDragonTeeth()
            this.collideBlocks()
        }

        if (this.hasEnergyStorage() && this.tickCount % 20 == 0) {
            for (stack in this.inventory.getItems()) {
                val neededEnergy: Int = this.maxEnergy - this.energy
                if (neededEnergy <= 0) break

                val energyCap = stack.getCapability(Capabilities.EnergyStorage.ITEM) ?: continue

                val stored = energyCap.energyStored
                if (stored <= 0) continue

                val energyToExtract = Math.min(stored, neededEnergy)
                energyCap.extractEnergy(energyToExtract, false)
                this.energy += energyToExtract
            }
        }

        if (this.level() is ServerLevel) {
            updateBackupAmmoCount()
        }

        hornVolume *= 0.5f

        if (hasDecoy()) {
            if (this.vehicleType == VehicleType.AIRPLANE || this.vehicleType == VehicleType.HELICOPTER) {
                releaseDecoy()
            } else {
                releaseSmokeDecoy(getTurretVector(1f))
            }
        }

        val terrainCompat = this.computed().terrainCompat
        if (terrainCompat.isNotEmpty()) {
            if (!((vehicleType == VehicleType.AIRPLANE || vehicleType == VehicleType.HELICOPTER) && isWreck)) {
                this.terrainCompact(terrainCompat)
            }
        }

        if (this.leftTrack < 0) {
            this.leftTrackO = this.getTrackAnimationLength().toFloat()
            this.leftTrack = this.getTrackAnimationLength().toFloat()
        }

        if (this.leftTrack > this.getTrackAnimationLength()) {
            this.leftTrackO = 0f
            this.leftTrack = 0f
        }

        if (this.rightTrack < 0) {
            this.rightTrackO = this.getTrackAnimationLength().toFloat()
            this.rightTrack = this.getTrackAnimationLength().toFloat()
        }

        if (this.rightTrack > this.getTrackAnimationLength()) {
            this.rightTrackO = 0f
            this.rightTrack = 0f
        }

        if (turretBurnTimer > 0) {
            turretBurnTimer--
        }

        if (level().isClientSide) {
            absoluteSpeedLerp = Mth.lerp(0.2, absoluteSpeedLerp, positionO.vectorTo(position()).length())
            absoluteSpeed = absoluteSpeedLerp
        }

        if (prevMotion != null) {
            val motion = this.deltaMovement
            var acceleration = prevMotion?.let { motion.subtract(it) }

            if (acceleration != null && acceleration.length() > 0.02) {
                acceleration = acceleration.normalize().scale(0.02)
            }

            val yaw = this.yRot
            val sinYaw = Mth.sin(yaw * Mth.DEG_TO_RAD)
            val cosYaw = Mth.cos(yaw * Mth.DEG_TO_RAD)

            val forward = Vec3(-sinYaw.toDouble(), 0.0, cosYaw.toDouble())
            val right = Vec3(-cosYaw.toDouble(), 0.0, -sinYaw.toDouble())

            val accelForward: Double = acceleration!!.multiply(1.0, 0.0, 1.0).dot(forward)
            val accelRight: Double = acceleration.multiply(1.0, 0.0, 1.0).dot(right)

            val targetPitch = (10 * accelForward).toFloat()
            val omegaP = 2.0f * Math.PI.toFloat() * 2f
            val zetaP = 0.6f
            val angularAccelP: Float = omegaP * omegaP * (targetPitch - pitchAngle) - 2 * zetaP * omegaP * pitchVelocity
            pitchVelocity += angularAccelP * 0.05f // dt = 0.05s
            pitchAngle += pitchVelocity * 0.05f

            val targetRoll = (15 * accelRight).toFloat()
            val omegaR = 2.0f * Math.PI.toFloat() * 2f
            val zetaR = 0.6f
            val angularAccelR: Float = omegaR * omegaR * (targetRoll - rollAngle) - 2 * zetaR * omegaR * rollVelocity
            rollVelocity += angularAccelR * 0.05f
            rollAngle += rollVelocity * 0.05f

            prevMotion = motion

            xRot -= pitchAngle * computed().inertiaRotateRate
            roll -= rollAngle * computed().inertiaRotateRate
        }

        lowHealthWarning()
        if (!this.enableAABB()) {
            this.handlePartDamaged(this)
            // 处理部件血量
            this.handlePartHealth()
            this.updateOBB()
        }

        if (level() is ServerLevel && VehicleConfig.VEHICLE_CHUNK_LOADING.get() && computed().keepChunkLoaded) {
            this.keepChunkLoaded(this.position())
            this.keepChunkLoaded(position().add(deltaMovement.normalize().scale(16.0)))
        }
    }

    fun keepChunkLoaded(position: Vec3) {
        val chunkPos = ChunkPos(BlockPos.containing(position))
        (level() as ServerLevel).chunkSource.addRegionTicket(TicketType.POST_TELEPORT, chunkPos, 3, this.id)
    }

    // TODO 添加更多的雷达机制
    fun vehicleRadar(player: Player) {
        if (!MiscConfig.SYNC_ENTITY_OVER_RANGE.get()) return
        if ((level().server?.tickCount ?: return) % MiscConfig.SYNC_ENTITY_INTERVAL.get() != 0) return
        val data = this.getGunData(player) ?: return
        val seekWeaponInfo = data.get(GunProp.SEEK_WEAPON_INFO) ?: return

        val level = this.level()
        if (level is ServerLevel) {
            // 搜索范围
            val seekRange = seekWeaponInfo.seekRange
            // 最小目标高度
            val minTargetHeight = seekWeaponInfo.minTargetHeight
            // 最大目标高度
            val maxTargetHeight = seekWeaponInfo.maxTargetHeight

            val hostileList = level.allEntities
                .asSequence()
                .mapNotNull {
                    val flag = (it is VehicleEntity || VehicleConfig.inScanList(it.type))
                            && SeekTool.NOT_IN_SMOKE.test(it)
                            && it.distanceToSqr(this) <= seekRange * seekRange
                            && SeekTool.IN_HEIGHT_RANGE.test(it, minTargetHeight, maxTargetHeight)
                            && !SeekTool.IS_FRIENDLY.test(player, it)
                            && VectorTool.checkNoClip(eyePosition, it.eyePosition, level())
                    if (!flag) return@mapNotNull null
                    EntitySyncMessage.SyncedEntity(
                        it.id,
                        BuiltInRegistries.ENTITY_TYPE.getKey(it.type),
                        it.position(),
                        it.deltaMovement,
                        CompoundTag().also { tag -> it.saveWithoutId(tag) }
                    )
                }.toList()
            sendPacketTo(player, EntitySyncMessage(level.dimension().location(), hostileList, false))
        }
    }

    override fun canFreeze() = false

    open fun updateOBB() {
        this.obb.forEach { obbInfo ->
            val transform = this.getTransformFromString(obbInfo.transform)
            val obb = obbInfo.getOBB()
            val worldPos = this.transformPosition(transform, obbInfo.position.x, obbInfo.position.y, obbInfo.position.z)

            if (hasTurret() && sympatheticDetonated && (obbInfo.transform.equals("Turret") || obbInfo.transform.equals("Barrel"))) {
                obb.setExtents(Vector3d(0.0, 0.0, 0.0))
            }

            obb.center.set(Vec3(worldPos.x, worldPos.y, worldPos.z).toVector3d())
            obb.updateRotation(this.getRotationFromString(obbInfo.rotation))
        }
    }

    open val shootSoundInstance: SoundEvent?
        get() {
            // TODO why 0?
            val gunData = getGunData(0)
            if (gunData != null) {
                val instance = gunData.get(GunProp.SOUND_INFO).fireSoundInstances
                if (instance != null) return instance
            } else {
                return getShootSoundInstance("Main")
            }
            return SoundEvents.EMPTY
        }

    open fun getShootSoundInstance(weaponName: String): SoundEvent {
        val gunData = getGunData(weaponName) ?: return SoundEvents.EMPTY

        return gunData.get(GunProp.SOUND_INFO).fireSoundInstances ?: SoundEvents.EMPTY
    }

    open val isFiring: Boolean
        get() {
            val gunData = getGunData(0)
            return if (gunData != null) {
                val instance = gunData.get(GunProp.SOUND_INFO).fireSoundInstances
                if (instance != null) {
                    gunData.shootTimer.get() > 0
                } else {
                    false
                }
            } else {
                false
            }
        }

    open fun shootingVolume(): Float {
        val gunData = getGunData(0)
        return if (gunData != null) {
            gunData.shootTimer.get() * 0.25f
        } else {
            0f
        }
    }

    open fun shootingPitch(): Float {
        val gunData = getGunData(0)
        return if (gunData != null) {
            (0.98f + gunData.shootTimer.get() * 0.01f - (if (gunData.heat.get() > 80) (gunData.heat.get() - 80) * 0.01 else 0.0)).toFloat()
        } else {
            1f
        }
    }

    protected fun updateBackupAmmoCount() {
        for (i in 0..<this.maxPassengers) {
            val currentData = getGunData(i) ?: continue

            if (!currentData.useBackpackAmmo()) {
                if (currentData.backupAmmoCount.get() != 0) {
                    modifyGunData(i) { it.backupAmmoCount.reset() }
                }
                continue
            }

            val count = currentData.countBackupAmmo(this.ammoSupplier)
            if (currentData.backupAmmoCount.get() != count) {
                modifyGunData(i) { it.backupAmmoCount.set(count) }
            }
        }
    }

    open val ammoSupplier: Entity
        /**
         * 获取开火用AmmoSupplier实体
         */
        get() = this

    open fun handlePartDamaged(obbEntity: OBBEntity) {
        val obbList = obbEntity.getOBBs()
        for (obb in obbList) {
            val pos = obb.center.toVec3()
            when (obb.part) {
                TURRET -> {
                    if (turretDamaged) {
                        this.onTurretDamaged(pos)
                    }
                }

                WHEEL_LEFT -> {
                    if (leftWheelDamaged) {
                        this.onLeftWheelDamaged(pos)
                    }
                }

                WHEEL_RIGHT -> {
                    if (rightWheelDamaged) {
                        this.onRightWheelDamaged(pos)
                    }
                }

                MAIN_ENGINE -> {
                    if (mainEngineDamaged) {
                        this.onEngine1Damaged(pos)
                    }
                }

                SUB_ENGINE -> {
                    if (subEngineDamaged) {
                        this.onEngine2Damaged(pos)
                    }
                }

                else -> {}
            }
        }
    }

    open fun handlePartHealth() {
        if (this.hasTurret() && (vehicleType == VehicleType.AA || vehicleType == VehicleType.APC || vehicleType == VehicleType.TANK) && health < 0.05 * getMaxHealth()) {
            turretHealth = 0f
            mainEngineHealth = 0f
            subEngineHealth = 0f
        }
        if ((vehicleType == VehicleType.HELICOPTER || vehicleType == VehicleType.AIRPLANE) && health < 0.05 * getMaxHealth()) {
            mainEngineHealth = 0f
            subEngineHealth = 0f
        }

        if (turretHealth <= 0) {
            turretDamaged = true
        } else if (turretHealth > 0.95 * this.getTurretMaxHealth()) {
            turretDamaged = false
        }

        if (leftWheelHealth <= 0) {
            leftWheelDamaged = true
        } else if (leftWheelHealth > 0.95 * this.getWheelMaxHealth()) {
            leftWheelDamaged = false
        }

        if (rightWheelHealth <= 0) {
            rightWheelDamaged = true
        } else if (rightWheelHealth > 0.95 * this.getWheelMaxHealth()) {
            rightWheelDamaged = false
        }

        if (mainEngineHealth <= 0) {
            mainEngineDamaged = true
        } else if (mainEngineHealth > 0.95 * this.getEngineMaxHealth()) {
            mainEngineDamaged = false
        }

        if (subEngineHealth <= 0) {
            subEngineDamaged = true
        } else if (subEngineHealth > 0.95 * this.getEngineMaxHealth()) {
            subEngineDamaged = false
        }

        if (!isWreck) {
            turretHealth = Math.min(
                turretHealth + 0.0025f * this.getTurretMaxHealth(),
                this.getTurretMaxHealth()
            )
            leftWheelHealth = Math.min(
                leftWheelHealth + 0.0025f * this.getWheelMaxHealth(),
                this.getWheelMaxHealth()
            )
            rightWheelHealth = Math.min(
                rightWheelHealth + 0.0025f * this.getWheelMaxHealth(),
                this.getWheelMaxHealth()
            )
            mainEngineHealth = Math.min(
                mainEngineHealth + 0.0025f * this.getEngineMaxHealth(),
                this.getEngineMaxHealth()
            )
            subEngineHealth = Math.min(
                subEngineHealth + 0.0025f * this.getEngineMaxHealth(),
                this.getEngineMaxHealth()
            )
        }
    }

    open fun addRandomParticle(
        particleOptions: ParticleOptions,
        pos: Vec3,
        randomPos: Float,
        level: Level,
        speed: Float,
        count: Int
    ) {
        val randomX = 2 * (this.random.nextFloat() - 0.5f)
        val randomY = 2 * (this.random.nextFloat() - 0.5f)
        val randomZ = 2 * (this.random.nextFloat() - 0.5f)
        repeat(count) {
            level.addAlwaysVisibleParticle(
                particleOptions,
                true,
                pos.x + randomPos * randomX,
                pos.y + randomPos * randomY,
                pos.z + randomPos * randomZ,
                (randomX * speed).toDouble(),
                (randomY * speed).toDouble(),
                (randomZ * speed).toDouble()
            )
        }
    }

    open fun addRandomParticle(
        particleOptions: ParticleOptions,
        pos: Vec3,
        randomPos: Float,
        level: Level,
        count: Int,
        vec3: Vec3
    ) {
        val randomX = 2 * (this.random.nextFloat() - 0.5f)
        val randomY = 2 * (this.random.nextFloat() - 0.5f)
        val randomZ = 2 * (this.random.nextFloat() - 0.5f)
        repeat(count) {
            level.addAlwaysVisibleParticle(
                particleOptions,
                true,
                pos.x + randomPos * randomX,
                pos.y + randomPos * randomY,
                pos.z + randomPos * randomZ,
                vec3.x,
                vec3.y,
                vec3.z
            )
        }
    }

    open fun defaultPartDamageEffect(pos: Vec3) {
        if (level().isClientSide) {
            addRandomParticle(ModParticleTypes.FIRE_STAR.get(), pos, 0f, level(), 0.25f, 1)
            addRandomParticle(ParticleTypes.LARGE_SMOKE, pos, 0.5f, level(), 0.001f, 1)
        }
    }

    open fun onTurretDamaged(pos: Vec3) {
        this.defaultPartDamageEffect(pos)
    }

    open fun onLeftWheelDamaged(pos: Vec3) {
        this.defaultPartDamageEffect(pos)
    }

    open fun onRightWheelDamaged(pos: Vec3) {
        this.defaultPartDamageEffect(pos)
    }

    open fun onEngine1Damaged(pos: Vec3) {
        this.defaultPartDamageEffect(pos)
    }

    open fun onEngine2Damaged(pos: Vec3) {
        this.defaultPartDamageEffect(pos)
    }

    open fun clearArrow() {
        if (tickCount % 5 != 0) return
        this.level().getEntities(
            this,
            this.boundingBox.inflate(0.0, 0.5, 0.0)
        ) { e -> e is AbstractArrow }
            .forEach { obj -> obj.discard() }
    }

    open fun lowHealthWarning() {
        if (!data().compute().hasLowHealthWarning) return
        if (this.health <= 0.4 * this.getMaxHealth()) {
            addRandomParticle(
                ParticleTypes.LARGE_SMOKE,
                Vec3(this.x, this.y + 0.7f * bbHeight, this.z),
                0.35f * this.bbWidth,
                level(),
                0.01f,
                1
            )
        }

        if (this.health <= 0.25 * this.getMaxHealth()) {
            playLowHealthParticle()
        }
        if (this.health <= 0.15 * this.getMaxHealth()) {
            playLowHealthParticle()
        }

        if (this.health <= 0.1 * this.getMaxHealth()) {
            val random = 2 * (this.random.nextFloat() - 0.5f)
            if (level().isClientSide) {
                addRandomParticle(
                    ParticleTypes.LARGE_SMOKE,
                    Vec3(this.x, this.y + 0.7f * bbHeight, this.z),
                    0.35f * this.bbWidth,
                    level(),
                    0.01f,
                    2
                )
                addRandomParticle(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    Vec3(this.x, this.y + 0.7f * bbHeight, this.z),
                    0.35f * this.bbWidth,
                    level(),
                    0.01f,
                    2
                )
                addRandomParticle(
                    CustomCloudOption(
                        1f,
                        0.1f,
                        0f,
                        (240 + 40 * random).toInt(),
                        2.5f + 0.5f * random,
                        -0.07f,
                        cooldown = true,
                        light = true
                    ),
                    Vec3(this.x, this.y + 0.85f * bbHeight, this.z),
                    0.35f * this.bbWidth,
                    level(),
                    0.01f,
                    1
                )
                addRandomParticle(
                    CustomCloudOption(
                        1f,
                        0.35f,
                        0f,
                        (80 + 40 * random).toInt(),
                        1.5f + 0.5f * random,
                        -0.07f,
                        cooldown = false,
                        light = true
                    ),
                    Vec3(this.x, this.y + 0.85f * bbHeight, this.z),
                    0.3f * this.bbWidth,
                    level(),
                    0.01f,
                    1
                )
            }

            if (computed().destroyInfo.sympatheticDetonation
                && health < 0.05 * getMaxHealth() && this.hasTurret()
                && (vehicleType == VehicleType.AA || vehicleType == VehicleType.APC || vehicleType == VehicleType.TANK)
                && !sympatheticDetonated
                && !turretBurned
            ) {
                turretBurned = true
                turretBurnTimer = 400
            }

            if (turretBurnTimer > 0 && !sympatheticDetonated && health < 0.05 * getMaxHealth()) {
                if (level().isClientSide) {
                    val pos = turretBurnEffectPos()
                    val dir = getUpVec(1f)
                    ParticleTool.spawnDirectionalParticles(
                        (12 + 10 * random).toInt(),
                        0.05 * random.toDouble(),
                        level(),
                        CannonMuzzleFlareOption(1f, 0.97f, 0.97f, 4, 0.5f, 1, 0.3f),
                        dir,
                        pos,
                        4.5 + random
                    )
                    ParticleTool.spawnDirectionalParticles(
                        (4 + 4 * random).toInt(),
                        0.8 * random.toDouble(),
                        level(),
                        ModParticleTypes.FIRE_STAR.get(),
                        dir,
                        pos,
                        0.4 + random
                    )
                    ParticleTool.spawnDirectionalParticles(
                        (4 + 4 * random).toInt(),
                        0.8 * random.toDouble(),
                        level(),
                        ParticleTypes.LAVA,
                        dir,
                        pos,
                        0.4 + random
                    )
                    ParticleTool.spawnDirectionalParticles(
                        (4 + 4 * random).toInt(),
                        0.8 * random.toDouble(),
                        level(),
                        ParticleTypes.FLAME,
                        dir,
                        pos,
                        0.4 + random
                    )
                }

                if (turretBurnTimer == 400) {
                    this.level().playSound(
                        null,
                        onPos,
                        ModSounds.TURRET_BURN_START.get(),
                        SoundSource.BLOCKS,
                        4f,
                        1f + 0.05f * random
                    )
                }
                if (turretBurnTimer % 5 == 0) {
                    this.level().playSound(
                        null,
                        onPos,
                        ModSounds.TURRET_BURN.get(),
                        SoundSource.BLOCKS,
                        1.5f,
                        1f + 0.05f * random
                    )
                }
            }

            if (health > 0.05 * getMaxHealth()) {
                turretBurned = false
                turretBurnTimer = 0
            }

            if (this.tickCount % 15 == 0) {
                this.level().playSound(null, this.onPos, SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 1f, 1f)
            }
        }

        if (health > 0 && health < 0.1f * this.getMaxHealth() && tickCount % 13 == 0) {
            this.level().playSound(null, this.onPos, ModSounds.NO_HEALTH.get(), SoundSource.PLAYERS, 1f, 1f)
        } else if (this.health >= 0.1f && this.health < 0.4f * this.getMaxHealth() && tickCount % 10 == 0) {
            this.level().playSound(null, this.onPos, ModSounds.LOW_HEALTH.get(), SoundSource.PLAYERS, 1f, 1f)
        }
    }

    open fun turretBurnEffectPos(): Vec3? {
        val pos = turretPos
        val worldPosition = pos?.let {
            transformPosition(
                getVehicleTransform(1f),
                pos.x, pos.y, it.z
            )
        }
        return worldPosition?.let { Vec3(it.x, worldPosition.y, worldPosition.z) }
    }

    open fun playLowHealthParticle() {
        if (level().isClientSide) {
            addRandomParticle(
                ParticleTypes.LARGE_SMOKE,
                Vec3(this.x, this.y + 0.7f * bbHeight, this.z),
                0.35f * this.bbWidth,
                level(),
                0.01f,
                1
            )
            addRandomParticle(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                Vec3(this.x, this.y + 0.7f * bbHeight, this.z),
                0.35f * this.bbWidth,
                level(),
                0.01f,
                1
            )
        }
    }

    open fun adjustTurretAngle() {
        VehicleWeaponUtils.adjustTurretAngle(this)
    }

    open fun getSelectedWeapon(seatIndex: Int) =
        selectedWeapon.getOrElse(seatIndex) { -1 }

    open fun turretAutoAimFromVector(shootVec: Vec3?) {
        VehicleWeaponUtils.turretAutoAimFromVector(this, shootVec)
    }

    open fun turretAutoAimFromUuid(uuid: String, pLiving: LivingEntity?) {
        VehicleWeaponUtils.turretAutoAimFromUuid(this, uuid, pLiving)
    }

    override fun onPassengerTurned(entity: Entity) {
        this.clampRotation(entity)
    }

    open val customTurretMinPitch: Float
        /**
         * @return 自定义炮塔最低俯角
         */
        get() = 0f

    open val customTurretMaxPitch: Float
        /**
         * @return 自定义炮塔最大仰角
         */
        get() = 0f

    private fun clampRotation(entity: Entity) {
        val index = getSeatIndex(entity)
        val seats = computed().seats()
        if (index < 0 || index >= seats.size) return
        val seat = seats[index]

        var vec3 = getTransformDirection(1f, entity)

        if ((seat.transform == "Barrel" && turretControllerIndex == getSeatIndex(entity)) ||
            (seat.transform == "WeaponStationBarrel" && passengerWeaponStationControllerIndex == getSeatIndex(entity))
        ) {
            vec3 = getTransformDirectionFromString(1f, entity, "Turret")
        }

        val minPitch = -seat.maxPitch + customTurretMaxPitch
        val maxPitch = -seat.minPitch - customTurretMinPitch
        val f = Mth.wrapDegrees(entity.xRot - -getXRotFromVector(vec3)).toFloat()
        val f1 = Mth.clamp(f, minPitch, maxPitch)
        entity.xRotO += f1 - f
        entity.xRot = entity.xRot + f1 - f

        val minYaw = seat.minYaw
        val maxYaw = seat.maxYaw
        val f2 = Mth.wrapDegrees(entity.yRot - -getYRotFromVector(vec3)).toFloat()
        val f3 = Mth.clamp(f2, minYaw, maxYaw)
        entity.yRotO += f3 - f2
        entity.yRot = entity.yRot + f3 - f2

        if (seat.transform == "Turret" && turretControllerIndex == getSeatIndex(entity)) {
            if (!entity.level().isClientSide) return
            if (Minecraft.getInstance().options.cameraType != CameraType.FIRST_PERSON) return

            val f4 = Mth.wrapDegrees(entity.yRot - -getYRotFromVector(vec3)).toFloat()
            val f5 = Mth.clamp(f2, -16f, 16f)
            entity.yRotO += f5 - f4
            entity.yRot = entity.yRot + f5 - f4
        }
    }

    open fun copyEntityData(entity: Entity) {
        entity.yRot += destroyRot
        val index = getSeatIndex(entity)
        val seat = computed().seats()[index]
        val vec3 = getTransformDirection(1f, entity)
        val yaw = -getYRotFromVector(vec3).toFloat()

        if (seat.transform == "Vehicle" || seat.transform == "VehicleFlat") {
            if (!seat.canRotateHead) {
                entity.yRot = yaw
            }
        }

        if (!seat.canRotateBody) {
            entity.setYBodyRot(yaw)
        }
    }

    open fun getTransformDirection(ticks: Float, entity: Entity): Vec3 {
        val index = getSeatIndex(entity)
        val seat = computed().seats()[index]
        val passengerRot = seat.orientation
        val transform = getTransformFromString(seat.transform, ticks).rotate(Axis.YP.rotationDegrees(-passengerRot))
        val posO = transformPosition(transform, 0.0, 0.0, 0.0)
        val pos = transformPosition(transform, 0.0, 0.0, 1.0)
        return Vec3(posO.x, posO.y, posO.z).vectorTo(Vec3(pos.x, pos.y, pos.z))
    }

    open fun getTransformDirectionNoOrientation(ticks: Float, entity: Entity): Vec3 {
        val index = getSeatIndex(entity)
        val seat = computed().seats()[index]
        val transform = getTransformFromString(seat.transform, ticks)
        val posO = transformPosition(transform, 0.0, 0.0, 0.0)
        val pos = transformPosition(transform, 0.0, 0.0, 1.0)
        return Vec3(posO.x, posO.y, posO.z).vectorTo(Vec3(pos.x, pos.y, pos.z))
    }

    open fun getTransformDirectionFromString(ticks: Float, entity: Entity, string: String): Vec3 {
        val index = getSeatIndex(entity)
        val seat = computed().seats()[index]
        val passengerRot = seat.orientation
        val transform = getTransformFromString(string, ticks).rotate(Axis.YP.rotationDegrees(-passengerRot))
        val posO = transformPosition(transform, 0.0, 0.0, 0.0)
        val pos = transformPosition(transform, 0.0, 0.0, 1.0)
        return Vec3(posO.x, posO.y, posO.z).vectorTo(Vec3(pos.x, pos.y, pos.z))
    }

    public override fun positionRider(passenger: Entity, callback: MoveFunction) {
        if (!this.hasPassenger(passenger)) {
            return
        }

        val index = getSeatIndex(passenger)
        val seats = computed().seats()
        if (index < 0 || index >= seats.size) return

        val seat = seats[index]
        passengerPos(passenger, callback, seat.position, seat.transform)
    }

    open fun passengerPos(passenger: Entity, callback: MoveFunction, vec3: Vec3, string: String?) {
        val worldPosition = transformPosition(getTransformFromString(string), vec3.x, vec3.y, vec3.z)
        passenger.setPos(worldPosition.x, worldPosition.y, worldPosition.z)
        callback.accept(passenger, worldPosition.x, worldPosition.y, worldPosition.z)
        copyEntityData(passenger)
    }

    protected var positionTransform = HashMap<String, Function<Float, Matrix4d>>()
    protected var vectorTransform = HashMap<String, Function<Float, Vec3>>()
    protected var rotationTransform = HashMap<String, Function<Float, Quaterniond>>()

    //    @Override
    //    public void onAddedToWorld() {
    //        super.onAddedToWorld();
    //        this.setYRot(serverYaw);
    //    }
    init {
        registerTransforms()
        initOBB()

        if (this.hasEnergyStorage()) {
            this.energyStorage = VehicleEnergyStorage(this)
        }
        this.isInitialized = true

        this.health = this.getMaxHealth()
    }

    protected fun registerTransforms() {
        positionTransform["VehicleFlat"] = Function { partialTicks -> this.getVehicleFlatTransform(partialTicks) }
        positionTransform["Turret"] = Function { partialTicks -> this.getTurretTransform(partialTicks) }
        positionTransform["Barrel"] = Function { partialTicks -> this.getBarrelTransform(partialTicks) }
        positionTransform["WeaponStation"] = Function { partialTicks -> this.getGunTransform(partialTicks) }
        positionTransform["WeaponStationBarrel"] =
            Function { partialTicks -> this.getPassengerWeaponStationBarrelTransform(partialTicks) }
        positionTransform["Default"] = Function { ticks -> this.getVehicleTransform(ticks) }

        vectorTransform["Turret"] = Function { pPartialTicks -> this.getTurretVector(pPartialTicks) }
        vectorTransform["Barrel"] = Function { pPartialTicks -> this.getBarrelVector(pPartialTicks) }
        vectorTransform["WeaponStationBarrel"] =
            Function { partialTicks -> this.getPassengerWeaponStationVector(partialTicks) }
        vectorTransform["DeltaMovement"] = Function { _ -> deltaMovement.normalize() }
        vectorTransform["Up"] = Function { ticks -> this.getUpVec(ticks) }
        vectorTransform["Default"] = Function { partialTicks -> this.getViewVector(partialTicks) }

        rotationTransform["WeaponStation"] =
            Function { tick -> VectorTool.combineRotationsPassengerWeaponStation(tick, this) }
        rotationTransform["WeaponStationBarrel"] =
            Function { tick -> VectorTool.combineRotationsPassengerWeaponStationBarrel(tick, this) }
        rotationTransform["Turret"] = Function { tick -> combineRotationsTurret(tick, this) }
        rotationTransform["Barrel"] = Function { tick -> VectorTool.combineRotationsBarrel(tick, this) }
        rotationTransform["RotationsYaw"] = Function { tick -> VectorTool.combineRotationsYaw(tick, this) }
        rotationTransform["Default"] = Function { tick -> VectorTool.combineRotations(tick, this) }
    }

    open fun getTransformFromString(string: String?): Matrix4d {
        return getTransformFromString(string, 1f)
    }

    open fun getTransformFromString(string: String?, ticks: Float): Matrix4d {
        return positionTransform
            .getOrDefault(string, positionTransform["Default"])!!
            .apply(ticks)
    }

    open fun getVectorFromString(string: String?): Vec3 {
        return getVectorFromString(string, 0f)
    }

    open fun getVectorFromString(string: String?, ticks: Float): Vec3 {
        return vectorTransform
            .getOrDefault(string, vectorTransform["Default"])!!
            .apply(ticks)
    }

    open fun getVectorFromString(string: String, ticks: Float, seatIndex: Int): Vec3 {
        val entity = getNthEntity(seatIndex)
        return when (string) {
            "Bomb" -> bombHitPos(getNthEntity(seatIndex)).subtract(getShootPosForHud(getNthEntity(seatIndex), 1f))
            "Passenger" -> if (entity != null) entity.getViewVector(ticks) else getViewVector(ticks)
            "ClientCamera" -> if (entity != null && entity.level().isClientSide) cameraDirection() else getViewVector(
                ticks
            )

            else -> getVectorFromString(string, ticks)
        }
    }

    open fun cameraDirection(): Vec3 {
        return Vec3(Minecraft.getInstance().gameRenderer.mainCamera.lookVector)
    }

    open fun getRotationFromString(string: String?): Quaterniond {
        return getRotationFromString(string, 0f)
    }

    open fun getRotationFromString(string: String?, ticks: Float): Quaterniond {
        return rotationTransform
            .getOrDefault(string, rotationTransform["Default"])!!
            .apply(ticks)
    }

    /**
     * @return 炮弹发射位置
     */
    open fun getShootPos(seatIndex: Int, ticks: Float): Vec3 {
        return getShootPos(getNthEntity(seatIndex), ticks)
    }

    open fun bombHitPos(entity: Entity?): Vec3 {
        val gunData = getGunData(entity)
        return if (gunData != null && level().isClientSide) {
            ProjectileCalculator.calculatePreciseImpactPoint(
                level(),
                getShootPosForHud(entity, 1f),
                getShootVec(entity, 1f),
                deltaMovement.length() * gunData.get(GunProp.VELOCITY),
                -getProjectileGravity(entity).toDouble()
            )
        } else {
            Vec3.ZERO
        }
    }

    /**
     * @param entity 操控载具的实体
     * @return 炮弹发射位置
     */
    open fun getShootPos(entity: Entity?, ticks: Float): Vec3 {
        val data = getGunData(getSeatIndex(entity))
        if (data != null) {
            val vec3 = data.firePosition()

            val worldPosition = transformPosition(
                this.getTransformFromString(data.get(GunProp.SHOOT_POS).transform, ticks),
                vec3.x, vec3.y, vec3.z
            )

            return Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
        }
        return getEyePosition(ticks)
    }

    open fun getShootPos(weaponName: String, ticks: Float): Vec3 {
        val data = getGunData(weaponName)
        if (data != null) {
            val vec3 = data.firePosition()

            val worldPosition = transformPosition(
                this.getTransformFromString(data.get(GunProp.SHOOT_POS).transform, ticks),
                vec3.x, vec3.y, vec3.z
            )

            return Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
        }
        return getEyePosition(ticks)
    }

    /**
     * @param entity 操控载具的实体
     * @return 所有炮弹发射位置的中心点，用于HUD瞄准
     */
    open fun getShootPosForHud(entity: Entity?, ticks: Float): Vec3 {
        val data = getGunData(getSeatIndex(entity))
        if (data != null) {
            val vec3 = data.firePositionForHud()

            val worldPosition = transformPosition(
                this.getTransformFromString(data.get(GunProp.SHOOT_POS).transform, ticks),
                vec3.x, vec3.y, vec3.z
            )

            return Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
        }
        return getEyePosition(ticks)
    }

    /**
     * @param entity 操控载具的实体
     * @return 所有炮弹发射位置的方向，用于HUD瞄准
     */
    open fun getShootDirectionForHud(entity: Entity, partialTicks: Float): Vec3 {
        val data = getGunData(getSeatIndex(entity)) ?: return getViewVector(partialTicks)

        val stringOrVec3 = data.fireDirectionForHud()

        if (stringOrVec3 == null) {
            return getViewVec(entity, partialTicks)
        } else if (stringOrVec3.isString) {
            return getVectorFromString(stringOrVec3.string!!, partialTicks, getSeatIndex(entity))
        } else {
            val vec3 = stringOrVec3.vec3!!
            val worldPosition = transformPosition(
                getTransformFromString(data.get(GunProp.SHOOT_POS).transform, partialTicks),
                vec3.x + stringOrVec3.vec3.x,
                vec3.y + stringOrVec3.vec3.y,
                vec3.z + stringOrVec3.vec3.z
            )

            val worldPositionO = transformPosition(
                getTransformFromString(data.get(GunProp.SHOOT_POS).transform, partialTicks),
                vec3.x, vec3.y, vec3.z
            )

            val startPos = Vec3(worldPositionO.x, worldPositionO.y, worldPositionO.z)
            val endPos = Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
            return startPos.vectorTo(endPos).normalize()
        }
    }

    open fun getShootVec(seatIndex: Int, ticks: Float): Vec3? {
        return getShootVec(getNthEntity(seatIndex), ticks)
    }

    open fun getShootVec(entity: Entity?, partialTicks: Float): Vec3 {
        return VehicleVecUtils.getShootVec(this, entity, partialTicks)
    }

    open fun getShootVec(weaponName: String, partialTicks: Float): Vec3 {
        return VehicleVecUtils.getShootVec(this, weaponName, partialTicks)
    }

    open fun getViewVec(entity: Entity, partialTicks: Float): Vec3 {
        return VehicleVecUtils.getViewVec(this, entity, partialTicks)
    }

    open fun getViewPos(entity: Entity, partialTicks: Float): Vec3? {
        return VehicleVecUtils.getViewPos(this, entity, partialTicks)
    }

    open fun getSeekVec(entity: Entity?, partialTicks: Float): Vec3? {
        return VehicleVecUtils.getSeekVec(this, entity, partialTicks)
    }

    open fun getSeekVec(seatIndex: Int, partialTicks: Float): Vec3? {
        return VehicleVecUtils.getSeekVec(this, getNthEntity(seatIndex), partialTicks)
    }

    open fun getPlayerLookAtEntityOnVehicle(shooter: Entity, entityReach: Double, partialTick: Float): Entity? {
        val eye = getShootPosForHud(shooter, partialTick)
        val distance = entityReach * entityReach
        var hitResult = TraceTool.pickNew(eye, 512.0, this)

        val viewVec = getViewVec(shooter, partialTick)
        val toVec = eye.add(viewVec.x * entityReach, viewVec.y * entityReach, viewVec.z * entityReach)
        val aabb = boundingBox.expandTowards(viewVec.scale(entityReach)).inflate(1.0)
        val entityHitResult = ProjectileUtil.getEntityHitResult(
            this, eye, toVec, aabb,
            { p ->
                !p!!.isSpectator
                        && p.isAlive
                        && SeekTool.BASIC_FILTER.test(p)
                        && !p.type.`is`(ModTags.EntityTypes.DECOY)
                        && SeekTool.NOT_IN_SMOKE.test(p)
                        && p !== shooter
                        && p.vehicle != shooter.vehicle
                        && (p !is Projectile)
            }, distance
        )
        if (entityHitResult != null) {
            hitResult = entityHitResult
        }
        if (hitResult!!.type == HitResult.Type.ENTITY) {
            if (entityHitResult != null) {
                return entityHitResult.entity
            }
        }
        return null
    }

    /**
     * @param entity 操控载具的实体
     * @return 炮弹发射时的初始速度
     */
    open fun getProjectileVelocity(entity: Entity?): Float {
        val gunData = getGunData(getSeatIndex(entity)) ?: return 25f
        if (gunData.get(GunProp.ADD_SHOOTER_DELTA_MOVEMENT)) {
            return (deltaMovement.length() * gunData.get(GunProp.VELOCITY)).toFloat()
        }

        return gunData.get(GunProp.VELOCITY).toFloat()
    }

    open fun getProjectileVelocity(seatIndex: Int): Float {
        val gunData = getGunData(seatIndex) ?: return 25f
        if (gunData.get(GunProp.ADD_SHOOTER_DELTA_MOVEMENT)) {
            return (deltaMovement.length() * gunData.get(GunProp.VELOCITY)).toFloat()
        }

        return gunData.get(GunProp.VELOCITY).toFloat()
    }

    open fun getProjectileVelocity(weaponName: String): Float {
        val gunData = getGunData(weaponName) ?: return 25f
        if (gunData.get(GunProp.ADD_SHOOTER_DELTA_MOVEMENT)) {
            return (deltaMovement.length() * gunData.get(GunProp.VELOCITY)).toFloat()
        }

        return gunData.get(GunProp.VELOCITY).toFloat()
    }

    open fun getProjectileVelocity(gunData: GunData?): Float {
        if (gunData == null) return 25f
        if (gunData.get(GunProp.ADD_SHOOTER_DELTA_MOVEMENT)) {
            return (deltaMovement.length() * gunData.get(GunProp.VELOCITY)).toFloat()
        }
        return gunData.get(GunProp.VELOCITY).toFloat()
    }

    /**
     * @param entity 操控载具的实体
     * @return 炮弹重力
     */
    open fun getProjectileGravity(entity: Entity?): Float {
        val gunData = getGunData(getSeatIndex(entity)) ?: return 0f

        return gunData.get(GunProp.GRAVITY).toFloat()
    }

    open fun getProjectileGravity(seatIndex: Int): Float {
        val gunData = getGunData(seatIndex) ?: return 0f

        return gunData.get(GunProp.GRAVITY).toFloat()
    }

    open fun getProjectileGravity(weaponName: String): Float {
        val gunData = getGunData(weaponName) ?: return 0f

        return gunData.get(GunProp.GRAVITY).toFloat()
    }

    open fun getProjectileGravity(gunData: GunData?): Float {
        if (gunData == null) return 0f
        return gunData.get(GunProp.GRAVITY).toFloat()
    }

    /**
     * @param entity 操控载具的实体
     * @return 炮弹发射时的散布
     */
    open fun getProjectileSpread(entity: Entity?): Float {
        val gunData = getGunData(getSeatIndex(entity)) ?: return 0.5f

        return gunData.get(GunProp.SPREAD).toFloat()
    }

    open fun getProjectileSpread(seatIndex: Int): Float {
        val gunData = getGunData(seatIndex) ?: return 0.5f

        return gunData.get(GunProp.SPREAD).toFloat()
    }

    open fun getProjectileSpread(weaponName: String): Float {
        val gunData = getGunData(weaponName) ?: return 0.5f

        return gunData.get(GunProp.SPREAD).toFloat()
    }

    open fun getProjectileSpread(gunData: GunData?): Float {
        if (gunData == null) return 0.5f
        return gunData.get(GunProp.SPREAD).toFloat()
    }

    /**
     * 根据UUID，使乘客位武器自动瞄准
     *
     * @param uuid    目标的UUID字符串
     * @param pLiving 操控载具的实体
     */
    open fun passengerWeaponAutoAimFormUuid(uuid: String?, pLiving: LivingEntity) {
        var target = EntityFindUtil.findEntity(level(), uuid)
        if (target != null) {
            if (target.vehicle != null) {
                target = target.vehicle
            }

            val targetPos = target!!.boundingBox.center
            var targetVel = target.deltaMovement

            if (target is LivingEntity) {
                val gravity = target.getAttributeValue(Attributes.GRAVITY)
                targetVel = targetVel.add(0.0, gravity, 0.0)
            }

            if (target is Player) {
                targetVel = targetVel.multiply(2.0, 1.0, 2.0)
            }

            val targetVec = calculateFiringSolution(
                getShootPos(pLiving, 1f).subtract(
                    getShootVec(pLiving, 1f).scale(
                        getShootPos(
                            pLiving,
                            1f
                        ).distanceTo(pLiving.position())
                    )
                ),
                targetPos,
                targetVel,
                getProjectileVelocity(pLiving).toDouble(),
                getProjectileGravity(pLiving).toDouble()
            )
            passengerWeaponAutoAimFormVector(targetVec)
        }
    }

    /**
     * 根据方向向量，使乘客位武器自动瞄准
     *
     * @param shootVec 需要让武器站以这个角度发射的向量
     */
    open fun passengerWeaponAutoAimFormVector(shootVec: Vec3) {
        val ySpeed = this.passengerWeaponYSpeed
        val xSpeed = this.passengerWeaponXSpeed
        val diffY = Mth.wrapDegrees(
            -getYRotFromVector(shootVec) + getYRotFromVector(
                getPassengerWeaponStationVector(1f)
            )
        ).toFloat()
        val diffX = Mth.wrapDegrees(
            -getXRotFromVector(shootVec) + getXRotFromVector(
                getPassengerWeaponStationVector(1f)
            )
        ).toFloat()

        this.turretTurnSound(diffX, diffY, 0.95f)

        this.gunXRot = Mth.clamp(
            this.gunXRot + Mth.clamp(diffX, -xSpeed, xSpeed),
            -this.passengerWeaponMaxPitch,
            -this.passengerWeaponMinPitch
        )
        this.gunYRot = Mth.clamp(
            this.gunYRot - Mth.clamp(diffY, -ySpeed, ySpeed),
            -this.passengerWeaponMaxYaw,
            -this.passengerWeaponMinYaw
        )
    }

    open fun adjustWeaponControllerAngle() {
        val entity = getNthEntity(this.passengerWeaponStationControllerIndex)
        val pos: Vec3? = passengerWeaponStationBarrelPosition
        if (entity != null && pos != null) {
            val aimPos = boundingBox.center.add(entity.getViewVector(1f).scale(512.0))

            val transform: Matrix4d = getGunTransform(1f)
            val worldPosition = transformPosition(transform, pos.x, pos.y, pos.z)

            val aimVec = Vec3(worldPosition.x, worldPosition.y, worldPosition.z).vectorTo(aimPos)
            passengerWeaponAutoAimFormVector(aimVec)
        }

        if (entity == null) {
            gunYRot += turretYRotLock
        }
    }

    open fun destroy() {
        val destroyInfo = computed().destroyInfo

        if (vehicleType != VehicleType.AIRPLANE && vehicleType != VehicleType.HELICOPTER) {
            if (destroyInfo.explodePassengers) {
                if (this.crash && destroyInfo.crashPassengers) {
                    crashPassengers()
                } else {
                    explodePassengers()
                }
            }
            vehicleExplosion(destroyInfo)
        }

        if (hasTurret() && destroyInfo.sympatheticDetonation && Math.random() < destroyInfo.sympatheticDetonationChance) {
            sympatheticDetonated = true
            val turretWreckEntity = TurretWreckEntity(ModEntities.TURRET_WRECK.get(), level())
            if (turretPos != null) {
                val pos = turretPos?.let { position().add(it) }
                pos?.let { turretWreckEntity.setPos(it.x, pos.y, pos.z) }
            } else {
                turretWreckEntity.setPos(this.x, this.eyeY, this.z)
            }

            val dir = getUpVec(1f).add(deltaMovement + Vec3(0.0, this.computed().gravity, 0.0))

            val rdm = (Math.random() - 0.5) * 0.4 + 1
            turretWreckEntity.deltaMovement = Vec3(dir.x, dir.y, dir.z).normalize().add(
                random.triangle(0.0, 0.0172275 * 12.toDouble()),
                random.triangle(0.0, 0.0172275 * 12.toDouble()),
                random.triangle(0.0, 0.0172275 * 12.toDouble())
            ).scale(destroyInfo.sympatheticDetonationForce.toDouble() * rdm)

            val quaternion = combineRotationsTurret(1f, this)
            turretWreckEntity.vehicleName = BuiltInRegistries.ENTITY_TYPE.getKey(this.type).toString()
            turretWreckEntity.xRot = this.getTurretPitch(1f)
            turretWreckEntity.yRot = -getYRotFromVector(getBarrelVector(1f)).toFloat()
            turretWreckEntity.setQuaternion0(quaternion)
            turretWreckEntity.setQuaternion(quaternion)
            level().addFreshEntity(turretWreckEntity)
        }

        if (destroyInfo.noWreck) {
            discard()
        }
    }

    open fun vehicleExplosion(destroyInfo: DestroyInfo) {
        val radius = destroyInfo.explosionRadius
        if (radius > 0) {
            queueServerWork(1) {
                val damage = destroyInfo.explosionDamage
                val particleType = destroyInfo.particleType

                val explosion = createCustomExplosion()
                    .radius(radius)
                    .damage(damage)
                    .withParticleType(particleType)

                if (!destroyInfo.explodeBlocks) {
                    explosion.keepBlock()
                }

                explosion.explode()
            }
        }
    }

    open fun createCustomExplosion(): CustomExplosion.Builder = CustomExplosion.Builder(this)
        .attacker(this.lastAttacker)

    protected fun crashPassengers() {
        for (entity in this.getPassengers()) {
            if (entity is LivingEntity) {
                repeat(VehicleConfig.AIR_CRASH_EXPLOSION_COUNT.get()) {
                    val tempAttacker = if (entity === this.lastAttacker) null else this.lastAttacker
                    entity.invulnerableTime = 0
                    entity.hurt(
                        ModDamageTypes.causeAirCrashDamage(this.level().registryAccess(), null, tempAttacker),
                        VehicleConfig.AIR_CRASH_EXPLOSION_DAMAGE.get().toFloat()
                    )
                }
            }
        }
    }

    protected fun explodePassengers() {
        for (entity in this.getPassengers()) {
            if (entity !is LivingEntity) continue
            repeat(VehicleConfig.SELF_EXPLOSION_COUNT.get()) {
                val tempAttacker = if (entity === this.lastAttacker) null else this.lastAttacker
                entity.invulnerableTime = 0
                entity.hurt(
                    ModDamageTypes.causeVehicleExplosionDamage(
                        this.level().registryAccess(),
                        null,
                        tempAttacker
                    ), VehicleConfig.SELF_EXPLOSION_DAMAGE.get().toFloat()
                )
            }
        }
    }

    open fun travel() {
        val computed = computed()

        val engineType = computed.engineType
        if (engineType == EngineType.EMPTY) return
        if (engineType == EngineType.FIXED) {
            this.fixedEngine()
            return
        }

        if (this.engineInfo == null) {
            val engineInfo = computed.engineInfo
            try {
                val serializer = when (engineType) {
                    EngineType.WHEEL -> Wheel.serializer()
                    EngineType.TRACK -> Track.serializer()
                    EngineType.HELICOPTER -> Helicopter.serializer()
                    EngineType.SHIP -> Ship.serializer()
                    EngineType.AIRCRAFT -> Aircraft.serializer()
                    EngineType.WHEELCHAIR -> WheelChair.serializer()
                    EngineType.TOM6 -> Tom6.serializer()

                    else -> null
                }

                this.engineInfo = if (serializer == null) {
                    null
                } else {
                    DataLoader.JSON.decodeFromJsonElement(serializer, engineInfo.toKxJson())
                }
            } catch (e: Exception) {
                Mod.LOGGER.error("Failed to parse engine info for vehicle {}, {}", this, e)
            }
        } else {
            this.engineInfo!!.work(this)
        }
    }

    open fun getEngineSoundVolume(): Float {
        val computed = computed()

        val engineType = computed.engineType
        if (engineType == EngineType.EMPTY || engineType == EngineType.FIXED) return 0f

        val engineInfo = this.engineInfo ?: return 0f

        return when (engineType) {
            EngineType.TRACK -> Math.max(
                Mth.abs(power),
                Mth.abs(1.4f * deltaRot)
            ) * engineInfo.engineSoundVolume

            EngineType.HELICOPTER -> synchedPropellerRot * engineInfo.engineSoundVolume
            else -> Mth.abs(power) * engineInfo.engineSoundVolume
        }
    }

    open fun getVehicleTransform(ticks: Float): Matrix4d {
        val transformV = this.getVehicleYOffsetTransform(ticks)
        val transform = Matrix4d()
        val worldPosition = transformPosition(transform, 0.0, -this.rotateOffsetHeight, 0.0)
        transformV.translate(worldPosition.x, worldPosition.y, worldPosition.z)
        return transformV
    }

    open fun getVehicleTransformWithCustomPitch(ticks: Float): Matrix4d {
        val transformV = this.getVehicleYOffsetTransform(ticks)
        val transform = Matrix4d()
        val worldPosition = transformPosition(transform, 0.0, -this.rotateOffsetHeight, 0.0)
        transformV.translate(worldPosition.x, worldPosition.y, worldPosition.z)
        transformV.rotate(Axis.XP.rotationDegrees(turretCustomPitch))
        return transformV
    }

    // From Immersive_Aircraft
    open fun getVehicleYOffsetTransform(partialTicks: Float): Matrix4d {
        return VehicleVecUtils.getVehicleYOffsetTransform(this, partialTicks)
    }

    open val rotateOffsetHeight: Double
        get() = computed().rotateOffsetHeight.toDouble()

    open fun getVehicleFlatTransform(partialTicks: Float): Matrix4d {
        return VehicleVecUtils.getVehicleFlatTransform(this, partialTicks)
    }

    open fun getClientVehicleTransform(partialTicks: Float): Matrix4d {
        return VehicleVecUtils.getClientVehicleTransform(this, partialTicks)
    }

    open fun hasTurret() = this.turretPos != null

    open val turretPos: Vec3?
        get() = computed().turretPos

    open val turretControllerIndex: Int
        get() = computed().turretControllerIndex

    open val turretTurnXSpeed: Float
        /**
         * @return 炮塔最大俯仰速度
         */
        get() = computed().turretTurnSpeed.x

    open val turretTurnYSpeed: Float
        /**
         * @return 炮塔最大偏航速度
         */
        get() = computed().turretTurnSpeed.y

    open val turretMinYaw: Float
        /**
         * @return 炮塔最小偏航
         */
        get() = computed().turretYawRange.x

    open val turretMaxYaw: Float
        /**
         * @return 炮塔最大偏航
         */
        get() = computed().turretYawRange.y

    open val turretMinPitch: Float
        /**
         * @return 炮塔最小俯角
         */
        get() = computed().turretPitchRange.x

    open val turretMaxPitch: Float
        /**
         * @return 炮塔最大仰角
         */
        get() = computed().turretPitchRange.y

    open val barrelPosition: Vec3?
        get() = computed().barrelPos

    open fun hasPassengerWeaponStation(): Boolean {
        return this.passengerWeaponStationPosition != null
    }

    open val passengerWeaponStationPosition: Vec3?
        get() = computed().passengerWeaponStationPos

    open val passengerWeaponStationBarrelPosition: Vec3?
        get() = computed().passengerWeaponStationBarrelPos

    open val passengerWeaponStationControllerIndex: Int
        get() = computed().passengerWeaponStationControllerIndex

    open val passengerWeaponYSpeed: Float
        /**
         * @return 乘客武器站最大偏航速度
         */
        get() = computed().passengerWeaponStationTurnSpeed.y

    open val passengerWeaponXSpeed: Float
        /**
         * @return 乘客武器站最大俯仰速度
         */
        get() = computed().passengerWeaponStationTurnSpeed.x

    open val passengerWeaponMinPitch: Float
        /**
         * @return 乘客武器站最小仰角
         */
        get() = computed().passengerWeaponStationPitchRange.x

    open val passengerWeaponMaxPitch: Float
        /**
         * @return 乘客武器站最大仰角
         */
        get() = computed().passengerWeaponStationPitchRange.y

    open val passengerWeaponMinYaw: Float
        /**
         * @return 炮塔最小偏航
         */
        get() = computed().passengerWeaponStationYawRange.x

    open val passengerWeaponMaxYaw: Float
        /**
         * @return 炮塔最大偏航
         */
        get() = computed().passengerWeaponStationYawRange.y


    open val turretCustomPitch: Float
        /**
         * @return 炮塔自定义俯仰
         */
        get() = computed().turretCustomPitch


    open fun getTurretTransform(partialTicks: Float): Matrix4d {
        return VehicleVecUtils.getTurretTransform(this, partialTicks)
    }

    open fun getTurretVector(pPartialTicks: Float): Vec3 {
        return VehicleVecUtils.getTurretVector(this, pPartialTicks)
    }

    open fun getBarrelTransform(partialTicks: Float): Matrix4d {
        return VehicleVecUtils.getBarrelTransform(this, partialTicks)
    }

    open fun getGunTransform(partialTicks: Float): Matrix4d {
        return VehicleVecUtils.getGunTransform(this, partialTicks)
    }

    open fun getPassengerWeaponStationBarrelTransform(partialTicks: Float): Matrix4d {
        return VehicleVecUtils.getPassengerWeaponStationBarrelTransform(this, partialTicks)
    }

    open fun getPassengerWeaponStationVector(partialTicks: Float): Vec3 {
        return VehicleVecUtils.getPassengerWeaponStationVector(this, partialTicks)
    }

    open fun transformPosition(transform: Matrix4d, x: Double, y: Double, z: Double): Vector4d {
        return transform.transform(Vector4d(x, y, z, 1.0))
    }

    open fun handleClientSync() {
        if (level() is ServerLevel && tickCount % 2 == 0) {
            serverYaw = yRot
            serverPitch = xRot
        }
        if (isControlledByLocalInstance) {
            interpolationSteps = 0
            syncPacketPositionCodec(x, y, z)
        }
        if (interpolationSteps <= 0) {
            return
        }

        val interpolatedX = x + (xO - x) / interpolationSteps.toDouble()
        val interpolatedY = y + (yO - y) / interpolationSteps.toDouble()
        val interpolatedZ = z + (zO - z) / interpolationSteps.toDouble()

        val diffY = Mth.wrapDegrees(serverYaw - this.yRot)
        val diffX = Mth.wrapDegrees(serverPitch - this.xRot)

        this.yRot += 0.1f * diffY
        this.xRot += 0.1f * diffX

        setPos(interpolatedX, interpolatedY, interpolatedZ)

        --interpolationSteps
    }

    override fun lerpTo(x: Double, y: Double, z: Double, yRot: Float, xRot: Float, steps: Int) {
        this.xO = x
        this.yO = y
        this.zO = z
        this.interpolationSteps = 10
    }

    @Deprecated("")
    protected fun getDismountOffset(vehicleWidth: Double, passengerWidth: Double): Vec3 {
        return VehicleMiscUtils.getDismountOffset(this, vehicleWidth, passengerWidth)
    }

    override fun getDismountLocationForPassenger(passenger: LivingEntity): Vec3 {
        val index = this.getTagSeatIndex(passenger)
        return if (index < 0) {
            super.getDismountLocationForPassenger(passenger)
        } else {
            this.getDismountLocationForIndex(passenger, index)
        }
    }

    /**
     * 获取第N个乘客的坐下位置
     *
     * @param passenger 乘客
     * @param index     座位
     * @return 下车的位置
     */
    open fun getDismountLocationForIndex(passenger: LivingEntity, index: Int): Vec3 {
        val seats = this.computed().seats()
        if (index >= seats.size) return dismount(passenger)

        val dismountInfo = seats[index].dismountInfo
        if (dismountInfo != null) {
            val vec3 = dismountInfo.position
            if (vec3 != null) {
                val worldPosition = transformPosition(
                    this.getTransformFromString(dismountInfo.transform),
                    vec3.x, vec3.y, vec3.z
                )
                return Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
            } else {
                return dismount(passenger)
            }
        } else {
            return dismount(passenger)
        }
    }

    open fun dismount(passenger: LivingEntity): Vec3 {
        val vec3d = VehicleMiscUtils.getDismountOffset(
            this,
            (bbWidth * Mth.SQRT_OF_TWO).toDouble(),
            (passenger.bbWidth * Mth.SQRT_OF_TWO).toDouble()
        )
        val ox = x - vec3d.x
        val oz = z + vec3d.z
        val exitPos = BlockPos(ox.toInt(), y.toInt(), oz.toInt())
        val floorPos = exitPos.below()
        if (!level().isWaterAt(floorPos)) {
            val list = mutableListOf<Vec3>()
            val exitHeight = level().getBlockFloorHeight(exitPos)
            if (DismountHelper.isBlockFloorValid(exitHeight)) {
                list.add(Vec3(ox, exitPos.y.toDouble() + exitHeight, oz))
            }
            val floorHeight = level().getBlockFloorHeight(floorPos)
            if (DismountHelper.isBlockFloorValid(floorHeight)) {
                list.add(Vec3(ox, floorPos.y.toDouble() + floorHeight, oz))
            }
            for (entityPose in passenger.dismountPoses) {
                for (vec3d2 in list) {
                    if (!DismountHelper.canDismountTo(level(), vec3d2, passenger, entityPose)) continue
                    passenger.pose = entityPose
                    return vec3d2
                }
            }
        }
        return super.getDismountLocationForPassenger(passenger)
    }

    open fun getEjectionPosition(passenger: LivingEntity, index: Int): Vec3 {
        val seats = this.computed().seats()
        if (index >= seats.size) return passenger.position()

        val dismountInfo = seats[index].dismountInfo
        if (dismountInfo != null) {
            val vec3 = dismountInfo.ejectPosition ?: return passenger.position()
            val worldPosition = transformPosition(
                this.getTransformFromString(dismountInfo.transform),
                vec3.x, vec3.y, vec3.z
            )

            return Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
        }
        return passenger.position()
    }

    open fun allowEjection(seatIndex: Int) =
        computed().seats().getOrNull(seatIndex)?.dismountInfo?.canEject ?: false

    open fun removeSeatIndexTag(entity: Entity) {
        entity.persistentData.remove(TAG_SEAT_INDEX)
    }

    open fun getEjectionMovement(entity: LivingEntity?, index: Int): Vec3 {
        val dismountInfo = this.computed().seats().getOrNull(index)?.dismountInfo ?: return deltaMovement

        val force = dismountInfo.ejectForce
        val stringOrVec3 = dismountInfo.ejectDirection

        if (stringOrVec3 == null) {
            return deltaMovement.add(getUpVec(1f).scale(force))
        } else if (stringOrVec3.isString) {
            return deltaMovement.add(
                getVectorFromString(
                    stringOrVec3.string!!,
                    1f,
                    getSeatIndex(entity)
                ).scale(force)
            )
        } else {
            val vec3 = stringOrVec3.vec3!!
            val worldPosition = transformPosition(
                getTransformFromString(dismountInfo.transform),
                vec3.x + stringOrVec3.vec3.x,
                vec3.y + stringOrVec3.vec3.y,
                vec3.z + stringOrVec3.vec3.z
            )

            val worldPositionO = transformPosition(
                getTransformFromString(dismountInfo.transform),
                vec3.x,
                vec3.y,
                vec3.z
            )

            val startPos = Vec3(worldPositionO.x, worldPositionO.y, worldPositionO.z)
            val endPos = Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
            return deltaMovement.add(startPos.vectorTo(endPos).normalize().scale(force))
        }
    }

    open val vehicleIcon: ResourceLocation?
        get() = computed().vehicleIcon

    open fun allowFreeCam() = computed().allowFreeCam

    open fun getUpVec(ticks: Float): Vec3 {
        val transform = getVehicleTransform(ticks)
        val force0 = transformPosition(transform, 0.0, 0.0, 0.0)
        val force1 = transformPosition(transform, 0.0, 1.0, 0.0)
        return Vec3(force0.x, force0.y, force0.z).vectorTo(Vec3(force1.x, force1.y, force1.z))
    }

    open fun getRightVec(ticks: Float): Vec3 {
        val transform = getVehicleTransform(ticks)
        val force0 = transformPosition(transform, 0.0, 0.0, 0.0)
        val force1 = transformPosition(transform, -1.0, 0.0, 0.0)
        return Vec3(force0.x, force0.y, force0.z).vectorTo(Vec3(force1.x, force1.y, force1.z))
    }

    // 本方法留空
    override fun push(pX: Double, pY: Double, pZ: Double) {}

    open fun getBarrelVector(pPartialTicks: Float): Vec3 {
        val transform = getBarrelTransform(pPartialTicks)
        val rootPosition = transformPosition(transform, 0.0, 0.0, 0.0)
        val targetPosition = transformPosition(transform, 0.0, 0.0, 1.0)
        return Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(
            Vec3(
                targetPosition.x,
                targetPosition.y,
                targetPosition.z
            )
        )
    }

    open fun getBarrelXRot(pPartialTicks: Float): Float {
        return Mth.lerp(pPartialTicks, turretXRotO - this.xRotO, this.turretXRot - this.xRot)
    }

    open fun getBarrelYRot(pPartialTick: Float): Float {
        return -Mth.lerp(pPartialTick, turretYRotO - this.yRotO, this.turretYRot - this.yRot)
    }

    open fun getGunXRot(pPartialTicks: Float): Float {
        return Mth.lerp(pPartialTicks, gunXRotO - this.xRotO, this.gunXRot - this.xRot)
    }

    open fun getGunYRot(pPartialTick: Float): Float {
        return -Mth.lerp(pPartialTick, gunYRotO - this.yRotO, this.gunYRot - this.yRot)
    }

    open fun getTurretYaw(pPartialTick: Float): Float {
        return Mth.lerp(pPartialTick, turretYRotO, this.turretYRot)
    }

    open fun getTurretPitch(pPartialTick: Float): Float {
        return Mth.lerp(pPartialTick, turretXRotO, this.turretXRot)
    }

    open fun getCameraPos(entity: Entity, partialTicks: Float): Vec3 {
        return VehicleVecUtils.getCameraPos(this, entity, partialTicks)
    }

    open fun cameraDirection(entity: Entity, partialTicks: Float): Vec3 {
        return VehicleVecUtils.getCameraDirection(this, entity, partialTicks)
    }

    open fun getZoomPos(entity: Entity, partialTicks: Float): Vec3 {
        return VehicleVecUtils.getZoomPos(this, entity, partialTicks)
    }

    open fun getZoomDirection(entity: Entity, partialTicks: Float): Vec3 {
        return VehicleVecUtils.getZoomDirection(this, entity, partialTicks)
    }

    open val mouseSensitivity: Double
        get() = computed().mouseSensitivity

    open val passengerRenderScale: Float
        get() = computed().passengerRenderScale

    open fun gearRot(tickDelta: Float) = Mth.lerp(tickDelta, gearRotO, this.gearRot)

    open val mass: Float
        get() = computed().mass

    override fun setDeltaMovement(pDeltaMovement: Vec3) {
        val currentMomentum = this.deltaMovement

        // 计算当前速度和新速度的标量大小
        val currentSpeedSq = currentMomentum.lengthSqr()
        val newSpeedSq = pDeltaMovement.lengthSqr()

        // 只在新速度大于当前速度时（加速过程）进行检查
        if (newSpeedSq > currentSpeedSq) {
            // 计算加速度向量
            val acceleration = pDeltaMovement.subtract(currentMomentum)

            // 检查加速度大小是否超过阈值
            if (acceleration.lengthSqr() > 8) {
                // 限制加速度不超过阈值
                val limitedAcceleration = acceleration.normalize().scale(0.125)
                val finalMomentum = currentMomentum.add(limitedAcceleration)

                super.setDeltaMovement(finalMomentum)
                return
            }
        }
        // 对于减速或允许的加速，直接设置新动量
        super.setDeltaMovement(pDeltaMovement)
    }

    override fun addDeltaMovement(pAddend: Vec3) {
        var pAddend = pAddend
        val length = pAddend.length()
        if (length > 0.1) pAddend = pAddend.scale(0.1 / length)

        super.addDeltaMovement(pAddend)
    }

    /**
     * 玩家在载具上的灵敏度调整
     *
     * @param original   原始灵敏度
     * @param zoom       是否在载具上瞄准
     * @param seatIndex  玩家座位
     * @param isOnGround 载具是否在地面
     * @return 调整后的灵敏度
     */
    open fun getSensitivity(original: Double, zoom: Boolean, seatIndex: Int, isOnGround: Boolean): Double {
        val seat = computed().seats()[seatIndex]
        val sensitivity = seat.sensitivity
        return if (zoom) sensitivity.x * original else if (Minecraft.getInstance().options.cameraType
                .isFirstPerson
        ) sensitivity.y * original else sensitivity.z * original
    }

    open val vehicleItemIcon: ResourceLocation?
        /**
         * 载具在集装箱物品上显示的贴图
         */
        get() = computed().containerIcon

    /**
     * 判断一个座位是否是封闭的（封闭载具座位具有免疫负面效果等功能）
     * 默认认为隐藏乘客的座位均为封闭座位
     *
     * @param index 位置
     */
    open fun isEnclosed(index: Int): Boolean {
        val seats = computed().seats()

        val seat = seats.getOrNull(index) ?: return false
        if (seat.isEnclosed == null) {
            return seat.hidePassenger
        }

        return seat.isEnclosed!!
    }

    open fun isEnclosed(passenger: Entity?): Boolean {
        return isEnclosed(getSeatIndex(passenger))
    }

    /**
     * 是否禁用玩家手臂
     *
     * @param entity 玩家
     */
    open fun banHand(entity: LivingEntity?): Boolean {
        val index = getSeatIndex(entity)

        val gunData = getGunData(index)
        val seat = computed().seats().getOrNull(index) ?: return false
        return gunData != null || seat.banHand
    }

    /**
     * 是否隐藏载具上的玩家
     *
     * @return 是否隐藏
     */
    open fun hidePassenger(index: Int): Boolean {
        val seats = computed().seats()
        if (index < 0 || index >= seats.size) return false

        val seat = seats[index]
        return seat.hidePassenger
    }

    open fun hidePassenger(passenger: Entity?) = hidePassenger(getSeatIndex(passenger))

    open fun getAmmoCount(living: LivingEntity?): Int {
        val data = getGunData(getSeatIndex(living)) ?: return 0
        return getAmmo(data)
    }

    open fun getAmmoCount(seatIndex: Int): Int {
        val data = getGunData(seatIndex) ?: return 0
        return getAmmo(data)
    }

    open fun getAmmoCount(weaponName: String): Int {
        val data = getGunData(weaponName) ?: return 0
        return getAmmo(data)
    }

    open fun getAmmo(data: GunData) = if (data.useBackpackAmmo()) data.backupAmmoCount.get() else data.ammo.get()

    override fun getPickResult(): ItemStack? {
        if (!this.getRetrieveItems().isEmpty()) {
            return this.getRetrieveItems().firstOrNull()
        }
        return ContainerBlockItem.createInstance(this.type)
    }

    open fun useAircraftCamera(seatIndex: Int): Boolean {
        val seat = computed().seats().getOrNull(seatIndex)
        if (seat != null) {
            val data = seat.cameraPos
            return data?.useAircraftCamera ?: false
        } else {
            return false
        }
    }

    /**
     * 获取视角旋转
     *
     * @param zoom          是否在载具上瞄准
     * @param isFirstPerson 是否是第一人称视角
     */
    @OnlyIn(Dist.CLIENT)
    open fun getCameraRotation(partialTicks: Float, player: Player, zoom: Boolean, isFirstPerson: Boolean): Vec2? {
        val index = this.getSeatIndex(player)
        val seat = computed().seats().getOrNull(index)
        val gunData = getGunData(player)
        if (seat != null) {
            val data = seat.cameraPos
            if (data != null) {
                if (zoom && gunData != null && gunData.get(GunProp.SHOOT_POS).viewDirection != null) {
                    return Vec2(
                        -getYRotFromVector(getViewVec(player, partialTicks)).toFloat(),
                        -getXRotFromVector(getViewVec(player, partialTicks)).toFloat()
                    )
                }
                if (useAircraftCamera(index)) {
                    return Vec2(
                        (getYaw(partialTicks) - ClientMouseHandler.freeCameraYaw).toFloat(),
                        (getPitch(partialTicks) + ClientMouseHandler.freeCameraPitch).toFloat()
                    )
                }
                if (zoom || isFirstPerson) {
                    return Vec2(
                        -getYRotFromVector(cameraDirection(player, partialTicks)).toFloat(),
                        -getXRotFromVector(cameraDirection(player, partialTicks)).toFloat()
                    )
                }
            } else {
                return null
            }
        }
        return null
    }

    /**
     * 获取视角位置
     *
     * @param zoom          是否在载具上瞄准
     * @param isFirstPerson 是否是第一人称视角
     */
    @OnlyIn(Dist.CLIENT)
    open fun getCameraPosition(partialTicks: Float, player: Player, zoom: Boolean, isFirstPerson: Boolean): Vec3? {
        val index = this.getSeatIndex(player)
        val seat = computed().seats().getOrNull(index)
        if (seat != null) {
            val data = seat.cameraPos
            val gunData = getGunData(player)
            if (data != null) {
                if (zoom || isFirstPerson) {
                    return if (zoom) {
                        if (gunData != null && gunData.get(GunProp.SHOOT_POS).viewPosition != null) {
                            getViewPos(player, partialTicks)
                        } else {
                            getZoomPos(player, partialTicks)
                        }
                    } else {
                        getCameraPos(player, partialTicks)
                    }
                } else if (useAircraftCamera(index)) {
                    val transform = getClientVehicleTransform(partialTicks)
                    val maxCameraPosition = transformPosition(
                        transform,
                        data.aircraftCameraPos.x,
                        data.aircraftCameraPos.y + 0.1 * ClientMouseHandler.custom3pDistanceLerp,
                        data.aircraftCameraPos.z - ClientMouseHandler.custom3pDistanceLerp
                    )
                    return maxCameraPosition.maxZoom(transform)
                }
            }
            return null
        }
        return null
    }

    /**
     * 是否使用载具固定视角
     */
    @OnlyIn(Dist.CLIENT)
    open fun useFixedCameraPos(entity: Entity?): Boolean {
        val index = this.getSeatIndex(entity)
        val seat = computed().seats().getOrNull(index)
        if (seat != null) {
            val data = seat.cameraPos
            if (data != null) {
                return data.useFixedCameraPos
            }
        }
        return false
    }

    /**
     * 瞄准时的放大倍率
     *
     * @return 放大倍率
     */
    open fun getDefaultZoom(entity: Entity?): Double {
        val gunData = getGunData(getSeatIndex(entity))
        return gunData?.get(GunProp.DEFAULT_ZOOM) ?: 1.0
    }

    open fun canCrushEntities() = true

    open fun fixedEngine() {
        this.move(MoverType.SELF, Vec3(0.0, this.deltaMovement.y, 0.0))
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO)
        } else {
            this.setDeltaMovement(Vec3(0.0, this.deltaMovement.y, 0.0))
        }
    }

    open fun releaseSmokeDecoy(vec3: Vec3) = VehicleWeaponUtils.releaseSmokeDecoy(this, vec3)

    open fun releaseDecoy() = VehicleWeaponUtils.releaseDecoy(this)

    open fun terrainCompact(positions: MutableList<Vec3>) {
        VehicleMotionUtils.terrainCompact(this, positions)
    }

    open fun getWheelsTransform(partialTicks: Float): Matrix4d {
        return VehicleMotionUtils.getWheelsTransform(this, partialTicks)
    }

    open fun moveOnDragonTeeth() {
        VehicleMotionUtils.handleVehicleMoveOnDragonTeeth(this)
    }

    open fun collideBlocks() {
        if (tickCount % 4 != 0) return
        if (computed().engineType == EngineType.FIXED) return
        if (deltaMovement.lengthSqr() < 0.01) return
        VehicleMotionUtils.collideBlocks(this)
    }

    open val lastAttacker: Entity?
        get() = EntityFindUtil.findEntity(level(), lastAttackerUUID)

    private var sbwCacheOnGround = false

    fun vCollide(pVec: Vec3): Vec3 {
        if (ignoreEntityGroundCheckStepping) {
            sbwCacheOnGround = this.onGround()
            this.setOnGround(true)
        }

        val aabb = this.boundingBox

        val list = level().getEntityCollisions(this, aabb.expandTowards(pVec))
        val vec3 = if (pVec.lengthSqr() == 0.0) pVec else collideBoundingBox(this, pVec, aabb, this.level(), list)
        val flag = pVec.x != vec3.x
        val flag1 = pVec.y != vec3.y
        val flag2 = pVec.z != vec3.z
        val flag3 = this.onGround() || flag1 && pVec.y < 0.0
        val stepHeight = maxUpStep()

        if (stepHeight > 0.0f && flag3 && (flag || flag2)) {

            var vec31 = collideBoundingBox(
                this, Vec3(pVec.x, stepHeight.toDouble(), pVec.z), aabb,
                this.level(), list
            )
            val vec32 = collideBoundingBox(
                this, Vec3(0.0, stepHeight.toDouble(), 0.0), aabb.expandTowards(pVec.x, 0.0, pVec.z),
                this.level(), list
            )
            if (vec32.y < stepHeight.toDouble()) {
                val vec33 = collideBoundingBox(
                    this, Vec3(pVec.x, 0.0, pVec.z), aabb.move(vec32),
                    this.level(), list
                ).add(vec32)
                if (vec33.horizontalDistanceSqr() > vec31.horizontalDistanceSqr()) {
                    vec31 = vec33
                }
            }

            if (vec31.horizontalDistanceSqr() > vec3.horizontalDistanceSqr()) {
                return vec31.add(
                    collideBoundingBox(
                        this,
                        Vec3(0.0, -vec31.y + pVec.y, 0.0),
                        aabb.move(vec31),
                        this.level(),
                        list
                    )
                )
            }
        }

        if (ignoreEntityGroundCheckStepping) {
            this.setOnGround(sbwCacheOnGround)
            ignoreEntityGroundCheckStepping = false
        }

        return vec3
    }

    fun vMove(pType: MoverType, pPos: Vec3) {
        var pPos = pPos

        level().profiler.push("move")

        pPos = this.maybeBackOffFromEdge(pPos, pType)
        val vec3 = this.vCollide(pPos)
        val d0 = vec3.lengthSqr()
        if (d0 > 1.0E-7) {
            this.setPos(this.x + vec3.x, this.y + vec3.y, this.z + vec3.z)
        }

        level().profiler.pop()
        level().profiler.push("rest")
        val flag4 = !Mth.equal(pPos.x, vec3.x)
        val flag = !Mth.equal(pPos.z, vec3.z)
        this.horizontalCollision = flag4 || flag
        this.verticalCollision = pPos.y != vec3.y
        this.verticalCollisionBelow = this.verticalCollision && pPos.y < 0.0
        if (this.horizontalCollision) {
            this.minorHorizontalCollision = this.isHorizontalCollisionMinor(vec3)
        } else {
            this.minorHorizontalCollision = false
        }

        this.setOnGroundWithMovement(this.verticalCollisionBelow, vec3)
        val blockpos = this.getOnPos(0.2f)
        val blockstate = level().getBlockState(blockpos)
        if (this.isRemoved) {
            level().profiler.pop()
        } else {
            if (this.horizontalCollision) {
                val vec31 = this.deltaMovement
                this.setDeltaMovement(if (flag4) 0.0 else vec31.x, vec31.y, if (flag) 0.0 else vec31.z)
            }

            val block = blockstate.block
            if (pPos.y != vec3.y) {
                block.updateEntityAfterFallOn(this.level(), this)
            }

            if (this.onGround()) {
                block.stepOn(this.level(), blockpos, blockstate, this)
            }

            level().profiler.pop()
        }
    }

    override fun move(movementType: MoverType, movement: Vec3) {
        if (!this.level().isClientSide()) {
            ignoreEntityGroundCheckStepping = true
        }

        vMove(movementType, movement)

        if (lastTickSpeed < 0.2 || collisionCoolDown > 0 || this is DroneEntity) return
        val driver = this.lastDriver

        if (verticalCollision) {
            if (this.vehicleType == VehicleType.AIRPLANE
                && ((synchedGearRot > 0.15 && this !is Tom6Entity) || Mth.abs(this.roll) > 20 || Mth.abs(xRot) > 30)
            ) {
                this.hurt(
                    ModDamageTypes.causeVehicleStrikeDamage(
                        this.level().registryAccess(),
                        this,
                        driver ?: this
                    ),
                    if (isWreck) 0f else ((8 + Mth.abs(this.roll * 0.2f)) * (lastTickSpeed - 0.4) * (lastTickSpeed - 0.4)).toFloat()
                )
                this.bounceVertical(
                    Direction.getNearest(
                        this.deltaMovement.x(),
                        this.deltaMovement.y(),
                        this.deltaMovement.z()
                    ).opposite
                )
            } else if (this.vehicleType == VehicleType.HELICOPTER) {
                this.hurt(
                    ModDamageTypes.causeVehicleStrikeDamage(
                        this.level().registryAccess(),
                        this,
                        driver ?: this
                    ), if (isWreck) 0f else (10 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))).toFloat()
                )
                this.bounceVertical(
                    Direction.getNearest(
                        this.deltaMovement.x(),
                        this.deltaMovement.y(),
                        this.deltaMovement.z()
                    ).opposite
                )
            } else if (Mth.abs(lastTickVerticalSpeed.toFloat()) > 0.4) {
                this.hurt(
                    ModDamageTypes.causeVehicleStrikeDamage(
                        this.level().registryAccess(),
                        this,
                        driver ?: this
                    ),
                    if (isWreck) 0f else (24 * ((Mth.abs(lastTickVerticalSpeed.toFloat()) - 0.4) * (lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))).toFloat()
                )
                if (!this.level().isClientSide) {
                    this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.soundSource, 1f, 1f)
                }
                this.bounceVertical(
                    Direction.getNearest(
                        this.deltaMovement.x(),
                        this.deltaMovement.y(),
                        this.deltaMovement.z()
                    ).opposite
                )
            }
        }

        if (this.horizontalCollision) {
            this.hurt(
                ModDamageTypes.causeVehicleStrikeDamage(
                    this.level().registryAccess(),
                    this,
                    driver ?: this
                ), (18 * ((lastTickSpeed - 0.2) * (lastTickSpeed - 0.2))).toFloat()
            )
            this.bounceHorizontal(
                Direction.getNearest(
                    this.deltaMovement.x(),
                    this.deltaMovement.y(),
                    this.deltaMovement.z()
                ).opposite
            )
            if (!this.level().isClientSide) {
                this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.soundSource, 1f, 1f)
            }
            collisionCoolDown = 4
            crash = true
            power *= 0.8f
        }
    }

    open fun bounceHorizontal(direction: Direction) {
        VehicleMotionUtils.bounceHorizontal(this, direction)
    }

    open fun bounceVertical(direction: Direction) {
        VehicleMotionUtils.bounceVertical(this, direction)
    }

    open fun preventStacking() {
        VehicleMotionUtils.preventStacking(this)
    }

    open fun pushNew(pX: Double, pY: Double, pZ: Double) {
        this.setDeltaMovement(this.deltaMovement.add(pX, pY, pZ))
    }

    open fun supportEntities() {
        VehicleMotionUtils.supportEntities(this)
    }

    override fun getRandom(): RandomSource = this.random

    open fun crushEntities() = VehicleMotionUtils.crushEntities(this)

    open fun getForwardDirection(): Vector3f = Vector3f(
        Mth.sin(-yRot * (Math.PI.toFloat() / 180)),
        0.0f,
        Mth.cos(yRot * (Math.PI.toFloat() / 180))
    ).normalize()

    open fun getRightDirection(): Vector3f = Vector3f(
        Mth.cos(-yRot * (Math.PI.toFloat() / 180)),
        0.0f,
        Mth.sin(yRot * (Math.PI.toFloat() / 180))
    ).normalize()

    open fun getEngineSound(): SoundEvent? = this.computed().engineSound

    open fun getAcceleration() = absoluteSpeed - absoluteSpeedO

    open fun getTrackAnimationLength() = 100

    open fun hasDecoy() = computed().hasDecoy

    open fun engineRunning() = Math.abs(power) > 0

    /**
     * 撬棍shift+右键收回载具时返还的物品
     */
    open fun getRetrieveItems(): List<ItemStack> = listOf(ContainerBlockItem.createInstance(this))

    open val hudColor: Int
        get() = computed().hudColor.get()

    open var power by POWER
    open var deltaRot by DELTA_ROT
    open var decoyReady by DECOY_READY
    open var synchedPropellerRot by SYNCHED_PROPELLER_ROT
    open var planeBreak by PLANE_BREAK
    open var synchedGearRot by SYNCHED_GEAR_ROT
    open var gearUp by GEAR_UP

    open var subEngineDamaged by SUB_ENGINE_DAMAGED
    open var subEngineHealth by SUB_ENGINE_HEALTH
    open var mainEngineDamaged by MAIN_ENGINE_DAMAGED
    open var mainEngineHealth by MAIN_ENGINE_HEALTH

    open var leftWheelDamaged by L_WHEEL_DAMAGED
    open var leftWheelHealth by L_WHEEL_HEALTH
    open var rightWheelDamaged by R_WHEEL_DAMAGED
    open var rightWheelHealth by R_WHEEL_HEALTH

    open var turretDamaged by TURRET_DAMAGED
    open var turretHealth by TURRET_HEALTH

    open var selectedWeapon by SELECTED_WEAPON
    open var chargeProgress by CHARGE_PROGRESS

    open var laserScale by LASER_SCALE
    open var laserScaleO by LASER_SCALE_O
    open var laserLength by LASER_LENGTH

    open var serverYaw by SERVER_YAW
    open var serverPitch by SERVER_PITCH
    open var cannonRecoilTime by CANNON_RECOIL_TIME
    open var cannonRecoilForce by CANNON_RECOIL_FORCE

    open var override by OVERRIDE
    open var lastAttackerUUID by LAST_ATTACKER_UUID
    open var lastDriverUUID by LAST_DRIVER_UUID
    open var dogTagIcon by DOG_TAG_ICON
    open var aiTurretTargetUUID by AI_TURRET_TARGET_UUID
    open var aiPassengerWeaponTargetUUID by AI_PASSENGER_WEAPON_TARGET_UUID

    open var yawWhileShoot by YAW_WHILE_SHOOT
    open var hornVolume by HORN_VOLUME

    open var isWreck by IS_WRECK
    open var sympatheticDetonated by SYMPATHETIC_DETONATED
    open var turretBurned by TURRET_BURNED
    open var turretBurnTimer by TURRET_BURN_TIMER
    open var hoverMode by HOVER_MODE

    open val hornSound: SoundEvent
        get() = this.computed().hornSound

    // TODO 以更好的方式播放车载音乐，现在是读取副手的唱片

    //    @NotNull
    //    public SoundEvent getInCarMusicSound() {
    //        var passenger = this.getFirstPassenger();
    //        if (passenger instanceof Player player) {
    //            var stack = player.getOffhandItem();
    //
    //            var playableData = stack.get(DataComponents.JUKEBOX_PLAYABLE);
    //            if (playableData == null) return SoundEvents.EMPTY;
    //
    //            return playableData.song().unwrap(this.level().registryAccess())
    //                    .map(h -> h.value().soundEvent().value())
    //                    .orElse(SoundEvents.EMPTY);
    //        }
    //        return SoundEvents.EMPTY;
    //    }
    open fun horn() {
        hornVolume += 0.7f
    }

    open fun hornWorking() = Math.abs(this.hornVolume) > 0.05

    open fun stuka() = xRot > 5 && xRot < 175 && deltaMovement.y < -0.4 && !onGround()
    open fun heliCrash() = vehicleType == VehicleType.HELICOPTER && health < getMaxHealth() * 0.1f && !onGround()
    open fun vehicleSkip() =
        engineInfo is Wheel && engineInfo !is WheelChair && (if (engineInfo is Track) drift() else upInputDown) && onGround() && deltaMovement.horizontalDistanceSqr() > (if (engineInfo is Track) 0.0004 else 0.01)

    open fun drift() = upInputDown && (rightInputDown || leftInputDown)

    open val vehicleType: VehicleType?
        get() = computed().type

    /**
     * @author YWZJ Ranpoes
     */
    open fun support(entity: Entity) {
        VehicleMotionUtils.support(this, entity)
    }

    open val isAmphibious: Boolean
        get() = VehicleMiscUtils.isAmphibious(this)

    @OnlyIn(Dist.CLIENT)
    open fun firstPersonAmmoComponent(data: GunData, player: Player?): Component {
        val name = data.get(GunProp.NAME)
        if (name.isNullOrBlank()) return Component.empty()

        val ammoCount = this.getAmmoCount(player)
        return Component.translatable(name, if (ammoCount == Int.MAX_VALUE) "∞" else ammoCount)
    }

    @OnlyIn(Dist.CLIENT)
    open fun thirdPersonAmmoComponent(data: GunData, player: Player?): Component {
        return firstPersonAmmoComponent(data, player)
    }

    override fun getOBBs(): MutableList<OBB> {
        if (this.obbCache == null) {
            this.obbCache = this.obb.asSequence().map { it.getOBB() }.toMutableList()
        }
        return this.obbCache!!
    }

    open fun getEnergyDataAccessor() = ENERGY

    open fun generateWreckageLoot() {
        val data = WreckageLootDataManager.getLootData(this.type) ?: return
        val pools = data.pools
        if (pools.isEmpty()) return
        pools.forEach poolLoop@{ pool ->
            val type = pool.type
            if (type == WreckageLootData.Pool.Type.TURRET_ONLY) return@poolLoop
            val entries = pool.entries
            if (entries.isEmpty()) return@poolLoop
            val source = pool.source
            if (source != "@Default") {
                val lastSource = this.lastDamageSource ?: return@poolLoop
                val parsedLoc = ResourceLocation.tryParse(source) ?: return@poolLoop
                val damageType = ResourceKey.create(Registries.DAMAGE_TYPE, parsedLoc)
                if (!lastSource.`is`(damageType)) return@poolLoop
            } else if (this.lastDamageSource?.`is`(ModDamageTypes.REPAIR_TOOL) == true) {
                return@poolLoop
            }

            repeat(pool.rolls) {
                entries.forEach { entry ->
                    val random = Random.nextDouble()
                    val chance =
                        if (type == WreckageLootData.Pool.Type.VEHICLE_ONLY) {
                            if (this.hasTurret() && this.sympatheticDetonated) {
                                entry.chance
                            } else return@poolLoop
                        } else if (type == WreckageLootData.Pool.Type.COMPLETE) {
                            if (this.hasTurret()) {
                                if (this.sympatheticDetonated) return@poolLoop
                                else entry.chance
                            } else {
                                entry.chance
                            }
                        } else {
                            entry.chance * if (this.hasTurret() && this.sympatheticDetonated) (1.0 - VehicleConfig.TURRET_WRECKAGE_LOOT_RATE.get()) else 1.0
                        }

                    if (random > chance) return@forEach
                    val name = entry.name
                    val item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(name))
                    val count = entry.count
                    val entity = ItemEntity(level(), x, (y + 1), z, ItemStack(item, count))
                    entity.setPickUpDelay(10)
                    level().addFreshEntity(entity)
                }
            }
        }
    }

    companion object {
        const val TAG_SEAT_INDEX: String = "SBWSeatIndex"

        @JvmField
        val HEALTH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val OVERRIDE: EntityDataAccessor<String> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.STRING)

        @JvmField
        val LAST_ATTACKER_UUID: EntityDataAccessor<String> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.STRING)

        @JvmField
        val LAST_DRIVER_UUID: EntityDataAccessor<String> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.STRING)

        @JvmField
        val DOG_TAG_ICON: EntityDataAccessor<List<List<Short>>> =
            SynchedEntityData.defineId(VehicleEntity::class.java, ModSerializers.SHORT_LIST_LIST_SERIALIZER.get())

        @JvmField
        val AI_TURRET_TARGET_UUID: EntityDataAccessor<String> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.STRING)

        @JvmField
        val AI_PASSENGER_WEAPON_TARGET_UUID: EntityDataAccessor<String> = SynchedEntityData.defineId(
            VehicleEntity::class.java, EntityDataSerializers.STRING
        )

        @JvmField
        val DELTA_ROT: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val MOUSE_SPEED_X: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val MOUSE_SPEED_Y: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val SELECTED_WEAPON: EntityDataAccessor<List<Int>> = SynchedEntityData.defineId(
            VehicleEntity::class.java, ModSerializers.INT_LIST_SERIALIZER.get()
        )

        @JvmField
        val TURRET_HEALTH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val L_WHEEL_HEALTH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val R_WHEEL_HEALTH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val MAIN_ENGINE_HEALTH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val SUB_ENGINE_HEALTH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val TURRET_DAMAGED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val L_WHEEL_DAMAGED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val R_WHEEL_DAMAGED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val MAIN_ENGINE_DAMAGED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val SUB_ENGINE_DAMAGED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val HORN_VOLUME: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        var playTrackSound: Consumer<VehicleEntity?> = Consumer { }

        @JvmField
        var playEngineSound: Consumer<VehicleEntity?> = Consumer { }

        @JvmField
        var playSwimSound: Consumer<VehicleEntity?> = Consumer { }

        @JvmField
        var playHornSound: Consumer<VehicleEntity?> = Consumer { }

        @JvmField
        var playStukaSound: Consumer<VehicleEntity?> = Consumer { }

        @JvmField
        var playHeliCrashSound: Consumer<VehicleEntity?> = Consumer { }

        @JvmField
        var playVehicleSkipSound: Consumer<VehicleEntity?> = Consumer { }

        @JvmField
        var playFireSound: Consumer<VehicleEntity>? = Consumer { }

        @JvmField
        var ignoreEntityGroundCheckStepping = false

        @JvmField
        val SERVER_YAW: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val SERVER_PITCH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val CANNON_RECOIL_TIME: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.INT)

        @JvmField
        val CANNON_RECOIL_FORCE: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val POWER: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val YAW_WHILE_SHOOT: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val DECOY_READY: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val SYNCHED_PROPELLER_ROT: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val SYNCHED_GEAR_ROT: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val GEAR_UP: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val FORWARD_INPUT_DOWN: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val BACK_INPUT_DOWN: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val LEFT_INPUT_DOWN: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val RIGHT_INPUT_DOWN: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val UP_INPUT_DOWN: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val DOWN_INPUT_DOWN: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val DECOY_INPUT_DOWN: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val FIRE_INPUT_DOWN: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val SPRINT_INPUT_DOWN: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val PLANE_BREAK: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val ENERGY: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.INT)

        @JvmField
        val LASER_LENGTH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val LASER_SCALE: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val LASER_SCALE_O: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val CHARGE_PROGRESS: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val IS_WRECK: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val SYMPATHETIC_DETONATED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val TURRET_BURNED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val TURRET_BURN_TIMER: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.INT)

        @JvmField
        val HOVER_MODE: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(VehicleEntity::class.java, EntityDataSerializers.BOOLEAN)

        // Map SeatIndex -> GunData
        protected val GUN_DATA_MAP: EntityDataAccessor<Map<String, GunData>> =
            SynchedEntityData.defineId(VehicleEntity::class.java, ModSerializers.VEHICLE_GUN_DATA_MAP_SERIALIZER.get())
    }
}
