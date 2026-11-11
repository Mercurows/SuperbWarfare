package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.ProjectileConfig
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.tools.forceHurt
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.AreaEffectCloud
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseFireBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil

open class MolotovEntity(type: EntityType<out MolotovEntity>, level: Level) :
    FastThrowableProjectile(type, level), BasicGeoProjectileEntity {
    init {
        damageValue = 5f
    }

    override fun getDefaultItem() = ModItems.MOLOTOV.get()

    override fun isFastMoving() = false

    override fun onHitEntity(result: EntityHitResult) {
        if (level().isClientSide || isRemoved || result.entity === owner) return
        result.entity.forceHurt(
            ModDamageTypes.causeProjectileHitDamage(level().registryAccess(), this, owner), damageValue
        )
        shatter(result.location)
    }

    override fun afterHitBlock(result: BlockHitResult) {
        val normal = Vec3.atLowerCornerOf(result.direction.normal)
        shatter(result.location.add(normal.scale(0.05)), level().getFluidState(result.blockPos).isEmpty)
    }

    override fun projectileMove(level: Level) {
        if (!isRemoved) super.projectileMove(level)
    }

    private fun shatter(location: Vec3, ignite: Boolean = true) {
        val level = level() as? ServerLevel ?: return
        if (isRemoved) return
        level.playSound(null, location.x, location.y, location.z, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1f, 1f)
        level.sendParticles(
            BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()),
            location.x, location.y, location.z, 24, 0.2, 0.2, 0.2, 0.15
        )
        // Water extinguishes the contents, but the bottle still breaks on impact.
        if (ignite && level.getFluidState(BlockPos.containing(location)).isEmpty) {
            val radius = ProjectileConfig.MOLOTOV_FIRE_RADIUS.get()
            if (radius > 0.0) {
                spreadFire(level, location, radius)
                createBurnCloud(level, location, radius.toFloat())
            }
        }
        discard()
    }

    private fun createBurnCloud(level: ServerLevel, location: Vec3, radius: Float) {
        val duration = ProjectileConfig.MOLOTOV_CLOUD_DURATION.get()
        if (duration <= 0) return
        val cloud = AreaEffectCloud(level, location.x, location.y, location.z)
        cloud.addTag(BURN_CLOUD_TAG)
        cloud.owner = owner as? LivingEntity
        cloud.radius = radius
        cloud.radiusOnUse = 0f
        cloud.radiusPerTick = 0f
        cloud.waitTime = 0
        cloud.duration = duration * 20
        cloud.particle = ParticleTypes.FLAME
        cloud.addEffect(MobEffectInstance(ModMobEffects.BURN.get(), 60, 0))
        level.addFreshEntity(cloud)
    }

    private fun spreadFire(level: ServerLevel, location: Vec3, radius: Double) {
        val range = ceil(radius).toInt()
        val center = BlockPos.containing(location)
        for (dx in -range..range) {
            for (dz in -range..range) {
                if (dx * dx + dz * dz > radius * radius) continue
                // Follow nearby ground, including one-block steps and shallow dips.
                for (dy in 1 downTo -2) {
                    val pos = center.offset(dx, dy, dz)
                    if (!level.isEmptyBlock(pos) ||
                        !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    ) continue
                    val fire = BaseFireBlock.getState(level, pos)
                    if (!fire.canSurvive(level, pos)) continue
                    // Keep the splash from igniting the other side of a wall.
                    val path = level.clip(ClipContext(location, Vec3.atCenterOf(pos),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this))
                    if (path.type != HitResult.Type.MISS) break
                    level.setBlockAndUpdate(pos, fire)
                    level.sendParticles(ParticleTypes.FLAME, pos.x + 0.5, pos.y + 0.2, pos.z + 0.5,
                        5, 0.3, 0.1, 0.3, 0.03)
                    break
                }
            }
        }
        level.playSound(null, location.x, location.y, location.z, SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 1f, 1f)
    }

    companion object {
        const val BURN_CLOUD_TAG = "superbwarfare:molotov_cloud"
    }
}
