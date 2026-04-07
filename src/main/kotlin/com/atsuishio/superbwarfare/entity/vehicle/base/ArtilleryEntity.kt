package com.atsuishio.superbwarfare.entity.vehicle.base

import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.Plz05Entity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getXRotFromVector
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSerializers
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.item.misc.ArtilleryIndicatorItem
import com.atsuishio.superbwarfare.item.misc.firingParameters
import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import com.atsuishio.superbwarfare.tools.InventoryTool
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.TrajectoryCalculator.calculateLaunchVector
import com.atsuishio.superbwarfare.tools.randomPos
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Math
import org.joml.Vector3f
import java.util.*

open class ArtilleryEntity(type: EntityType<*>, world: Level) : GeoVehicleEntity(type, world) {

    open var barrelAnim by BARREL_ANIM
    open var shootVec by SHOOT_VEC
    open var depressed by DEPRESSED
    open var targetPos by TARGET_POS
    open var radius by RADIUS
    open var lockTurret by LOCK_TURRET

    init {
        barrelAnim = List(Math.max(4, this.maxBarrel)) { 0 }
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        val gunData = getGunData("Main") ?: return InteractionResult.SUCCESS

        val stack = player.mainHandItem
        val item = stack.item

        if (this.canBind() && item is ArtilleryIndicatorItem && !isWreck) {
            if (player.rootVehicle === this) return InteractionResult.FAIL
            return item.bind(stack, player, this)
        }

        if (stack.`is`(ModTags.Items.TOOLS_CROWBAR) && !player.isShiftKeyDown && !isWreck) {
            if (gunData.ammo.get() > 0 && player.level() is ServerLevel) {
                vehicleShoot(player, "Main")
            }
            return InteractionResult.SUCCESS
        }

        if (player.mainHandItem.item === ModItems.FIRING_PARAMETERS.get() && player.isShiftKeyDown) {
            setTarget(player.mainHandItem, player, "Main")
            return InteractionResult.SUCCESS
        }

        if (player.offhandItem.item === ModItems.FIRING_PARAMETERS.get() && player.isShiftKeyDown) {
            setTarget(player.offhandItem, player, "Main")
            return InteractionResult.SUCCESS
        }

        return super.interact(player, hand)
    }

    override fun onAddedToLevel() {
        super.onAddedToLevel()
        shootVec = forward.toVector3f()
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)

