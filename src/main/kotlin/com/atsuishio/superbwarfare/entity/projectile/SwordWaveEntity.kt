package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.entity.OBBEntity
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.mixin.OBBHitter
import com.atsuishio.superbwarfare.entity.projectile.IAdvancedHitDetection.Companion.rayTraceBlocks
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.tools.HitboxHelper
import com.atsuishio.superbwarfare.tools.OBB
import com.atsuishio.superbwarfare.tools.forceApplyEffect
import com.atsuishio.superbwarfare.world.phys.EntityResult
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.roundToInt

open class SwordWaveEntity(type: EntityType<out SwordWaveEntity>, level: Level) : FastThrowableProjectile(type, level) {
    open var roll by ROLL
    open var rgb by RGB
    var launchOwnerId = -1
        private set

    init {
        lifeValue = 20
        velocityValue = LAUNCH_SPEED
        gravityValue = 0f
    }

    override fun defineSynchedData() {
        super.defineSynchedData()
        entityData.define(ROLL, 0f)
        entityData.define(RGB, 0xE6F2FF)
    }

    override fun writeSpawnData(buffer: FriendlyByteBuf) {
        super.writeSpawnData(buffer)
        buffer.writeVarInt(owner?.id ?: -1)
        buffer.writeVarInt(lifeValue)
        buffer.writeFloat(velocityValue)
    }

    override fun readSpawnData(additionalData: FriendlyByteBuf) {
        super.readSpawnData(additionalData)
        launchOwnerId = additionalData.readVarInt()
        lifeValue = additionalData.readVarInt()
        velocityValue = additionalData.readFloat()
    }

    override fun getDefaultItem(): Item = Items.WOODEN_SWORD

    override fun canPassThroughFluid() = true

    override fun isFastMoving() = false

    override fun getRGB() = floatArrayOf(
        ((rgb ushr 16) and 255) / 255f,
        ((rgb ushr 8) and 255) / 255f,
        (rgb and 255) / 255f,
    )

    override fun setRGB(rgb: FloatArray) {
        if (rgb.size < 3 || !rgb[0].isFinite() || !rgb[1].isFinite() || !rgb[2].isFinite()) return
        fun channel(index: Int) = (rgb[index].coerceIn(0f, 1f) * 255).roundToInt()
        this.rgb = (channel(0) shl 16) or (channel(1) shl 8) or channel(2)
    }

    override fun getHitResult(entity: Entity, startVec: Vec3, endVec: Vec3): EntityResult? {
        super.getHitResult(entity, startVec, endVec)?.let { return it }

        val axisHit = if (entity is OBBEntity && !entity.enableAABB()) {
            val (hit, part) = entity.getOBBs().asSequence()
                .filter { it.part != OBB.Part.COLLISION }
                .mapNotNull { box ->
                    val intersection = LaserBeamEntity.intersectBeam(box, startVec, endVec, HIT_RADIUS)
                    intersection?.takeIf(::canReachContact)?.let { it to box.part }
                }
                .minByOrNull { startVec.distanceToSqr(it.first.axis) } ?: return null
            OBBHitter.getInstance(this).`sbw$setCurrentHitPart`(part)
            hit.axis
        } else {
            val bounds = HitboxHelper.getCompensatedBoundingBox(entity, owner)
            val hit = LaserBeamEntity.intersectBeam(bounds, startVec, endVec, HIT_RADIUS) ?: return null
            if (!canReachContact(hit)) return null
            hit.axis
        }

        // Glancing contact widens the body hit only; direct hits keep the inherited hit zones.
        return EntityResult(entity, axisHit, headshot = false, legShot = false)
    }

    private fun canReachContact(hit: LaserBeamEntity.BeamIntersection): Boolean {
        if (hit.axis.distanceToSqr(hit.contact) < 1e-10) return true
        val ray = ClipContext(hit.axis, hit.contact, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)
        return rayTraceBlocks(level(), ray) { false }.type == HitResult.Type.MISS
    }

    override fun shoot(x: Double, y: Double, z: Double, velocity: Float, inaccuracy: Float) {
        super.shoot(x, y, z, velocity, inaccuracy)
        velocityValue = velocity
        deltaMovement = deltaMovement.scale(speedFraction(0))
    }

    override fun tick() {
        super.tick()
        if (!level().isClientSide && tickCount >= lifeValue) discard()
    }

    override fun projectileMove(level: Level) {
        if (isRemoved) return
        val movement = deltaMovement
        setPos(x + movement.x, y + movement.y, z + movement.z)
        if (level.isClientSide) return

        val speed = movement.length()
        if (speed > 0.0) {
            deltaMovement = movement.scale(velocityValue * speedFraction(tickCount) / speed)
        }
    }

    // The midpoint speed is the average over a tick, so total travel is launch speed times half the lifetime.
    private fun speedFraction(elapsedTicks: Int): Double =
        (1.0 - (elapsedTicks + 0.5) / lifeValue.coerceAtLeast(1)).coerceAtLeast(0.0)

    override fun onHitEntity(result: EntityHitResult) {
        val speed = deltaMovement.length()
        val stepProgress = if (speed > 0.0) (position().distanceTo(result.location) / speed).coerceIn(0.0, 1.0) else 0.0
        val progress = ((tickCount - 1 + stepProgress) / lifeValue.coerceAtLeast(1)).coerceIn(0.0, 1.0)
        val launchDamage = damageValue
        damageValue = (launchDamage * (1.0 - MAX_DAMAGE_FALLOFF * progress)).toFloat()
        try {
            super.onHitEntity(result)
        } finally {
            damageValue = launchDamage
        }
    }

    override fun performAddEffect(entity: Entity, damage: Float, isHeadshot: Boolean) {
        if (entity !is LivingEntity || entity.level().isClientSide) return
        for (effect in getEffects()) {
            entity.forceApplyEffect(MobEffectInstance(effect), owner)
        }
    }

    companion object {
        private const val LAUNCH_SPEED = 1.0f
        private const val MAX_DAMAGE_FALLOFF = 0.8
        private const val HIT_RADIUS = 0.35

        @JvmField
        val ROLL: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(SwordWaveEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val RGB: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(SwordWaveEntity::class.java, EntityDataSerializers.INT)
    }
}
