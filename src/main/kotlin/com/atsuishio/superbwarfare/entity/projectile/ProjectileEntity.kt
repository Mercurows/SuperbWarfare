package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent.HitBlock
import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent.HitEntity
import com.atsuishio.superbwarfare.client.particle.BulletDecalOption
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.config.server.ProjectileConfig
import com.atsuishio.superbwarfare.entity.OBBEntity
import com.atsuishio.superbwarfare.entity.living.DPSGeneratorEntity
import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback
import com.atsuishio.superbwarfare.entity.mixin.OBBHitter
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeGunFireAbsoluteDamage
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeGunFireDamage
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeGunFireHeadshotAbsoluteDamage
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeGunFireHeadshotDamage
import com.atsuishio.superbwarfare.item.weapon.BeastItem.Companion.beastKill
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.HitboxHelper.getBoundingBox
import com.atsuishio.superbwarfare.tools.HitboxHelper.getVelocity
import com.atsuishio.superbwarfare.tools.VectorTool.isInLiquid
import com.atsuishio.superbwarfare.world.phys.EntityResult
import com.atsuishio.superbwarfare.world.phys.ExtendedEntityRayTraceResult
import com.mojang.datafixers.util.Pair
import net.minecraft.core.BlockPos
import net.minecraft.core.BlockPos.MutableBlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.VoxelShape
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.entity.PartEntity
import net.neoforged.neoforge.event.EventHooks
import net.neoforged.neoforge.network.PacketDistributor
import java.util.*
import java.util.function.*
import java.util.function.Function
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max

