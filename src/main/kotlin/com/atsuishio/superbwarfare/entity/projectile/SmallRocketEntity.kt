package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModDamageTypes.causeProjectileHitDamage
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult

open class SmallRocketEntity(type: EntityType<out SmallRocketEntity>, level: Level) :
    FastThrowableProjectile(type, level), BasicGeoProjectileEntity {
    override fun getModel() = BedrockModelLoader.SMALL_ROCKET_MODEL

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

    override fun onHitEntity(result: EntityHitResult) {
        super.onHitEntity(result)
        val entity = result.entity
        val owner = this.owner
        if (owner != null && owner.vehicle != null && entity == owner.vehicle) return
        if (this.level() is ServerLevel) {
            if (owner is ServerPlayer) {
                this.level()
                    .playSound(null, owner.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1f, 1f)
                sendPacketTo(owner, ClientIndicatorMessage(0, 5))
            }

            entity.forceHurt(
                causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()),
                this.damageValue
            )

            if (entity is LivingEntity) {
                entity.invulnerableTime = 0
            }

            causeExplode(result.getLocation())
            this.discard()
        }
    }

    public override fun onHitBlock(result: BlockHitResult) {
        super.onHitBlock(result)
        if (this.level() is ServerLevel) {
            destroyBlock(result)
        }
    }

    override fun tick() {
        super.tick()
        mediumTrail()

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
