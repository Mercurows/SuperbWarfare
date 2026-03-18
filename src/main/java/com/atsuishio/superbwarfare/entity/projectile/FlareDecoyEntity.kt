package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModEntities
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

open class FlareDecoyEntity : Entity {
    constructor(type: EntityType<out FlareDecoyEntity>, world: Level) : super(type, world)

    constructor(level: Level) : super(ModEntities.FLARE_DECOY.get(), level)

    override fun readAdditionalSaveData(compoundTag: CompoundTag) {
    }

    override fun addAdditionalSaveData(compoundTag: CompoundTag) {
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
    }

    override fun tick() {
        super.tick()
        this.deltaMovement = this.deltaMovement.add(0.0, -0.02, 0.0)
        this.move(MoverType.SELF, this.deltaMovement)

        if (level().isClientSide()) {
            level().addAlwaysVisibleParticle(ParticleTypes.END_ROD, true, this.xo, this.yo, this.zo, 0.0, 0.0, 0.0)
            level().addAlwaysVisibleParticle(ParticleTypes.CLOUD, true, this.xo, this.yo, this.zo, 0.0, 0.0, 0.0)
        }
        if (this.tickCount > 200 || this.isInWater || this.onGround()) {
            this.discard()
        }
    }

    fun decoyShoot(entity: Entity, shootVec: Vec3, pVelocity: Float, pInaccuracy: Float) {
        val vec3 = shootVec.normalize().add(
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble()),
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble()),
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble())
        ).scale(pVelocity.toDouble())
        this.deltaMovement = entity.deltaMovement.scale(0.75).add(vec3)
        val d0 = vec3.horizontalDistance()
        this.yRot = (Mth.atan2(vec3.x, vec3.z) * 57.2957763671875).toFloat()
        this.xRot = (Mth.atan2(vec3.y, d0) * 57.2957763671875).toFloat()
        this.yRotO = this.yRot
        this.xRotO = this.xRot
    }
}