        with(builder) {
            define(SHOOT_VEC, forward.toVector3f())
            define(DEPRESSED, false)
            define(TARGET_POS, BlockPos(0, 0, 0))
            define(RADIUS, 0)
            define(BARREL_ANIM, List(4) { 0 })
            define(LOCK_TURRET, false)
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putFloat("ShootVecX", shootVec.x)
        compound.putFloat("ShootVecY", shootVec.y)
        compound.putFloat("ShootVecZ", shootVec.z)

        compound.putBoolean("Depressed", depressed)
        compound.putInt("Radius", radius)
        compound.putInt("TargetX", targetPos.x)
        compound.putInt("TargetY", targetPos.y)
        compound.putInt("TargetZ", targetPos.z)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("ShootVecX") && compound.contains("ShootVecY") && compound.contains("ShootVecZ")) {
            shootVec =
                Vector3f(compound.getFloat("ShootVecX"), compound.getFloat("ShootVecY"), compound.getFloat("ShootVecZ"))
        }
        if (compound.contains("Depressed")) {
            depressed = compound.getBoolean("Depressed")
        }
        if (compound.contains("Radius")) {
            radius = compound.getInt("Radius")
        }
        if (compound.contains("TargetX") && compound.contains("TargetY") && compound.contains("TargetZ")) {
            targetPos = BlockPos(compound.getInt("TargetX"), compound.getInt("TargetX"), compound.getInt("TargetZ"))
        }
    }

    open fun setTarget(stack: ItemStack, entity: Entity?, weaponName: String) {
        if (this.isWreck) return
        val parameters = stack.firingParameters
        var canAim = true

        targetPos = parameters.pos
        depressed = !parameters.isDepressed
        radius = parameters.radius
        val distance = targetPos.center.distanceTo(getShootPos(weaponName, 1f))
        val randomPos = targetPos.center.randomPos(radius).add(0.0, -1.0 - 0.0015 * distance, 0.0)
        val launchVector = calculateLaunchVector(
            getShootPos(weaponName, 1f),
            randomPos,
            getProjectileVelocity(weaponName).toDouble(),
            getProjectileGravity(weaponName).toDouble(),
            depressed
        )
        val launchVector2 = calculateLaunchVector(
            getShootPos(weaponName, 1f),
            randomPos,
            getProjectileVelocity(weaponName).toDouble(),
            getProjectileGravity(weaponName).toDouble(),
            !depressed
        )

        var component = Component.literal("")
        val location = Component.translatable("tips.superbwarfare.mortar.position", this.displayName)
            .append(Component.literal(" X:" + format0D(x) + " Y:" + format0D(y) + " Z:" + format0D(z) + " "))

        if (launchVector == null) {
            canAim = false
            component = Component.translatable("tips.superbwarfare.mortar.out_of_range")
        } else {
            val angle = -getXRotFromVector(launchVector).toFloat()
            val angle2 = -launchVector2?.let { getXRotFromVector(it).toFloat() }!!
            if (angle < -turretMaxPitch || angle > -turretMinPitch) {
                if (angle2 > -turretMaxPitch && angle2 < -turretMinPitch) {
                    component = Component.translatable("tips.superbwarfare.ballistics.warn2")
                    canAim = false
                } else {
                    component = Component.translatable("tips.superbwarfare.mortar.warn", this.displayName)
                    if (entity is Player) {
                        entity.displayClientMessage(
                            location.copy().append(component).withStyle(ChatFormatting.RED),
                            false
                        )
                    }
                    return
                }
            }

            if (angle < -turretMaxPitch) {
                component = Component.translatable("tips.superbwarfare.ballistics.warn")
                canAim = false
            }
        }

        if (canAim) {
            lockTurret = false
            launchVector?.toVector3f()?.let { shootVec = it }
        } else if (entity is Player) {
            entity.displayClientMessage(location.copy().append(component).withStyle(ChatFormatting.RED), false)
        }
    }

    open fun resetTarget(weaponName: String) {
        if (this.isWreck) return
        val distance = targetPos.center.distanceTo(getShootPos(weaponName, 1f))
        val randomPos = targetPos.center.randomPos(radius).add(0.0, -1.0 - 0.0015 * distance, 0.0)
        val launchVector = calculateLaunchVector(
            getShootPos(weaponName, 1f),
            randomPos,
            getProjectileVelocity(weaponName).toDouble(),
            getProjectileGravity(weaponName).toDouble(),
            depressed
        ) ?: return

        val angle = -getXRotFromVector(launchVector).toFloat()
        if (angle > -turretMaxPitch && angle < -turretMinPitch) {
            shootVec = launchVector.toVector3f()
        }
    }

    open val maxBarrel: Int
        get() = getGunData("Main")?.get(GunProp.MAGAZINE) ?: 1

    override fun baseTick() {
        super.baseTick()

        if (this.isWreck) return
        for (i in 0..<this.maxBarrel) {
            val animCounters = barrelAnim.toMutableList()
            if (i < animCounters.size && animCounters[i] > 0) {
                animCounters[i] = animCounters[i] - 1
                barrelAnim = animCounters.toList()
            }
        }

        // TODO 替换装弹逻辑？
        val gunData = getGunData("Main")
        val controller = getNthEntity(turretControllerIndex)
        if (gunData != null && level() is ServerLevel && controller is Player) {
            val ammoCount = InventoryTool.countItem(controller, gunData.selectedAmmoConsumer().stack().item)
            if (ammoCount > 0) {
                val inStack = this.getItems().first()
                val count = inStack.count

                if (count < Math.min(this.maxStackSize, inStack.maxStackSize)) {
                    this.setItem(0, gunData.selectedAmmoConsumer().stack().copyWithCount(count + 1))
                    InventoryTool.consumeItem(controller, gunData.selectedAmmoConsumer().stack().item, 1)
                }
            }
        }

        if (deltaMovement.horizontalDistanceSqr() > 0.007 && this !is Plz05Entity) {
            lockTurret = true
        }

        if (controller != null) {
            shootVec = controller.getViewVector(1f).toVector3f()
        } else if (!lockTurret) {
            turretAutoAimFromVector(Vec3(shootVec))
        }
    }

    override fun vehicleShoot(living: LivingEntity?, weaponName: String) {
        beforeShoot(living)
        super.vehicleShoot(living, weaponName)
    }

    override fun vehicleShoot(living: LivingEntity?, uuid: UUID?, targetPos: Vec3?) {
        beforeShoot(living)
        super.vehicleShoot(living, uuid, targetPos)
    }

    open fun beforeShoot(living: LivingEntity?) {
        val data = getGunData("Main")
        if (data != null && data.ammo.get() > 0) {
            val animCounters = barrelAnim.toMutableList()
            animCounters[data.ammo.get() - 1] = data.get(GunProp.SHOOT_ANIMATION_TIME)
            barrelAnim = animCounters.toList()
        }

        val level = living?.level()
        if (level is ServerLevel) {
            ParticleTool.spawnBigCannonMuzzleParticles(getShootVec("Main", 1f), getShootPos("Main", 1f), level, this)
        }
    }

    open fun canBind() = false

    companion object {
        @JvmField
        val BARREL_ANIM: EntityDataAccessor<List<Int>> =
            SynchedEntityData.defineId(ArtilleryEntity::class.java, ModSerializers.INT_LIST_SERIALIZER.get())

        @JvmField
        val SHOOT_VEC: EntityDataAccessor<Vector3f> =
            SynchedEntityData.defineId(ArtilleryEntity::class.java, EntityDataSerializers.VECTOR3)

        @JvmField
        val DEPRESSED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(ArtilleryEntity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val TARGET_POS: EntityDataAccessor<BlockPos> =
            SynchedEntityData.defineId(ArtilleryEntity::class.java, EntityDataSerializers.BLOCK_POS)

        @JvmField
        val RADIUS: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(ArtilleryEntity::class.java, EntityDataSerializers.INT)

        @JvmField
        val LOCK_TURRET: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(ArtilleryEntity::class.java, EntityDataSerializers.BOOLEAN)
    }
}
