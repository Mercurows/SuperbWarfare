package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.init.ModSounds
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3

open class MelonBombEntity(type: EntityType<out MelonBombEntity>, level: Level) : DestroyableProjectile(type, level) {
    init {
        this.explosionRadiusValue = 10f
        this.explosionDamageValue = 500f
    }

    override fun getDefaultItem(): Item {
        return Items.MELON
    }

    override fun afterHitEntity(result: EntityHitResult) {
        if (result.entity is MelonBombEntity) return

        val level = this.level()
        if (level is ServerLevel) {
            if (ExplosionConfig.EXPLOSION_DESTROY.get() && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                val aabb = AABB(result.location, result.location).inflate(5.0)
                BlockPos.betweenClosedStream(aabb).forEach {
                    val hard = level.getBlockState(it).block.defaultDestroyTime()
                    if (hard != -1f && Vec3(
                            it.x.toDouble(),
                            it.y.toDouble(),
                            it.z.toDouble()
                        ).distanceTo(result.location) < 3
                    ) {
                        level.destroyBlock(it, true)
                    }
                }
            }
        }

        super.afterHitEntity(result)
    }

    override fun afterHitBlock(result: BlockHitResult) {
        val level = this.level()
        if (level is ServerLevel) {
            if (ExplosionConfig.EXPLOSION_DESTROY.get() && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                val aabb = AABB(result.location, result.location).inflate(5.0)
                BlockPos.betweenClosedStream(aabb).forEach {
                    val hard = level.getBlockState(it).block.defaultDestroyTime()
                    if (hard != -1f && Vec3(
                            it.x.toDouble(),
                            it.y.toDouble(),
                            it.z.toDouble()
                        ).distanceTo(result.location) < 3
                    ) {
                        level.destroyBlock(it, true)
                    }
                }
            }
        }

        super.afterHitBlock(result)
    }

    override val maxHealth: Float
        get() = 15f

    override fun getSound(): SoundEvent {
        return ModSounds.SHELL_FLY.get()
    }

    override fun getVolume(): Float {
        return 0.7f
    }
}
