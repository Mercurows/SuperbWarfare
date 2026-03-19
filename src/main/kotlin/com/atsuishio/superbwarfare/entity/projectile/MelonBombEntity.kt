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
        this.noCulling = true
        this.explosionRadiusValue = 10f
        this.explosionDamageValue = 500f
    }

    override fun getDefaultItem(): Item {
        return Items.MELON
    }

    override fun onHitEntity(result: EntityHitResult) {
        super.onHitEntity(result)
        val entity = result.entity
        val owner = this.owner
        if (entity == owner || (owner != null && entity == owner.vehicle) || entity is MelonBombEntity) return
        val level = this.level()
        if (level is ServerLevel) {
            if (ExplosionConfig.EXPLOSION_DESTROY.get() && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                val aabb = AABB(result.getLocation(), result.getLocation()).inflate(5.0)
                BlockPos.betweenClosedStream(aabb).forEach {
                    val hard = level.getBlockState(it).block.defaultDestroyTime()
                    if (hard != -1f && Vec3(
                            it.x.toDouble(),
                            it.y.toDouble(),
                            it.z.toDouble()
                        ).distanceTo(result.getLocation()) < 3
                    ) {
                        level.destroyBlock(it, true)
                    }
                }
            }

            causeExplode(result.getLocation())
            this.discard()
        }
    }

    public override fun onHitBlock(result: BlockHitResult) {
        super.onHitBlock(result)
        val level = this.level()
        if (level is ServerLevel) {
            if (ExplosionConfig.EXPLOSION_DESTROY.get() && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                val aabb = AABB(result.getLocation(), result.getLocation()).inflate(5.0)
                BlockPos.betweenClosedStream(aabb).forEach {
                    val hard = level.getBlockState(it).block.defaultDestroyTime()
                    if (hard != -1f && Vec3(
                            it.x.toDouble(),
                            it.y.toDouble(),
                            it.z.toDouble()
                        ).distanceTo(result.getLocation()) < 3
                    ) {
                        level.destroyBlock(it, true)
                    }
                }
            }

            causeExplode(result.getLocation())
            this.discard()
        }
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
