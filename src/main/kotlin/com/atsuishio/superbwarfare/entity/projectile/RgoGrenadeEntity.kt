package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.customExplode
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BellBlock
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

open class RgoGrenadeEntity : FastThrowableProjectile, BasicGeoProjectileEntity {
    constructor(type: EntityType<out RgoGrenadeEntity>, level: Level) : super(type, level) {
        this.noCulling = true
        this.explosionDamageValue = ExplosionConfig.RGO_GRENADE_EXPLOSION_DAMAGE.get().toFloat()
        this.explosionRadiusValue = ExplosionConfig.RGO_GRENADE_EXPLOSION_RADIUS.get().toFloat()
    }

    constructor(type: EntityType<out RgoGrenadeEntity>, x: Double, y: Double, z: Double, level: Level) : super(
        type,
        x,
        y,
        z,
        level
    ) {
        this.noCulling = true
        this.explosionDamageValue = ExplosionConfig.RGO_GRENADE_EXPLOSION_DAMAGE.get().toFloat()
        this.explosionRadiusValue = ExplosionConfig.RGO_GRENADE_EXPLOSION_RADIUS.get().toFloat()
    }

    constructor(entity: LivingEntity?, level: Level) : super(ModEntities.RGO_GRENADE.get(), entity, level) {
        this.noCulling = true
        this.explosionDamageValue = ExplosionConfig.RGO_GRENADE_EXPLOSION_DAMAGE.get().toFloat()
        this.explosionRadiusValue = ExplosionConfig.RGO_GRENADE_EXPLOSION_RADIUS.get().toFloat()
    }

    constructor(entity: LivingEntity?, level: Level, life: Int) : this(entity, level) {
        this.lifeValue = life
    }

    override fun getDefaultItem(): Item {
        return ModItems.RGO_GRENADE.get()
    }

    override fun onHit(result: HitResult) {
        if (level() is ServerLevel) {
            when (result.type) {
                HitResult.Type.BLOCK -> {
                    val blockResult = result as BlockHitResult
                    val resultPos = blockResult.blockPos
                    val state = this.level().getBlockState(resultPos)
                    val block = state.block
                    if (block is BellBlock) {
                        block.attemptToRing(this.level(), resultPos, blockResult.direction)
                    }
                    this.customExplode(this.explosionDamageValue, this.explosionRadiusValue, 1.2f)
                }

                HitResult.Type.ENTITY -> {
                    val entityResult = result as EntityHitResult
                    val entity = entityResult.entity
                    if (this.owner != null && this.owner!!.vehicle != null && entity === this.owner!!.vehicle) return
                    if (entity !is DroneEntity) {
                        this.customExplode(this.explosionDamageValue, this.explosionRadiusValue, 1.2f)
                    }
                }

                else -> {}
            }
        }
    }

    override fun tick() {
        super.tick()
        val level = this.level() as? ServerLevel ?: return

        ParticleTool.sendParticle(
            level, ParticleTypes.SMOKE, this.xo, this.yo, this.zo,
            1, 0.0, 0.0, 0.0, 0.01, true
        )

        if (isInFluidType) {
            deltaMovement = deltaMovement.scale(0.75)
        }
    }

    override fun getModel() = BedrockModelLoader.RGO_GRENADE_MODEL
}
