package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeProjectileHitDamage
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.forceHurt
import com.atsuishio.superbwarfare.tools.sendPacketToTrackingThis
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.item.Item
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

open class MediumRocketEntity : FastThrowableProjectile, BasicGeoProjectileEntity {
    enum class Type {
        AP, HE, CM
    }

    private var type: Type? = Type.AP
    private var fireProbability = 0f
    private var fireTime = 0
    private var spreadAmount = 50
    private var spreadAngle = 15

    constructor(type: EntityType<out MediumRocketEntity>, world: Level) : super(type, world) {
        this.noCulling = true
    }

    constructor(
        pEntityType: EntityType<out ThrowableItemProjectile>,
        pX: Double,
        pY: Double,
        pZ: Double,
        pLevel: Level,
        damage: Float,
        radius: Float,
        explosionDamage: Float,
        fireProbability: Float,
        fireTime: Int,
        type: Type?,
        spreadAmount: Int,
        spreadAngle: Int
    ) : super(pEntityType, pX, pY, pZ, pLevel) {
        this.noCulling = true
        this.damageValue = damage
        this.explosionRadiusValue = radius
        this.explosionDamageValue = explosionDamage
        this.fireProbability = fireProbability
        this.fireTime = fireTime
        this.type = type
        this.spreadAmount = spreadAmount
        this.spreadAngle = spreadAngle
    }

    fun durability(durability: Int): MediumRocketEntity {
        this.durability = durability
        return this
    }

    override fun isColliding(pPos: BlockPos, pState: BlockState): Boolean {
        return true
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        compound.putFloat("FireProbability", this.fireProbability)
        compound.putInt("FireTime", this.fireTime)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        if (compound.contains("FireProbability")) {
            this.fireProbability = compound.getFloat("FireProbability")
        }

        if (compound.contains("FireTime")) {
            this.fireTime = compound.getInt("FireTime")
        }
    }

    override fun getDefaultItem(): Item {
        return ModItems.SMALL_ROCKET.get()
    }

    @Suppress("DEPRECATION")
    public override fun onHitBlock(result: BlockHitResult) {
        super.onHitBlock(result)

        val level = this.level()
        if (level is ServerLevel) {
            val pos = result.blockPos
            val blockState = level.getBlockState(pos)
            if (type == Type.HE || type == Type.CM) {
                causeExplode(result.getLocation())
                this.discard()
                return
            }
            if (ExplosionConfig.EXPLOSION_DESTROY.get()) {
                val hardness = level.getBlockState(pos).block.defaultDestroyTime()

                val resistance = 0.95 - (hardness / 100).coerceIn(0f, 1f)

                if (blockState.canOcclude() || blockState.soundType === SoundType.GLASS) {
                    durability -= 5 + (hardness).toInt()
                }

                if (blockState.soundType === SoundType.STONE) {
                    durability -= 5
                }

                if (blockState.soundType === SoundType.METAL || blockState.soundType === SoundType.COPPER || blockState.soundType === SoundType.NETHERITE_BLOCK) {
                    durability -= 25
                }

                if (hardness <= durability && hardness != -1f) {
                    level.destroyBlock(pos, true)
                }

                if (hardness == -1f || hardness > durability || durability <= 0) {
                    causeExplode(pos.center)
                    discard()
                } else {
                    ParticleTool.cannonHitParticles(level, result.getLocation())
                    val mediumRocket = MediumRocketEntity(ModEntities.MEDIUM_ROCKET.get(), level)
                    mediumRocket.setPos(result.getLocation().add(deltaMovement.normalize().scale(0.99)))
                    mediumRocket.shoot(
                        deltaMovement.x,
                        deltaMovement.y - gravityValue,
                        deltaMovement.z,
                        (deltaMovement.length() * resistance).toFloat(),
                        0f
                    )
                    mediumRocket.owner = owner
                    mediumRocket.durability(durability)
                    mediumRocket.setType(Type.AP)
                    mediumRocket.setGravity(gravityValue)
                    mediumRocket.setLife(lifeValue - tickCount)
                    mediumRocket.setDamage((damageValue * resistance).toFloat())
                    mediumRocket.setExplosionDamage((explosionDamageValue * resistance).toFloat())
                    mediumRocket.setExplosionRadius((explosionRadiusValue * resistance).toFloat())
                    level.addFreshEntity(mediumRocket)
                    discard()
                }
            } else {
                destroyBlock(result)
            }
        }
    }

