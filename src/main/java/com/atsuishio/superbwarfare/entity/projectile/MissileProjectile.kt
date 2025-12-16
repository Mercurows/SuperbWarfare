package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

abstract class MissileProjectile : DestroyableProjectile, CustomSyncMotionEntity {
    @JvmField
    var targetPos: Vec3? = null

    @JvmField
    var guideType: Int = 0

    @JvmField
    var distracted: Boolean = false

    @JvmField
    var lost: Boolean = false

    @JvmField
    var lostTarget: Boolean = false

    var targetUUID by TARGET_UUID

    constructor(pEntityType: EntityType<out ThrowableItemProjectile?>, pLevel: Level) : super(pEntityType, pLevel)

    constructor(pEntityType: EntityType<out ThrowableItemProjectile?>, pShooter: Entity?, pLevel: Level) : super(
        pEntityType,
        pLevel
    ) {
        this.owner = pShooter
        if (pShooter != null) {
            this.setPos(pShooter.x, pShooter.eyeY - 0.1, pShooter.z)
        }
    }

    fun setGuideType(guideType: Int) {
        this.guideType = guideType
    }

    fun setTargetVec(targetPos: Vec3?) {
        if (targetPos != null) {
            this.targetPos = targetPos
        }
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(TARGET_UUID, "none")
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("TargetUuid")) {
            targetUUID = compound.getString("TargetUuid")
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putString("TargetUuid", targetUUID)
    }

    override fun updateRotation() {}

    fun turn(vec3: Vec3, turnSpeed: Float) {
        var vec3 = vec3
        val v0 = deltaMovement.normalize()

        vec3 = vec3.add(v0.scale(-0.4))

        val d0 = vec3.horizontalDistance()
        val targetAngleY = (-Mth.atan2(vec3.x, vec3.z) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
        val targetAngleX = (-Mth.atan2(vec3.y, d0) * (180f / Math.PI.toFloat()).toDouble()).toFloat()

        val diffY = Mth.wrapDegrees(targetAngleY - this.yRot)
        val diffX = Mth.wrapDegrees(targetAngleX - this.xRot)

        this.yRot += Mth.clamp(0.95f * diffY, -turnSpeed, turnSpeed)
        this.xRot += Mth.clamp(0.95f * diffX, -turnSpeed, turnSpeed)
    }

    override fun shoot(pX: Double, pY: Double, pZ: Double, pVelocity: Float, pInaccuracy: Float) {
        val vec3 = (Vec3(pX, pY, pZ)).normalize().add(
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble()),
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble()),
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble())
        ).scale(pVelocity.toDouble())
        this.deltaMovement = vec3
        val d0 = vec3.horizontalDistance()
        this.yRot = (-Mth.atan2(vec3.x, vec3.z) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
        this.xRot = (-Mth.atan2(vec3.y, d0) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
        this.yRotO = this.yRot
        this.xRotO = this.xRot
    }

    override fun shouldSyncMotion() = true

    override fun forceLoadChunk() = true

    override fun isNoGravity() = true

    override fun getDefaultGravity() = 0.0

    companion object {
        @JvmField
        val TARGET_UUID: EntityDataAccessor<String> =
            SynchedEntityData.defineId(MissileProjectile::class.java, EntityDataSerializers.STRING)
    }
}
