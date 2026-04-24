package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3

open class AerialBombEntity(type: EntityType<out AerialBombEntity>, level: Level) : DestroyableProjectile(type, level) {
    override fun getDefaultItem(): Item {
        return ModItems.MEDIUM_AERIAL_BOMB.get()
    }

    override fun getSound(): SoundEvent {
        return ModSounds.SHELL_FLY.get()
    }

    override fun getVolume(): Float {
        return 0.7f
    }

    override fun onHitEntity(result: EntityHitResult) {
        super.onHitEntity(result)
        val entity = result.entity
        val owner = this.owner
        if (entity == owner || (owner != null && entity == owner.vehicle) || entity is AerialBombEntity) return
        if (this.level() is ServerLevel) {
            if (ExplosionConfig.EXPLOSION_DESTROY.get() && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                val aabb = AABB(result.getLocation(), result.getLocation()).inflate(5.0)
                BlockPos.betweenClosedStream(aabb).forEach {
                    val hard = this.level().getBlockState(it).block.defaultDestroyTime()
                    if (hard != -1f && Vec3(
                            it.x.toDouble(),
                            it.y.toDouble(),
                            it.z.toDouble()
                        ).distanceTo(result.getLocation()) < 3
                    ) {
                        this.level().destroyBlock(it, true)
                    }
                }
            }

            causeExplode(result.getLocation())
            this.discard()
        }
    }

    override fun onHitBlock(result: BlockHitResult) {
        super.onHitBlock(result)
        if (this.level() is ServerLevel) {
            if (ExplosionConfig.EXPLOSION_DESTROY.get() && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                val aabb = AABB(result.getLocation(), result.getLocation()).inflate(5.0)
                BlockPos.betweenClosedStream(aabb).forEach {
                    val hard = this.level().getBlockState(it).block.defaultDestroyTime()
                    if (hard != -1f && Vec3(
                            it.x.toDouble(),
                            it.y.toDouble(),
                            it.z.toDouble()
                        ).distanceTo(result.getLocation()) < 3
                    ) {
                        this.level().destroyBlock(it, true)
                    }
                }
            }

            causeExplode(result.getLocation())
            this.discard()
        }
    }
}