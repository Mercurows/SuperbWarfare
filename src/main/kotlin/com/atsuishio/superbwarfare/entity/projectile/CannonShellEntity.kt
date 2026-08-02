package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeProjectileHitDamage
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.forceHurt
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.max

open class CannonShellEntity(type: EntityType<out CannonShellEntity>, level: Level) :
    FastThrowableProjectile(type, level), BasicGeoProjectileEntity {

    private var fireProbability = 0f
    private var fireTime = 0

    enum class Type(val tagName: String) {
        AP("AP"), HE("HE"), CM("CM"), WP("WP")
    }

    private var type: Type? = Type.AP
    private var spreadAmount = 50
    private var spreadAngle = 15

    override fun getDefaultItem(): Item {
        return ModItems.LARGE_SHELL_HE.get()
    }

    fun durability(durability: Int): CannonShellEntity {
        this.durability = durability
        return this
    }

    override fun isColliding(pPos: BlockPos, pState: BlockState): Boolean {
        return true
    }

    override fun canPassThroughFluid() = this.type == Type.AP

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)

        compound.putFloat("FireProbability", this.fireProbability)
        compound.putInt("FireTime", this.fireTime)
        compound.putInt("SpreadAmount", this.spreadAmount)
        compound.putInt("SpreadAngle", this.spreadAngle)

        if (this.type != null) {
            compound.putString("Type", this.type!!.tagName)
        }
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)

        if (compound.contains("FireProbability")) {
            this.fireProbability = compound.getFloat("FireProbability")
        }

        if (compound.contains("FireTime")) {
            this.fireTime = compound.getInt("FireTime")
        }

        if (compound.contains("SpreadAmount")) {
            this.spreadAmount = compound.getInt("SpreadAmount")
        }

        if (compound.contains("SpreadAngle")) {
            this.spreadAngle = compound.getInt("SpreadAngle")
        }

        if (compound.contains("Type")) {
            val type = compound.getString("Type")
            this.type = Type.valueOf(type)
        }
    }

    override fun afterHitBlock(result: BlockHitResult) {
        val level = this.level()
        if (level !is ServerLevel) return

        if (type == Type.WP && this.owner != null) {
            causeWPEffect(result.location, owner!!)
            causeExplode(result.location)
            this.discard()
        }

        if (type != Type.AP) {
            causeExplode(result.location)
            this.discard()
        } else {
            if (ExplosionConfig.EXPLOSION_DESTROY.get() && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get() && this.explosionDestroyValue) {
                // AP穿透：沿入射线遍历方块，从近到远执行破坏并消耗穿甲值
                // 穿甲值不足以破坏下一个方块或检测到的方块为不可破坏的方块时停止
                val direction = deltaMovement.normalize()
                var rayPos = result.location.add(direction.scale(0.01))
                val processedBlocks = mutableSetOf<BlockPos>()

                for (step in 0..<30) {
                    if (durability <= 0) {
                        causeExplode(rayPos)
                        discard()
                        return
                    }

                    val blockPos = BlockPos.containing(rayPos)

                    if (!processedBlocks.add(blockPos)) {
                        rayPos = rayPos.add(direction.scale(0.5))
                        continue
                    }

                    val blockState = level.getBlockState(blockPos)
                    val hardness = blockState.block.defaultDestroyTime()

                    // 不可破坏的方块，停止穿透并爆炸
                    if (hardness == -1f) {
                        causeExplode(rayPos)
                        discard()
                        return
                    }

                    // 跳过空气
                    if (blockState.isAir) {
                        rayPos = rayPos.add(direction.scale(0.5))
                        continue
                    }

                    // 计算穿甲消耗
                    var cost = 0
                    if (blockState.canOcclude() || blockState.soundType == SoundType.GLASS) {
                        cost += 5 + hardness.toInt()
                    }
                    if (blockState.soundType == SoundType.STONE) {
                        cost += 5
                    }
                    if (blockState.soundType == SoundType.METAL || blockState.soundType == SoundType.COPPER || blockState.soundType == SoundType.NETHERITE_BLOCK) {
                        cost += 25
                    }

                    // 穿甲值不足，停止穿透并爆炸
                    if (durability < cost) {
                        causeExplode(rayPos)
                        discard()
                        return
                    }

                    // 破坏方块
                    level.destroyBlock(blockPos, true)
                    durability -= cost

                    // 累积减速和伤害衰减
                    val resistance = 0.95 - (hardness / 100).coerceIn(0f, 1f)
                    deltaMovement = deltaMovement.scale(resistance)
                    setDamage((damageValue * resistance).toFloat())
                    setExplosionDamage((explosionDamageValue * resistance).toFloat())
                    setExplosionRadius((explosionRadiusValue * resistance).toFloat())

                    ParticleTool.cannonHitParticles(level, Vec3.atCenterOf(blockPos))

                    // 移动到下一个方块
                    rayPos = rayPos.add(direction.scale(0.5))
                }

                // 穿透完毕，重设位置后继续飞行
                this.setPos(rayPos.x, rayPos.y, rayPos.z)
            } else {
                destroyBlock(result)
            }
        }
    }

    override fun afterHitEntity(result: EntityHitResult) {
        val level = this.level()
        if (level !is ServerLevel) return
        val entity = result.entity
        if (this.owner != null && entity == this.owner!!.vehicle) return

        if (type == Type.WP) {
            causeWPEffect(result.location, owner!!)
        }

        if (entity is VehicleEntity) {
            causeExplode(result.location)
            this.discard()
            return
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
                            causeProjectileHitDamage(this.level().registryAccess(), this, this.owner),
                            (this.damageValue * resistance).toFloat()
                        )
                        if (target is LivingEntity) {
                            target.invulnerableTime = 0
                        }
                        if (target is VehicleEntity) {
                            causeExplode(target.boundingBox.center)
                            this.discard()
                            return
                        }
                    }
                }
            }

            deltaMovement = deltaMovement.scale(resistance)
            this.setDamage((this.damageValue * resistance).toFloat())
        } else {
            causeExplode(result.location)
            this.discard()
        }
    }

    open fun causeWPEffect(pos: Vec3, shooter: Entity) {
        if (this.level() is ServerLevel) {
            val entities = SeekTool.Builder(shooter)
                .withinRange(pos, explosionRadiusValue.toDouble())
                .notItsVehicle()
                .baseFilter()
                .noVehicle()
                .build()

            entities.asSequence()
                .filter { it is LivingEntity && !(it is Player && it.isCreative) }
                .forEach {
                    val dis = pos.distanceTo(it.position())
                    if (!checkNoClip(it, pos)) return@forEach
                    (it as LivingEntity).addEffect(
                        MobEffectInstance(
                            ModMobEffects.PHOSPHORUS_FIRE.get(),
                            (300 - 30 * dis).toInt(),
                            max(explosionRadiusValue - dis, 0.0).toInt()
                        ), this.owner
                    )
                }
        }
    }

    override fun tick() {
        super.tick()

        shellTrail()

        if ((type == Type.CM || type == Type.WP) && tickCount > 3) {
            // 使用Minecraft内置的光线追踪进行碰撞检测
            val spreadTime = 8
            val hitResult = level().clip(
                ClipContext(
                    position(),
                    position().add(deltaMovement.scale(spreadTime.toDouble())),
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.ANY,
                    this
                )
            )

            if (hitResult.type == HitResult.Type.BLOCK) {
                if (type == Type.CM) {
                    releaseClusterMunitions(owner)
                } else {
                    releaseWp(owner)
                }
            }

            val target = TraceTool.findLookingEntity(this, deltaMovement.scale(spreadTime.toDouble()).length())
            if (target != null && target != this) {
                if (type == Type.CM) {
                    releaseClusterMunitions(owner)
                } else {
                    releaseWp(owner)
                }
            }
        }
    }

    open fun releaseClusterMunitions(shooter: Entity?) {
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

    open fun releaseWp(shooter: Entity?) {
        val level = this.level()
        if (level is ServerLevel) {
            ParticleTool.spawnMediumExplosionParticles(level, position())
            repeat(spreadAmount) {
                val whitePhosphorusProjectileEntity = WhitePhosphorusProjectileEntity(shooter, level)

                whitePhosphorusProjectileEntity.setPos(position().x, position().y, position().z)
                whitePhosphorusProjectileEntity.shoot(
                    deltaMovement.x,
                    deltaMovement.y,
                    deltaMovement.z,
                    (random.nextFloat() * 0.02f + 0.3f * deltaMovement.length()).toFloat(),
                    spreadAngle.toFloat()
                )
                level.addFreshEntity(whitePhosphorusProjectileEntity)
            }
            discard()
        }
    }

    override fun discardAfterExplode(): Boolean {
        return true
    }

    override fun getSound(): SoundEvent {
        return ModSounds.SHELL_FLY.get()
    }

    override fun getVolume(): Float {
        return 0.07f
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

    override fun getHiddenTicks() = 1
}