    public override fun onHitEntity(result: EntityHitResult) {
        super.onHitEntity(result)
        if (tickCount < 2) return
        val level = this.level()
        if (level is ServerLevel) {
            val entity = result.entity
            val owner = this.owner
            if (owner != null && entity == owner.vehicle) return

            entity.forceHurt(
                causeProjectileHitDamage(level.registryAccess(), this, owner),
                this.damageValue
            )
            if (entity is LivingEntity) {
                entity.invulnerableTime = 0
            }

            if (entity is VehicleEntity) {
                causeExplode(result.getLocation())
                this.discard()
            }

            if (type == Type.AP) {
                val pos = entity.boundingBox.center
                val resultEntities = TraceTool.getEntitiesAlongVector(level, pos, deltaMovement) { true }
                var resistance = 1.0

                for (rayTraceResultEntity in resultEntities) {
                    if (rayTraceResultEntity.entity != null) {
                        resistance *= 0.95
                        val target = rayTraceResultEntity.entity
                        if (rayTraceResultEntity.entity !== entity) {
                            target.forceHurt(
                                causeProjectileHitDamage(level.registryAccess(), this, owner),
                                (this.damageValue * resistance).toFloat()
                            )
                            if (target is LivingEntity) {
                                target.invulnerableTime = 0
                            }
                        }
                    }
                }

                deltaMovement = deltaMovement.scale(resistance)
                setDamage((this.damageValue * resistance).toFloat())
            }
        }
    }

    override fun tick() {
        super.tick()
        largeTrail()

        if (type == Type.CM) {
            // 使用Minecraft内置的光线追踪进行碰撞检测
            val hitResult = level().clip(
                ClipContext(
                    position(),
                    position().add(deltaMovement.scale(8.0)),
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.ANY,
                    this
                )
            )

            if (hitResult.type == HitResult.Type.BLOCK) {
                releaseClusterMunitions(owner)
            }
        }
    }

    override fun syncMotion() {
        if (!this.level().isClientSide) {
            sendPacketToTrackingThis(ClientMotionSyncMessage(this))
        }
    }

    override fun discardAfterExplode(): Boolean {
        return true
    }

    private fun releaseClusterMunitions(shooter: Entity?) {
        val level = this.level()
        if (level is ServerLevel) {
            ParticleTool.spawnMediumExplosionParticles(level, position())
            repeat(spreadAmount) {
                val gunGrenadeEntity = GunGrenadeEntity(
                    shooter, level,
                    6 * damageValue / spreadAmount,
                    5 * explosionDamageValue / spreadAmount,
                    explosionRadiusValue / 2
                )

                gunGrenadeEntity.setPos(position().x, position().y, position().z)
                gunGrenadeEntity.shoot(
                    deltaMovement.x,
                    deltaMovement.y,
                    deltaMovement.z,
                    (random.nextFloat() * 0.2f + 0.4f * deltaMovement.length()).toFloat(),
                    spreadAngle.toFloat()
                )
                level.addFreshEntity(gunGrenadeEntity)
            }
            discard()
        }
    }

    override fun getSound(): SoundEvent {
        return ModSounds.ROCKET_FLY.get()
    }

    override fun getVolume(): Float {
        return 0.7f
    }

    override fun forceLoadChunk(): Boolean {
        return true
    }

    fun setType(type: Type?) {
        this.type = type
    }

    fun setSpreadAmount(spreadAmount: Int) {
        this.spreadAmount = spreadAmount
    }

    fun setSpreadAngle(spreadAngle: Int) {
        this.spreadAngle = spreadAngle
    }

    override fun getModel() = BedrockModelLoader.MEDIUM_ROCKET_MODEL
}
