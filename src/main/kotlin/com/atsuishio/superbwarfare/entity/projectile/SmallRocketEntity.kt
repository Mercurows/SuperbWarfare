package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult

open class SmallRocketEntity(type: EntityType<out SmallRocketEntity>, level: Level) :
    FastThrowableProjectile(type, level), BasicGeoProjectileEntity {
    init {
        this.noCulling = true
        this.damageValue = 140f
        this.explosionDamageValue = 60f
        this.explosionRadiusValue = 5f
        this.durability = 20
    }

    override fun getDefaultItem(): Item {
        return ModItems.SMALL_ROCKET.get()
    }

    override fun afterHitBlock(result: BlockHitResult) {
        if (this.level() is ServerLevel) {
            destroyBlock(result)
        }
    }

    override fun tick() {
        super.tick()
        smallTrail()

        val level = this.level()
        if (this.tickCount == 3) {
            if (level is ServerLevel) {
                ParticleTool.sendParticle(
                    level,
                    ParticleTypes.CLOUD,
                    this.xo,
                    this.yo,
                    this.zo,
                    15,
                    0.8,
                    0.8,
                    0.8,
                    0.01,
                    true
                )
                ParticleTool.sendParticle(
                    level,
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    this.xo,
                    this.yo,
                    this.zo,
                    10,
                    0.8,
                    0.8,
                    0.8,
                    0.01,
                    true
                )
            }
        }
    }

    override fun getSound(): SoundEvent {
        return ModSounds.ROCKET_FLY.get()
    }

    override fun getVolume(): Float {
        return 0.2f
    }
}
