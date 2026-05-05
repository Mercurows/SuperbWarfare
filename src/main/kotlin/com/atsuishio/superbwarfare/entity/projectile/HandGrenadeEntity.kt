package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BellBlock
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import kotlin.math.min

open class HandGrenadeEntity : FastThrowableProjectile, BasicGeoProjectileEntity {
    override fun getModel() = BedrockModelLoader.HAND_GRENADE_MODEL

    constructor(type: EntityType<out HandGrenadeEntity>, level: Level) : super(type, level) {
        this.noCulling = true
        this.damageValue = 1f
        this.explosionDamageValue = ExplosionConfig.M67_GRENADE_EXPLOSION_DAMAGE.get().toFloat()
        this.explosionRadiusValue = ExplosionConfig.M67_GRENADE_EXPLOSION_RADIUS.get().toFloat()
    }

    constructor(type: EntityType<out HandGrenadeEntity>, x: Double, y: Double, z: Double, level: Level) : super(
        type,
        x,
        y,
        z,
        level
    ) {
        this.noCulling = true
        this.damageValue = 1f
        this.explosionDamageValue = ExplosionConfig.M67_GRENADE_EXPLOSION_DAMAGE.get().toFloat()
        this.explosionRadiusValue = ExplosionConfig.M67_GRENADE_EXPLOSION_RADIUS.get().toFloat()
    }

    constructor(entity: LivingEntity?, level: Level) : super(ModEntities.HAND_GRENADE.get(), entity, level) {
        this.noCulling = true
        this.damageValue = 1f
        this.explosionDamageValue = ExplosionConfig.M67_GRENADE_EXPLOSION_DAMAGE.get().toFloat()
        this.explosionRadiusValue = ExplosionConfig.M67_GRENADE_EXPLOSION_RADIUS.get().toFloat()
    }

    override fun getDefaultItem(): Item {
        return ModItems.HAND_GRENADE.get()
    }

    override fun onHit(result: HitResult) {
        when (result.type) {
            HitResult.Type.BLOCK -> {
                val blockResult = result as BlockHitResult
                val resultPos = blockResult.blockPos
                val state = this.level().getBlockState(resultPos)
                val block = state.block
                val event = block.getSoundType(state, this.level(), resultPos, this).breakSound
                val speed = this.deltaMovement.length()
                if (speed > 0.1) {
                    val volume = min(4f, speed.toFloat() / 4f + 0.5f)
                    this.level().playSound(
                        null,
                        result.getLocation().x,
                        result.getLocation().y,
                        result.getLocation().z,
                        event,
                        SoundSource.AMBIENT,
                        volume,
                        1f
                    )
                }
                this.bounce(blockResult.direction)

                if (block is BellBlock) {
                    block.attemptToRing(this.level(), resultPos, blockResult.direction)
                }
            }

            HitResult.Type.ENTITY -> {
                val entityResult = result as EntityHitResult
                val entity = entityResult.entity
                val owner = this.owner
                if (entity == owner || entity == this.vehicle) return
                val speedE = this.deltaMovement.length()
                if (speedE > 0.1) {
                    if (owner is LivingEntity) {
                        if (owner is ServerPlayer) {
                            owner.level().playSound(
                                null,
                                owner.blockPosition(),
                                ModSounds.INDICATION.get(),
                                SoundSource.VOICE,
                                1f,
                                1f
                            )

                            sendPacketTo(owner, ClientIndicatorMessage(0, 5))
                        }
                    }
                    entity.hurt(entity.damageSources().thrown(this, owner), this.damageValue)
                }
                this.bounce(
                    Direction.getNearest(
                        this.deltaMovement.x(),
                        this.deltaMovement.y(),
                        this.deltaMovement.z()
                    ).opposite
                )
                this.deltaMovement = this.deltaMovement.multiply(0.25, 1.0, 0.25)
            }

            else -> {}
        }
    }

    private fun bounce(direction: Direction) {
        when (direction.axis) {
            Direction.Axis.X -> this.deltaMovement = this.deltaMovement.multiply(-0.5, 0.75, 0.75)
            Direction.Axis.Y -> {
                this.deltaMovement = this.deltaMovement.multiply(0.75, -0.25, 0.75)
                if (this.deltaMovement.y() < this.getCustomGravity()) {
                    this.deltaMovement = this.deltaMovement.multiply(1.0, 0.0, 1.0)
                }
            }

            Direction.Axis.Z -> this.deltaMovement = this.deltaMovement.multiply(0.75, 0.75, -0.5)
        }
    }

    override fun tick() {
        super.tick()
        val level = this.level()
        if (level is ServerLevel) {
            ParticleTool.sendParticle(
                level, ParticleTypes.SMOKE, this.xo, this.yo, this.zo,
                1, 0.0, 0.0, 0.0, 0.01, true
            )
        }
        if (isInFluidType) {
            deltaMovement = deltaMovement.scale(0.75)
        }
    }

    override fun isFastMoving(): Boolean {
        return false
    }
}