@Suppress("unused")
open class ProjectileEntity(entityType: EntityType<out ProjectileEntity>, level: Level) : Projectile(entityType, level),
    CustomSyncMotionEntity, ExplosiveProjectile {
    // 子弹的发射者，可以为空
    var shooter: Entity? = null
        protected set

    // 子弹的发射者的ID
    var shooterId: Int = 0
        protected set

    // 子弹的伤害
    private var damage = 1f

    // 子弹的爆头倍率
    private var headShot = 1f

    // 子弹的打腿倍率
    private var legShot = 0.5f

    // 是否为野兽弹
    private var beast = false

    // 子弹是否是瞄准时发射的
    var isZoom: Boolean = false
        private set

    // 子弹的穿甲比例
    var bypassArmorRate: Float = 0.0f
        private set

    // 爆炸伤害（用于高爆弹等）
    private var explosionDamage = 0.0f

    // 爆炸半径（用于高爆弹等）
    private var explosionRadius = 0.0f

    // 燃烧弹等级
    private var fireLevel = 0

    // 是否为龙息弹
    private var dragonBreath = false

    // 击退力度
    private var knockback = 0.05f

    // 出膛速度
    private var velocity = 20f

    // 是否强制击退生物
    private var forceKnockback = false

    // 是否能穿墙
    var isPenetrating: Boolean = false

    // 子弹造成的状态效果
    private val mobEffects = ArrayList<Supplier<MobEffectInstance>>()

    // 发射子弹的武器ID
    var gunItemId: String? = null
        private set

    // 重力
    private var gravity = 0.05f
    private var life = 40

    init {
        this.noCulling = true
    }

    constructor(level: Level) : this(ModEntities.PROJECTILE.get(), level)

    protected fun findEntityOnPath(startVec: Vec3, endVec: Vec3): EntityResult? {
        var hitVec: Vec3? = null
        var hitEntity: Entity? = null
        var headshot = false
        var legShot = false
        val entities = this.level()
            .getEntities(
                this,
                this.boundingBox
                    .expandTowards(this.deltaMovement)
                    .inflate((if (this.beast) 3 else 1).toDouble()),
                PROJECTILE_TARGETS
            )
        var closestDistance = Double.MAX_VALUE

        for (entity in entities) {
            if (entity == this.shooter || this.shooter != null && entity == this.shooter!!.vehicle) continue

            if (entity is TargetEntity && entity.getEntityData().get(TargetEntity.DOWN_TIME) > 0) continue
            if (entity is DPSGeneratorEntity && entity.getEntityData().get(DPSGeneratorEntity.DOWN_TIME) > 0) continue

            val result = this.getHitResult(entity, startVec, endVec) ?: continue

            val hitPos = result.hitVec

            val distanceToHit = startVec.distanceTo(hitPos)
            if (distanceToHit < closestDistance) {
                hitVec = hitPos
                hitEntity = entity
                closestDistance = distanceToHit
                headshot = result.headshot
                legShot = result.legShot
            }
        }
        return if (hitEntity != null) EntityResult(hitEntity, hitVec!!, headshot, legShot) else null
    }

    protected fun findEntitiesOnPath(startVec: Vec3, endVec: Vec3): MutableList<EntityResult> {
        val hitEntities: MutableList<EntityResult> = arrayListOf()
        val entities = this.level().getEntities(
            this,
            this.boundingBox
                .expandTowards(this.deltaMovement)
                .inflate(1.0),
            PROJECTILE_TARGETS
        )
        for (entity in entities) {
            if (this.shooter == null || entity !== shooter && entity !== this.shooter!!.vehicle) {
                val result = this.getHitResult(entity, startVec, endVec) ?: continue
                if (entity.vehicle != null && this.shooter != null && entity.vehicle === this.shooter!!.vehicle) continue
                hitEntities.add(result)
            }
        }
        return hitEntities
    }

    /**
     * From TaC-Z
     */
    private fun getHitResult(entity: Entity, startVec: Vec3, endVec: Vec3): EntityResult? {
        val expandHeight = if (entity is Player && !entity.isCrouching) 0.0625 else 0.0

        var hitPos: Vec3? = null
        if (entity is OBBEntity && !entity.enableAABB()) {
            for (obb in entity.getOBBs()) {
                val obbVec = obb.clip(OBB.vec3ToVector3d(startVec), OBB.vec3ToVector3d(endVec)).orElse(null)
                if (obbVec != null) {
                    hitPos = OBB.vector3dToVec3(obbVec)
                    val level = this.level()
                    if (level is ServerLevel) {
                        level.playSound(
                            null,
                            BlockPos.containing(hitPos),
                            ModSounds.HIT.get(),
                            SoundSource.PLAYERS,
                            1f,
                            1f
                        )
                        ParticleTool.sendParticle(
                            level,
                            ModParticleTypes.FIRE_STAR.get(),
                            hitPos.x,
                            hitPos.y,
                            hitPos.z,
                            2,
                            0.0,
                            0.0,
                            0.0,
                            0.2,
                            false
                        )
                        ParticleTool.sendParticle(
                            level,
                            ParticleTypes.SMOKE,
                            hitPos.x,
                            hitPos.y,
                            hitPos.z,
                            2,
                            0.0,
                            0.0,
                            0.0,
                            0.01,
                            false
                        )
                    }

                    val acc = OBBHitter.getInstance(this)
                    acc.`sbw$setCurrentHitPart`(obb.part)
                }
            }
        } else {
            var boundingBox = entity.boundingBox
            var velocity = Vec3(entity.x - entity.xOld, entity.y - entity.yOld, entity.z - entity.zOld)

            val shooter = this.shooter
            if (entity is ServerPlayer && shooter is ServerPlayer) {
                val ping = Mth.floor((shooter.connection.latency() / 1000.0) * 20.0 + 0.5)
                boundingBox = getBoundingBox(entity, ping)
                velocity = getVelocity(entity, ping)
            }
            boundingBox = boundingBox.expandTowards(0.0, expandHeight, 0.0)
            boundingBox = boundingBox.expandTowards(velocity.x, velocity.y, velocity.z)

            val playerHitboxOffset = 3.0
            if (entity is ServerPlayer) {
                if (entity.vehicle != null) {
                    boundingBox = boundingBox.move(
                        velocity.multiply(
                            playerHitboxOffset / 2,
                            playerHitboxOffset / 2,
                            playerHitboxOffset / 2
                        )
                    )
                }
                boundingBox =
                    boundingBox.move(velocity.multiply(playerHitboxOffset, playerHitboxOffset, playerHitboxOffset))
            }

            if (entity.vehicle != null) {
                boundingBox = boundingBox.move(velocity.multiply(-2.5, -2.5, -2.5))
            }
            boundingBox = boundingBox.move(velocity.multiply(-5.0, -5.0, -5.0))

            if (this.beast) {
                boundingBox = boundingBox.inflate(3.0)
            }

            hitPos = boundingBox.clip(startVec, endVec).orElse(null)
        }

        if (hitPos == null) {
            return null
        }
        val hitBoxPos = hitPos.subtract(entity.position())
        var headshot = false
        var legShot = false
        val eyeHeight = entity.eyeHeight
        val bodyHeight = entity.bbHeight
        if ((eyeHeight - 0.25) < hitBoxPos.y && hitBoxPos.y < (eyeHeight + 0.3) && entity is LivingEntity) {
            headshot = true
        }
        if (hitBoxPos.y < (0.33 * bodyHeight) && entity is LivingEntity) {
            legShot = true
        }

        if (this.explosionDamage > 0) {
            explosionBullet(this, hitPos)
        }

        return EntityResult(entity, hitPos, headshot, legShot)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(COLOR_R, DEFAULT_R)
            .define(COLOR_G, DEFAULT_G)
            .define(COLOR_B, DEFAULT_B)
    }

    override fun tick() {
        super.tick()
        this.updateHeading()

        val vec = this.deltaMovement

        val level = this.level()
        if (!level.isClientSide()) {
            val startVec = this.position()
            var endVec = startVec.add(this.deltaMovement)
            var result: HitResult? =
                rayTraceBlocks(
                    this.level(),
                    ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this),
                    if (this.isPenetrating || this.beast) Predicate { true } else if (ProjectileConfig.PROJECTILE_DESTROY_BLOCKS.get()) IGNORE_LIST.and(
                        Predicate { input -> !input.`is`(ModTags.Blocks.BULLET_CAN_DESTROY) }) else IGNORE_LIST
                )

            val fluidResult: BlockHitResult =
                rayTraceBlocks(
                    this.level(),
                    ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this),
                    if (this.isPenetrating || this.beast) Predicate { true } else if (ProjectileConfig.PROJECTILE_DESTROY_BLOCKS.get()) IGNORE_LIST.and(
                        Predicate { input -> !input.`is`(ModTags.Blocks.BULLET_CAN_DESTROY) }) else IGNORE_LIST
                )

            if (result != null && result.type != HitResult.Type.MISS) {
                endVec = result.getLocation()
            }

            val entityResults: MutableList<EntityResult> = arrayListOf()
            val temp = findEntitiesOnPath(startVec, endVec)
            entityResults.addAll(temp)

            if (this.shooter != null) {
                entityResults.sortWith(Comparator.comparingDouble(ToDoubleFunction {
                    it.hitVec.distanceTo(this.shooter!!.position())
                }))
            }

            for (entityResult in entityResults) {
                result = ExtendedEntityRayTraceResult(entityResult)

                val resEntity = result.entity
                val shooter = this.shooter
                if (resEntity is Player) {
                    if (shooter is Player && !shooter.canHarmPlayer(resEntity)) {
                        result = null
                    }
                }
                if (result != null) {
                    if (!EventHooks.onProjectileImpact(this, result)) this.onHit(result)
                    else continue  // 命中事件被取消则检查下一个命中结果
                }

                if (!this.beast) {
                    this.bypassArmorRate -= 0.2f
                    if (this.bypassArmorRate < 0.8f) {
                        if (result != null && !(resEntity is TargetEntity && resEntity.getEntityData()
                                .get(TargetEntity.DOWN_TIME) > 0)
                            && !(resEntity is DPSGeneratorEntity && resEntity.getEntityData()
                                .get(DPSGeneratorEntity.DOWN_TIME) > 0)
                        ) {
                            break
                        }
                    }
                }
            }
            if (entityResults.isEmpty() && result != null) {
                this.onHit(result)
            }

            this.onHitWater(fluidResult.getLocation(), fluidResult)
            this.setPos(this.x + vec.x, this.y + vec.y, this.z + vec.z)
        } else {
            this.setPosRaw(this.x + vec.x, this.y + vec.y, this.z + vec.z)
        }

        this.deltaMovement = this.deltaMovement.add(0.0, -this.gravity.toDouble(), 0.0)

        if (this.tickCount > (if (fireLevel > 0) 10 else life)) {
            this.discard()
        }

        if (fireLevel > 0 && dragonBreath && level is ServerLevel) {
            val randomPos = this.tickCount * 0.08 * (Math.random() - 0.5)
            ParticleTool.sendParticle(
                level,
                ParticleTypes.FLAME,
                (this.xo + this.x) / 2 + randomPos,
                (this.yo + this.y) / 2 + randomPos,
                (this.zo + this.z) / 2 + randomPos,
                0,
                this.deltaMovement.x,
                this.deltaMovement.y,
                this.deltaMovement.z,
                max(this.deltaMovement.length() - 1.1 * this.tickCount, 0.2),
                true
            )
        }

        if (level is ServerLevel) {
            if (isInLiquid(level, position())) {
                this.deltaMovement = this.deltaMovement.multiply(0.75, 0.75, 0.75)
            }
            if (this.isInWater) {
                val l = deltaMovement.length()
                var i = 0.0
                while (i < l) {
                    val startPos = Vec3(this.xo, this.yo, this.zo)
                    val pos = startPos.add(deltaMovement.normalize().scale(i))
                    ParticleTool.sendParticle(
                        level, ParticleTypes.BUBBLE_COLUMN_UP, pos.x, pos.y, pos.z,
                        1, 0.0, 0.0, 0.0, 0.001, true
                    )
                    i++
                }
            }
        }

        this.syncMotion()
    }

    override fun syncMotion() {
        if (!this.level().isClientSide) {
            PacketDistributor.sendToPlayersTrackingEntity(this, ClientMotionSyncMessage(this))
        }
    }

    override fun onHit(result: HitResult) {
        if (result is BlockHitResult) {
            val level = this.level()
            if (result.type == HitResult.Type.MISS) {
                return
            }
            val resultPos = result.blockPos
            val state = level.getBlockState(resultPos)
            val event = state.block.getSoundType(state, level, resultPos, this).breakSound
            level.playSound(
                null,
                result.getLocation().x,
                result.getLocation().y,
                result.getLocation().z,
                event,
                SoundSource.AMBIENT,
                1f,
                1f
            )
            val hitVec = result.getLocation()

            this.onHitBlock(hitVec, result)
            if (this.explosionDamage > 0) {
                explosionBullet(this, hitVec)
            }
            if (fireLevel > 0 && level is ServerLevel) {
                ParticleTool.sendParticle(
                    level, ParticleTypes.LAVA, hitVec.x, hitVec.y, hitVec.z,
                    3, 0.0, 0.0, 0.0, 0.5, true
                )
            }
        }

        if (result is EntityHitResult) {
            val entity = result.entity
            if (entity.id == this.shooterId) {
                return
            }

            if (this.shooter is Player) {
                if (entity.hasIndirectPassenger(shooter!!)) {
                    return
                }
            }
            if (result is ExtendedEntityRayTraceResult) {
                this.onHitEntity(entity, result)
            } else { // 若不是带命中部位信息的结果，则构造一个用于触发命中事件，这种情况在外部手动调用onHit时出现
                this.onHitEntity(entity, ExtendedEntityRayTraceResult(result))
            }
            entity.invulnerableTime = 0
        }
    }

    private fun getRings(direction: Direction, hitVec: Vec3): Int {
        val x = abs(Mth.frac(hitVec.x) - 0.5)
        val y = abs(Mth.frac(hitVec.y) - 0.5)
        val z = abs(Mth.frac(hitVec.z) - 0.5)
        val axis = direction.axis
        val v: Double = if (axis === Direction.Axis.Y) {
            max(x, z)
        } else if (axis === Direction.Axis.Z) {
            max(x, y)
        } else {
            max(y, z)
        }

        return max(1, Mth.ceil(10.0 * Mth.clamp((0.5 - v) / 0.5, 0.0, 1.0)))
    }

    fun recordHitScore(direction: Direction, hitVec: Vec3) {
        val shooter = this.shooter ?: return
        val score = this.getRings(direction, hitVec)
        val distance = this.shooter!!.position().distanceTo(hitVec)

        if (shooter !is Player) {
            return
        }

        shooter.displayClientMessage(
            Component.literal(score.toString())
                .append(Component.translatable("tips.superbwarfare.shoot.rings"))
                .append(Component.literal(" " + format1D(distance, "m"))), false
        )

        if (shooter is ServerPlayer) {
            val holder = if (score == 10) Holder.direct(ModSounds.HEADSHOT.get())
            else Holder.direct(ModSounds.INDICATION.get())

            sendPacketTo(
                shooter,
                ClientboundSoundPacket(
                    holder,
                    SoundSource.PLAYERS,
                    shooter.x,
                    shooter.y,
                    shooter.z,
                    1f,
                    1f,
                    shooter.level().random.nextLong()
                )
            )
            sendPacketTo(shooter, ClientIndicatorMessage(if (score == 10) 1 else 0, 5))
        }

        val stack = shooter.offhandItem
        if (stack.`is`(ModItems.TRANSCRIPT.get())) {
            val size = 10

            var scores = stack.get(ModDataComponents.TRANSCRIPT_SCORE)
            if (scores == null) scores = mutableListOf()

            val queue = ArrayDeque(scores)
            queue.offer(Pair<Int, Double>(score, distance))

            while (queue.size > size) {
                queue.poll()
            }

            stack.set(
                ModDataComponents.TRANSCRIPT_SCORE,
                queue.toList()
            )
        }
    }

    protected fun onHitWater(location: Vec3, result: BlockHitResult) {
        val level = this.level()
        if (level is ServerLevel) {
            val pos = result.blockPos
            val face = result.direction
            val state = level().getBlockState(pos)

            val vx = face.stepX.toDouble()
            val vy = face.stepY.toDouble()
            val vz = face.stepZ.toDouble()
            val dir = Vec3(vx, vy, vz).add(deltaMovement.normalize().scale(-0.1))

            if (state.block === Blocks.WATER) {
                if (!this.isInWater) {
                    val particleData = CustomCloudOption(1f, 1f, 1f, 80, 0.5f, 1f, cooldown = false, light = false)
                    for (i in 0..9) {
                        val vec3 = randomVec(dir, 40.0)
                        ParticleTool.sendParticle(
                            level,
                            particleData,
                            location.x + 0.12 * i * dir.x,
                            location.y + 0.12 * i * dir.y,
                            location.z + 0.12 * i * dir.z,
                            0,
                            vec3.x,
                            vec3.y,
                            vec3.z,
                            15.0,
                            true
                        )
                    }

                    ParticleTool.spawnBulletHitWaterParticles(level, location)
                    level.playSound(
                        null,
                        BlockPos(location.x.toInt(), location.y.toInt(), location.z.toInt()),
                        ModSounds.HIT_WATER.get(),
                        SoundSource.BLOCKS,
                        1f,
                        1f
                    )

                    // 水下路径气泡
                    val l = deltaMovement.length()
                    var i = 0.0
                    while (i < l) {
                        val p = location.add(deltaMovement.normalize().scale(i))
                        ParticleTool.sendParticle(
                            level, ParticleTypes.BUBBLE_COLUMN_UP, p.x, p.y, p.z,
                            1, 0.0, 0.0, 0.0, 0.001, false
                        )
                        i++
                    }

                    this.deltaMovement = this.deltaMovement.multiply(0.1, 0.1, 0.1)
                }
            } else if (state.block === Blocks.LAVA) {
                if (!this.isInLava) {
                    val particleData = BlockParticleOption(ParticleTypes.BLOCK, state)
                    for (i in 0..6) {
                        val vec3 = randomVec(dir, 20.0)
                        ParticleTool.sendParticle(
                            level,
                            particleData,
                            location.x + 0.1 * i * dir.x,
                            location.y + 0.1 * i * dir.y,
                            location.z + 0.1 * i * dir.z,
                            0,
                            vec3.x,
                            vec3.y,
                            vec3.z,
                            10.0,
                            true
                        )
                    }
                    ParticleTool.sendParticle(
                        level, ParticleTypes.LAVA, location.x, location.y, location.z,
                        4, 0.0, 0.0, 0.0, 0.6, true
                    )
                    level.playSound(
                        null,
                        BlockPos(location.x.toInt(), location.y.toInt(), location.z.toInt()),
                        SoundEvents.LAVA_POP,
                        SoundSource.BLOCKS,
                        1f,
                        1f
                    )
                    this.discard()
                }
            }
        }
    }

    protected fun onHitBlock(location: Vec3, result: BlockHitResult) {
        val level = this.level()
        if (level is ServerLevel) {
            val pos = result.blockPos
            val face = result.direction
            val state = level().getBlockState(pos)

            if (NeoForge.EVENT_BUS.post(
                    HitBlock(
                        pos,
                        state,
                        face,
                        this.shooter,
                        this,
                        result.getLocation()
                    )
                ).isCanceled
            ) return

            val vx = face.stepX.toDouble()
            val vy = face.stepY.toDouble()
            val vz = face.stepZ.toDouble()
            val dir = Vec3(vx, vy, vz)

            if (this.beast) {
                ParticleTool.sendParticle(
                    level,
                    ParticleTypes.END_ROD,
                    location.x,
                    location.y,
                    location.z,
                    15,
                    0.1,
                    0.1,
                    0.1,
                    0.05,
                    true
                )
            } else {
                val bulletDecalOption = if (
                    this.entityData.get(COLOR_R) == DEFAULT_R
                    && this.entityData.get(COLOR_G) == DEFAULT_G
                    && this.entityData.get(COLOR_B) == DEFAULT_B
                ) {
                    BulletDecalOption(result.direction, result.blockPos)
                } else {
                    BulletDecalOption(
                        result.direction,
                        result.blockPos,
                        this.entityData.get(COLOR_R),
                        this.entityData.get(COLOR_G),
                        this.entityData.get(COLOR_B)
                    )
                }
                ParticleTool.sendParticle(
                    level,
                    bulletDecalOption,
                    location.x,
                    location.y,
                    location.z,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    true
                )
                summonVectorParticle(level, state, location, dir)

                this.discard()
            }
            level.playSound(
                null,
                BlockPos(location.x.toInt(), location.y.toInt(), location.z.toInt()),
                ModSounds.LAND.get(),
                SoundSource.BLOCKS,
                1f,
                1f
            )
        }
    }

    open fun summonVectorParticle(serverLevel: ServerLevel, state: BlockState, pos: Vec3, dir: Vec3) {
        val particleData = BlockParticleOption(ParticleTypes.BLOCK, state)
        for (i in 0..6) {
            val vec3 = randomVec(dir, 40.0)
            ParticleTool.sendParticle(
                serverLevel,
                particleData,
                pos.x + 0.05 * i * dir.x,
                pos.y + 0.05 * i * dir.y,
                pos.z + 0.05 * i * dir.z,
                0,
                vec3.x,
                vec3.y,
                vec3.z,
                10.0,
                true
            )
        }
        for (i in 0..2) {
            val vec3 = randomVec(dir, 20.0)
            ParticleTool.sendParticle(
                serverLevel,
                ParticleTypes.SMOKE,
                pos.x,
                pos.y,
                pos.z,
                0,
                vec3.x,
                vec3.y,
                vec3.z,
                0.05,
                true
            )
        }
        val blockPos = BlockPos.containing(pos)
        val soundType = state.getSoundType(serverLevel, blockPos, null)
        if (soundType === SoundType.METAL || soundType === SoundType.ANVIL || soundType === SoundType.CHAIN || soundType === SoundType.COPPER || soundType === SoundType.NETHERITE_BLOCK) {
            serverLevel.playSound(null, pos.x, pos.y, pos.z, ModSounds.HIT.get(), SoundSource.BLOCKS, 2f, 1f)
            for (i in 0..2) {
                val vec3 = randomVec(dir, 80.0)
                ParticleTool.sendParticle(
                    serverLevel,
                    ModParticleTypes.FIRE_STAR.get(),
                    pos.x,
                    pos.y,
                    pos.z,
                    0,
                    vec3.x,
                    vec3.y,
                    vec3.z,
                    0.2 + 0.1 * Math.random(),
                    true
                )
            }
        }
    }

    fun randomVec(vec3: Vec3, spread: Double): Vec3 {
        return vec3.normalize().add(
            this.random.triangle(0.0, 0.0172275 * spread),
            this.random.triangle(0.0, 0.0172275 * spread),
            this.random.triangle(0.0, 0.0172275 * spread)
        )
    }

    protected fun onHitEntity(entity: Entity?, result: ExtendedEntityRayTraceResult) {
        var entity = entity ?: return

        val headshot = result.headshot
        val legShot = result.legShot

        if (NeoForge.EVENT_BUS.post(HitEntity(this.shooter, this, result)).isCanceled()) return

        if (entity is PartEntity<*>) {
            entity = entity.getParent()
        }

        if (entity is LivingEntity) {
            entity.level().playSound(
                null,
                entity.onPos,
                ModSounds.MELEE_HIT.get(),
                SoundSource.PLAYERS,
                1f,
                (2 * Math.random() - 1).toFloat() * 0.1f + 1.0f
            )

            if (beast) {
                beastKill(this.shooter, entity)
                return
            }
        }

        this.damage *= (deltaMovement.length() / velocity).coerceIn(0.0, 1.0).toFloat()

        val shooter = this.shooter
        if (headshot) {
            if (shooter is ServerPlayer) {
                val holder = Holder.direct(ModSounds.HEADSHOT.get())
                sendPacketTo(
                    shooter, ClientboundSoundPacket(
                        holder,
                        SoundSource.PLAYERS,
                        shooter.x,
                        shooter.y,
                        shooter.z,
                        1f,
                        1f,
                        shooter.level().random.nextLong()
                    )
                )
                sendPacketTo(shooter, ClientIndicatorMessage(1, 5))
            }
            performOnHit(entity, this.damage, true, this.knockback.toDouble())
        } else {
            if (shooter is ServerPlayer) {
                val holder = Holder.direct(ModSounds.INDICATION.get())
                sendPacketTo(
                    shooter, ClientboundSoundPacket(
                        holder,
                        SoundSource.PLAYERS,
                        shooter.x,
                        shooter.y,
                        shooter.z,
                        1f,
                        1f,
                        shooter.level().random.nextLong()
                    )
                )
                sendPacketTo(shooter, ClientIndicatorMessage(0, 5))
            }

            if (legShot) {
                if (entity is LivingEntity) {
                    if (entity is Player && entity.isCreative) {
                        return
                    }
                    if (!entity.level().isClientSide()) {
                        entity.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false))
                    }
                }
                this.damage *= this.legShot
            }

            performOnHit(entity, this.damage, false, this.knockback.toDouble())
        }

        if (!this.mobEffects.isEmpty() && entity is LivingEntity) {
            for (instance in this.mobEffects) {
                entity.addEffect(MobEffectInstance(instance()), this.shooter)
            }
        }

        this.discard()
    }

    fun performOnHit(entity: Entity, damage: Float, headshot: Boolean, knockback: Double) {
        if (entity is LivingEntity) {
            if (this.forceKnockback) {
                val vec3 = this.deltaMovement.multiply(1.0, 0.0, 1.0).normalize()
                entity.addDeltaMovement(vec3.scale(knockback))
                performDamage(entity, damage, headshot)
            } else {
                val iCustomKnockback = ICustomKnockback.getInstance(entity)
                iCustomKnockback.`superbWarfare$setKnockbackStrength`(knockback)
                performDamage(entity, damage, headshot)
                iCustomKnockback.`superbWarfare$resetKnockbackStrength`()
            }
        } else {
            performDamage(entity, damage, headshot)
        }
    }

    protected fun explosionBullet(projectile: Entity, hitVec: Vec3) {
        CustomExplosion.Builder(projectile)
            .attacker(this.shooter)
            .damage(this.explosionDamage)
            .radius(this.explosionRadius)
            .position(hitVec)
            .explode()
    }

    override fun setDamage(damage: Float) {
        this.damage = damage
    }

    fun getDamage(): Float {
        return this.damage
    }

    open fun shoot(living: LivingEntity?, vecX: Double, vecY: Double, vecZ: Double, velocity: Float, spread: Float) {
        val vec3 = Vec3(vecX, vecY, vecZ).normalize()
            .add(
                this.random.triangle(0.0, 0.0172275 * spread.toDouble()),
                this.random.triangle(0.0, 0.0172275 * spread.toDouble()),
                this.random.triangle(0.0, 0.0172275 * spread.toDouble())
            ).scale(velocity.toDouble())
        this.deltaMovement = vec3
        val d0 = vec3.horizontalDistance()
        this.yRot = (Mth.atan2(vec3.x, vec3.z) * (180f / PI.toFloat()).toDouble()).toFloat()
        this.xRot = (Mth.atan2(vec3.y, d0) * (180f / PI.toFloat()).toDouble()).toFloat()
        this.yRotO = this.yRot
        this.xRotO = this.xRot
    }

    open fun updateHeading() {
        val horizontalDistance = this.deltaMovement.horizontalDistance()
        this.yRot = (Mth.atan2(
            this.deltaMovement.x(),
            this.deltaMovement.z()
        ) * (180.0 / PI)).toFloat()
        this.xRot = (Mth.atan2(this.deltaMovement.y(), horizontalDistance) * (180.0 / PI)).toFloat()
        this.yRotO = this.yRot
        this.xRotO = this.xRot
    }

    private fun performDamage(entity: Entity, damage: Float, isHeadshot: Boolean) {
        val rate = this.bypassArmorRate.coerceIn(0f, 1f)

        val normalDamage = damage * (1 - rate).coerceIn(0f, 1f)
        val absoluteDamage = damage * rate.coerceIn(0f, 1f)

        entity.invulnerableTime = 0

        val headShotModifier = if (isHeadshot) this.headShot else 1f
        // 先造成穿甲伤害
        if (absoluteDamage > 0) {
            entity.forceHurt(
                if (isHeadshot)
                    causeGunFireHeadshotAbsoluteDamage(this.level().registryAccess(), this, this.shooter)
                else
                    causeGunFireAbsoluteDamage(this.level().registryAccess(), this, this.shooter),
                absoluteDamage * headShotModifier
            )
            entity.invulnerableTime = 0

            // 大于1的穿甲对载具造成额外伤害
            if (entity is VehicleEntity && this.bypassArmorRate > 1) {
                entity.hurt(
                    causeGunFireAbsoluteDamage(this.level().registryAccess(), this, this.shooter),
                    absoluteDamage * (this.bypassArmorRate - 1) * 0.5f
                )
            }
        }
        if (normalDamage > 0) {
            entity.forceHurt(
                if (isHeadshot)
                    causeGunFireHeadshotDamage(this.level().registryAccess(), this, this.shooter)
                else
                    causeGunFireDamage(this.level().registryAccess(), this, this.shooter),
                normalDamage * headShotModifier
            )
            entity.invulnerableTime = 0
        }
    }

    override fun setGravity(gravity: Float) {
        this.gravity = gravity
    }

    override fun setExplosionDamage(explosionDamage: Float) {
        this.explosionDamage = explosionDamage
    }

    override fun setExplosionRadius(radius: Float) {
        this.explosionRadius = radius
    }

    /**
     * Builders
     */
    fun shooter(shooter: Entity?): ProjectileEntity {
        this.shooter = shooter
        return this
    }

    fun damage(damage: Float): ProjectileEntity {
        this.damage = damage
        return this
    }

    fun velocity(velocity: Float): ProjectileEntity {
        this.velocity = velocity
        return this
    }

    fun headShot(headShot: Float): ProjectileEntity {
        this.headShot = headShot
        return this
    }

    fun legShot(legShot: Float): ProjectileEntity {
        this.legShot = legShot
        return this
    }

    fun beast(): ProjectileEntity {
        this.beast = true
        return this
    }

    fun fireBullet(fireLevel: Int, dragonBreath: Boolean): ProjectileEntity {
        this.fireLevel = fireLevel
        this.dragonBreath = dragonBreath
        return this
    }

    fun zoom(zoom: Boolean): ProjectileEntity {
        this.isZoom = zoom
        return this
    }

    fun bypassArmorRate(bypassArmorRate: Float): ProjectileEntity {
        this.bypassArmorRate = bypassArmorRate
        return this
    }

    fun effect(mobEffectInstances: List<Supplier<MobEffectInstance>>): ProjectileEntity {
        this.mobEffects.addAll(mobEffectInstances)
        return this
    }

    fun setRGB(rgb: FloatArray) {
        this.entityData.set(COLOR_R, rgb[0])
        this.entityData.set(COLOR_G, rgb[1])
        this.entityData.set(COLOR_B, rgb[2])
    }

    fun knockback(knockback: Float): ProjectileEntity {
        this.knockback = knockback
        return this
    }

    fun forceKnockback(): ProjectileEntity {
        this.forceKnockback = true
        return this
    }

    fun setGunItemId(stack: ItemStack): ProjectileEntity {
        this.gunItemId = stack.descriptionId
        return this
    }

    fun setGunItemId(id: String?): ProjectileEntity {
        this.gunItemId = id
        return this
    }

    override fun setLife(life: Int) {
        this.life = life
    }

    companion object {
        @JvmField
        val COLOR_R: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(ProjectileEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val COLOR_G: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(ProjectileEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val COLOR_B: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(ProjectileEntity::class.java, EntityDataSerializers.FLOAT)

        private val PROJECTILE_TARGETS =
            Predicate { input: Entity? -> input != null && input.isPickable && !input.isSpectator && input.isAlive }
        private val IGNORE_LIST = Predicate { input: BlockState ->
            input.`is`(ModTags.Blocks.BULLET_IGNORE) && !(input.`is`(Blocks.IRON_DOOR) || input.`is`(Blocks.IRON_TRAPDOOR))
        }

        // 子弹的颜色
        const val DEFAULT_R: Float = 1.0f
        const val DEFAULT_G: Float = 222 / 255f
        const val DEFAULT_B: Float = 39 / 255f

        @JvmStatic
        fun rayTraceBlocks(
            world: Level,
            context: ClipContext,
            ignorePredicate: Predicate<BlockState>
        ): BlockHitResult {
            return performRayTrace(
                context, { rayTraceContext, blockPos ->
                    val blockState: BlockState = world.getBlockState(blockPos)
                    if (ignorePredicate.test(blockState)) return@performRayTrace null
                    val fluidState: FluidState = world.getFluidState(blockPos)
                    val startVec: Vec3 = rayTraceContext.from
                    val endVec: Vec3 = rayTraceContext.to
                    val blockShape: VoxelShape = rayTraceContext.getBlockShape(blockState, world, blockPos)
                    val blockResult: BlockHitResult? =
                        world.clipWithInteractionOverride(startVec, endVec, blockPos, blockShape, blockState)
                    val fluidShape: VoxelShape = rayTraceContext.getFluidShape(fluidState, world, blockPos)
                    val fluidResult: BlockHitResult? = fluidShape.clip(startVec, endVec, blockPos)
                    val blockDistance =
                        if (blockResult == null) Double.MAX_VALUE else rayTraceContext.from
                            .distanceToSqr(blockResult.getLocation())
                    val fluidDistance =
                        if (fluidResult == null) Double.MAX_VALUE else rayTraceContext.from
                            .distanceToSqr(fluidResult.getLocation())
                    if (blockDistance <= fluidDistance) blockResult else fluidResult
                },
                { rayTraceContext ->
                    val vec3 = rayTraceContext.from.subtract(rayTraceContext.to)
                    BlockHitResult.miss(
                        rayTraceContext.to,
                        Direction.getNearest(vec3.x, vec3.y, vec3.z),
                        BlockPos.containing(rayTraceContext.to)
                    )
                })
        }

        private fun <T> performRayTrace(
            context: ClipContext,
            hitFunction: BiFunction<ClipContext, BlockPos, T?>,
            function: Function<ClipContext, T>
        ): T {
            val startVec = context.from
            val endVec = context.to
            if (startVec != endVec) {
                val startX = Mth.lerp(-0.0000001, endVec.x, startVec.x)
                val startY = Mth.lerp(-0.0000001, endVec.y, startVec.y)
                val startZ = Mth.lerp(-0.0000001, endVec.z, startVec.z)
                val endX = Mth.lerp(-0.0000001, startVec.x, endVec.x)
                val endY = Mth.lerp(-0.0000001, startVec.y, endVec.y)
                val endZ = Mth.lerp(-0.0000001, startVec.z, endVec.z)
                var blockX = Mth.floor(endX)
                var blockY = Mth.floor(endY)
                var blockZ = Mth.floor(endZ)
                val mutablePos = MutableBlockPos(blockX, blockY, blockZ)
                val t = hitFunction.apply(context, mutablePos)
                if (t != null) {
                    return t
                }

                val deltaX = startX - endX
                val deltaY = startY - endY
                val deltaZ = startZ - endZ
                val signX = Mth.sign(deltaX)
                val signY = Mth.sign(deltaY)
                val signZ = Mth.sign(deltaZ)
                val d9 = if (signX == 0) Double.MAX_VALUE else signX.toDouble() / deltaX
                val d10 = if (signY == 0) Double.MAX_VALUE else signY.toDouble() / deltaY
                val d11 = if (signZ == 0) Double.MAX_VALUE else signZ.toDouble() / deltaZ
                var d12 = d9 * (if (signX > 0) 1 - Mth.frac(endX) else Mth.frac(endX))
                var d13 = d10 * (if (signY > 0) 1 - Mth.frac(endY) else Mth.frac(endY))
                var d14 = d11 * (if (signZ > 0) 1 - Mth.frac(endZ) else Mth.frac(endZ))

                while (d12 <= 1 || d13 <= 1 || d14 <= 1) {
                    if (d12 < d13) {
                        if (d12 < d14) {
                            blockX += signX
                            d12 += d9
                        } else {
                            blockZ += signZ
                            d14 += d11
                        }
                    } else if (d13 < d14) {
                        blockY += signY
                        d13 += d10
                    } else {
                        blockZ += signZ
                        d14 += d11
                    }

                    val t1 = hitFunction.apply(context, mutablePos.set(blockX, blockY, blockZ))
                    if (t1 != null) {
                        return t1
                    }
                }
            }
            return function.apply(context)
        }
    }
}
