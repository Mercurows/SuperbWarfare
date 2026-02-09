package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import org.joml.Quaterniond
import org.joml.Quaternionf
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.stream.StreamSupport


open class TurretWreckEntity(type: EntityType<TurretWreckEntity>, world: Level) : Entity(type, world), GeoEntity {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    companion object {
        // 是否已装填弹药
        @JvmField
        val QUATERNIOND_X: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(TurretWreckEntity::class.java, EntityDataSerializers.FLOAT)
        val QUATERNIOND_Y: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(TurretWreckEntity::class.java, EntityDataSerializers.FLOAT)
        val QUATERNIOND_Z: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(TurretWreckEntity::class.java, EntityDataSerializers.FLOAT)
        val QUATERNIOND_W: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(TurretWreckEntity::class.java, EntityDataSerializers.FLOAT)
        val BARREL_PITCH: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(TurretWreckEntity::class.java, EntityDataSerializers.FLOAT)
        val VEHICLE_NAME: EntityDataAccessor<String> =
            SynchedEntityData.defineId(TurretWreckEntity::class.java, EntityDataSerializers.STRING)
    }

    open var QuaternionX by QUATERNIOND_X
    open var QuaternionY by QUATERNIOND_Y
    open var QuaternionZ by QUATERNIOND_Z
    open var QuaternionW by QUATERNIOND_W
    open var BarrelPitch by BARREL_PITCH
    open var VehicleName by VEHICLE_NAME

    open var qxO = 0f
    open var qyO = 0f
    open var qzO = 0f
    open var qwO = 1f

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        with(builder) {
            define(QUATERNIOND_X, 0f)
            define(QUATERNIOND_Y, 0f)
            define(QUATERNIOND_Z, 0f)
            define(QUATERNIOND_W, 1f)
            define(BARREL_PITCH, 0f)
            define(VEHICLE_NAME, "GunMu")
        }
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        entityData.set(QUATERNIOND_X, compound.getFloat("Qx"))
        entityData.set(QUATERNIOND_Y, compound.getFloat("Qy"))
        entityData.set(QUATERNIOND_Z, compound.getFloat("Qz"))
        entityData.set(QUATERNIOND_W, compound.getFloat("Qw"))
        entityData.set(BARREL_PITCH, compound.getFloat("BarrelPitch"))
        entityData.set(VEHICLE_NAME, compound.getString("VehicleName"))
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        compound.putFloat("Qx", entityData.get(QUATERNIOND_X))
        compound.putFloat("Qy", entityData.get(QUATERNIOND_Y))
        compound.putFloat("Qz", entityData.get(QUATERNIOND_Z))
        compound.putFloat("Qw", entityData.get(QUATERNIOND_W))
        compound.putFloat("BarrelPitch", entityData.get(BARREL_PITCH))
        compound.putString("VehicleName", entityData.get(VEHICLE_NAME))
    }

    fun getPlayer(level: Level?): List<Entity> {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).all.spliterator(), false)
            .filter { e: Entity -> e is Player }
            .toList()
    }

    override fun baseTick() {
        qxO = QuaternionX
        qyO = QuaternionY
        qzO = QuaternionZ
        qwO = QuaternionW
        super.baseTick()


        this.move(MoverType.SELF, this.deltaMovement)
        var f = 0.98f
        if (this.onGround()) {
            val pos = this.blockPosBelowThatAffectsMyMovement
            f = level().getBlockState(pos).getFriction(this.level(), pos, this) * 0.98f

            val targetRotation = Quaternionf().rotationXYZ(0f, -yRot * Mth.DEG_TO_RAD, 0f)
            val lerpFactor = 0.5f
            this.lerpRotationToTarget(targetRotation, lerpFactor)
        } else {
            setQuaternion(Quaterniond(getQuaternion(1f).rotateX(0.02f * deltaMovement.y.toFloat())))
        }
//
//        for (player in getPlayer(level())) {
//            if (player is Player) {
//                player.displayClientMessage(Component.literal(getEulerAngles(getQuaternion(1f)).z.toString()), true)
//            }
//
//        }

//        for (player in getPlayer(level())) {
//            val vehicle = player.vehicle
//            if (vehicle is Lav150Entity) {
//                val quaterniond = VectorTool.combineRotationsTurret(1f, vehicle)
//                setQuaternion(quaterniond)
//            }
//        }

        this.deltaMovement = deltaMovement.multiply(f.toDouble(), 0.98, f.toDouble()).add(0.0, -0.04, 0.0)
    }

    private fun lerpRotationToTarget(targetRotation: Quaternionf, lerpFactor: Float) {
        val currentRotation: Quaternionf = this.getQuaternion(1f)
        currentRotation.slerp(targetRotation, lerpFactor)
        this.setQuaternion(Quaterniond(currentRotation))
    }

    open fun setQuaternion0(quaterniond: Quaterniond) {
        qxO = quaterniond.x.toFloat()
        qyO = quaterniond.y.toFloat()
        qzO = quaterniond.z.toFloat()
        qwO = quaterniond.w.toFloat()
    }

    open fun setQuaternion(quaterniond: Quaterniond) {
        QuaternionX = quaterniond.x.toFloat()
        QuaternionY = quaterniond.y.toFloat()
        QuaternionZ = quaterniond.z.toFloat()
        QuaternionW = quaterniond.w.toFloat()
    }

    open fun getQuaternion(tickDelta: Float) = Quaternionf(
        Mth.lerp(tickDelta, qxO, QuaternionX),
        Mth.lerp(tickDelta, qyO, QuaternionY),
        Mth.lerp(tickDelta, qzO, QuaternionZ),
        Mth.lerp(tickDelta, qwO, QuaternionW)
    )

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) {}
    override fun getAnimatableInstanceCache(): AnimatableInstanceCache = this.cache
}
