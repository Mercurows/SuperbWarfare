package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent.HitBlock
import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent.HitEntity
import com.atsuishio.superbwarfare.client.particle.BulletDecalOption
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.config.server.ProjectileConfig
import com.atsuishio.superbwarfare.entity.OBBEntity
import com.atsuishio.superbwarfare.entity.living.DPSGeneratorEntity
import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.entity.mixin.OBBHitter
import com.atsuishio.superbwarfare.entity.projectile.IAdvancedHitDetection.Companion.rayTraceBlocks
import com.atsuishio.superbwarfare.entity.projectile.IBulletProperties.Companion.DEFAULT_B
import com.atsuishio.superbwarfare.entity.projectile.IBulletProperties.Companion.DEFAULT_G
import com.atsuishio.superbwarfare.entity.projectile.IBulletProperties.Companion.DEFAULT_R
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeGunFireAbsoluteDamage
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeGunFireDamage
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeGunFireHeadshotAbsoluteDamage
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeGunFireHeadshotDamage
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.item.weapon.BeastItem.Companion.beastKill
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.tools.HitboxHelper.getBoundingBox
import com.atsuishio.superbwarfare.tools.HitboxHelper.getVelocity
import com.atsuishio.superbwarfare.tools.VectorTool.isInLiquid
import com.atsuishio.superbwarfare.world.phys.EntityResult
import com.atsuishio.superbwarfare.world.phys.ExtendedEntityRayTraceResult
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
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
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.entity.PartEntity
import java.util.function.Predicate
import kotlin.math.PI
import kotlin.math.max

@Suppress("unused")
open class ProjectileEntity(entityType: EntityType<out ProjectileEntity>, level: Level) : Projectile(entityType, level),
    IBulletProperties, IAdvancedHitDetection, IFastMotionSync {
    // ===== IBulletProperties 属性（使用 getter/setter 方法） =====
    protected var damageValue = 1f
    protected var headShotValue = 1f
    protected var legShotValue = 0.5f
    protected var beastValue = false
    protected var isZoomValue: Boolean = false
    protected var explosionDamageValue = 0.0f
    protected var explosionRadiusValue = 0.0f
    protected var fireLevelValue = 0
    protected var dragonBreathValue = false
    protected var knockbackValue = 0.05f
    protected var velocityValue = 20f
    protected var forceKnockbackValue = false
    protected var lifeValue = 40
    // 子弹的穿甲比例
    protected var bypassArmorRateValue = 0.0f
    // 是否能穿墙
    protected var penetratingValue: Boolean = false

    override fun getDamage(): Float = damageValue
    override fun setDamage(value: Float) {
        damageValue = value
    }

    override fun getHeadShot(): Float = headShotValue
    override fun setHeadShot(value: Float) {
        headShotValue = value
    }

    override fun getLegShot(): Float = legShotValue
    override fun setLegShot(value: Float) {
        legShotValue = value
    }

    override fun isBeast(): Boolean = beastValue
    override fun setBeast(value: Boolean) {
        beastValue = value
    }

    override fun isZoom(): Boolean = isZoomValue
    override fun setZoom(value: Boolean) {
        isZoomValue = value
    }

    override fun getExplosionDamage(): Float = explosionDamageValue
    override fun setExplosionDamage(value: Float) {
        explosionDamageValue = value
    }

    override fun getExplosionRadius(): Float = explosionRadiusValue
    override fun setExplosionRadius(value: Float) {
        explosionRadiusValue = value
    }

    override fun getFireLevel(): Int = fireLevelValue
    override fun setFireLevel(value: Int) {
        fireLevelValue = value
    }

    override fun isDragonBreath(): Boolean = dragonBreathValue
    override fun setDragonBreath(value: Boolean) {
        dragonBreathValue = value
    }

    override fun getKnockback(): Float = knockbackValue
    override fun setKnockback(value: Float) {
        knockbackValue = value
    }

    override fun getVelocity(): Float = velocityValue
    override fun setVelocity(value: Float) {
        velocityValue = value
    }

    override fun isForceKnockback(): Boolean = forceKnockbackValue
    override fun setForceKnockback(value: Boolean) {
        forceKnockbackValue = value
    }

    override fun getLife(): Int = lifeValue
    override fun setLife(value: Int) {
        lifeValue = value
    }

    override fun getBypassArmorRate(): Float = bypassArmorRateValue
    override fun setBypassArmorRate(value: Float) {
        bypassArmorRateValue = value
    }

    override fun isPenetrating(): Boolean = penetratingValue
    override fun setPenetrating(value: Boolean) {
        penetratingValue = value
    }

    // 子弹造成的状态效果
    private val mobEffects = arrayListOf<MobEffectInstance>()

    // 发射子弹的武器ID
    var gunItemId: String? = null
        private set

    // 重力（非接口属性，保留私有）
    private var gravity = 0.05f

    init {
        this.noCulling = true
    }

    constructor(level: Level) : this(ModEntities.PROJECTILE.get(), level)

    /**
     * From TaC-Z
     */
    override fun getHitResult(entity: Entity, startVec: Vec3, endVec: Vec3): EntityResult? {
        val expandHeight = if (entity is Player && !entity.isCrouching) 0.0625 else 0.0

        var hitPos: Vec3? = null
        if (entity is OBBEntity && !entity.enableAABB()) {
            for (obb in entity.getOBBs()) {
                if (obb.part == OBB.Part.COLLISION) continue
                val obbVec = obb.clip(startVec.toVector3d(), endVec.toVector3d()).orElse(null) ?: continue
                hitPos = obbVec.toVec3()
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
        } else {
            var boundingBox = entity.boundingBox
            var velocity = Vec3(entity.x - entity.xOld, entity.y - entity.yOld, entity.z - entity.zOld)

            val shooter = this.owner
            if (entity is ServerPlayer && shooter is ServerPlayer) {
                val ping = Mth.floor((shooter.latency / 1000.0) * 20.0 + 0.5)
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

            if (this.isBeast()) {
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

        return EntityResult(entity, hitPos, headshot, legShot)
    }

    override fun defineSynchedData() {
        this.entityData.define(COLOR_R, DEFAULT_R)
        this.entityData.define(COLOR_G, DEFAULT_G)
        this.entityData.define(COLOR_B, DEFAULT_B)
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
                    level,
                    ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this),
                    if (this.isPenetrating() || this.isBeast()) Predicate { true } else if (ProjectileConfig.PROJECTILE_DESTROY_BLOCKS.get()) IGNORE_LIST.and(
                        Predicate { input -> !input.`is`(ModTags.Blocks.BULLET_CAN_DESTROY) }) else IGNORE_LIST
                )

            val fluidResult: BlockHitResult =
                rayTraceBlocks(
                    level,
                    ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this),
                    if (this.isPenetrating() || this.isBeast()) Predicate { true } else if (ProjectileConfig.PROJECTILE_DESTROY_BLOCKS.get()) IGNORE_LIST.and(
                        Predicate { input -> !input.`is`(ModTags.Blocks.BULLET_CAN_DESTROY) }) else IGNORE_LIST
                )

            if (result != null && result.type != HitResult.Type.MISS) {
                endVec = result.getLocation()
            }

            val entityResults = findEntitiesOnPath(startVec, endVec)
            if (this.owner != null) {
                entityResults.sortBy { it.hitVec.distanceTo(this.owner!!.position()) }
            }

            for (entityResult in entityResults) {
                result = ExtendedEntityRayTraceResult(entityResult)
                val resEntity = result.entity
                val shooter = this.owner
                if (resEntity is Player) {
                    if (shooter is Player && !shooter.canHarmPlayer(resEntity)) {
                        result = null
                    }
                }
                if (result != null) {
                    this.onHit(result)
                }

                if (!this.isBeast()) {
                    this.bypassArmorRateValue -= 0.2f
                    if (this.bypassArmorRateValue < 0.8f) {
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

        if (this.tickCount > lifeValue) {
            this.discard()
        }

        if (fireLevelValue > 0 && dragonBreathValue && level is ServerLevel) {
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

    override fun onHit(result: HitResult) {
        if (result is BlockHitResult) {
            val level = this.level()
            if (result.type == HitResult.Type.MISS) {
                return
            }
            val resultPos = result.blockPos
            val state = level.getBlockState(resultPos)
            val event = state.block.getSoundType(state, level, resultPos, this).breakSound

            val hitVec = result.location
            level.playSound(
                null,
                hitVec.x,
                hitVec.y,
                hitVec.z,
                event,
                SoundSource.AMBIENT,
                1f,
                1f
            )

            level.gameEvent(
                GameEvent.PROJECTILE_LAND,
                hitVec,
                GameEvent.Context.of(this, state)
            )

            this.onHitBlock(result)

            if (fireLevelValue > 0 && level is ServerLevel) {
                ParticleTool.sendParticle(
                    level, ParticleTypes.LAVA, hitVec.x, hitVec.y, hitVec.z,
                    3, 0.0, 0.0, 0.0, 0.5, true
                )
            }
        }

        if (result is ExtendedEntityRayTraceResult) {
            val entity = result.entity
            if (entity == this.owner) {
                return
            }

            if (this.owner is Player) {
                if (entity.hasIndirectPassenger(this.owner!!)) {
                    return
                }
            }

            this.level().gameEvent(
                GameEvent.PROJECTILE_LAND,
                result.location,
                GameEvent.Context.of(this, null)
            )

            this.onHitEntity(result)
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

    override fun onHitBlock(result: BlockHitResult) {
        val level = this.level()
        val pos = result.blockPos
        val face = result.direction
        val state = level.getBlockState(pos)
        val location = result.location

        if (postEvent(HitBlock(pos, state, face, this.owner, this, location))) return

        state.onProjectileHit(level, state, result, this)

        if (level is ServerLevel) {
            if (this.explosionDamageValue > 0) {
                CustomExplosion.Builder(this)
                    .attacker(this.owner)
                    .damage(this.explosionDamageValue)
                    .radius(this.explosionRadiusValue)
                    .position(location)
                    .beast(this.isBeast())
                    .explode()
            }

            val vx = face.stepX.toDouble()
            val vy = face.stepY.toDouble()
            val vz = face.stepZ.toDouble()
            val dir = Vec3(vx, vy, vz)

            if (this.isBeast()) {
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
                val bulletDecalOption = BulletDecalOption(
                    result.direction,
                    result.blockPos,
                    this.entityData.get(COLOR_R),
                    this.entityData.get(COLOR_G),
                    this.entityData.get(COLOR_B)
                )

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
        val soundType = state.soundType
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

    override fun onHitEntity(result: EntityHitResult) {
        if (result !is ExtendedEntityRayTraceResult) return

        var entity = result.entity ?: return
        val headshot = result.headshot
        val legShot = result.legShot

        if (postEvent(HitEntity(this.owner, this, result))) return

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

            if (isBeast()) {
                beastKill(this.owner, entity)
                return
            }
        }

        this.damageValue *= (deltaMovement.length() / velocityValue).coerceIn(0.0, 1.0).toFloat()

        val shooter = this.owner
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
            performOnHit(entity, this.damageValue, true, this.knockbackValue.toDouble())
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
                this.damageValue *= this.legShotValue
            }

            performOnHit(entity, this.damageValue, false, this.knockbackValue.toDouble())
        }

        if (!this.mobEffects.isEmpty() && entity is LivingEntity) {
            for (instance in this.mobEffects) {
                entity.addEffect(instance, shooter)
            }
        }

        if (this.explosionDamageValue > 0) {
            CustomExplosion.Builder(this)
                .attacker(shooter)
                .damage(this.explosionDamageValue)
                .radius(this.explosionRadiusValue)
                .position(result.location)
                .beast(this.isBeast())
                .explode()
        }

        this.discard()
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

    override fun performDamage(entity: Entity, damage: Float, isHeadshot: Boolean) {
        val rate = this.bypassArmorRateValue.coerceIn(0f, 1f)

        val normalDamage = damage * (1 - rate).coerceIn(0f, 1f)
        val absoluteDamage = damage * rate.coerceIn(0f, 1f)

        entity.invulnerableTime = 0

        val headShotModifier = if (isHeadshot) this.headShotValue else 1f
        // 先造成穿甲伤害
        if (absoluteDamage > 0) {
            entity.forceHurt(
                if (isHeadshot)
                    causeGunFireHeadshotAbsoluteDamage(this.level().registryAccess(), this, this.owner)
                else
                    causeGunFireAbsoluteDamage(this.level().registryAccess(), this, this.owner),
                absoluteDamage * headShotModifier
            )
            entity.invulnerableTime = 0

            // 大于1的穿甲对载具造成额外伤害
            if (entity is VehicleEntity && this.bypassArmorRateValue > 1) {
                entity.hurt(
                    causeGunFireAbsoluteDamage(this.level().registryAccess(), this, this.owner),
                    absoluteDamage * (this.bypassArmorRateValue - 1) * 0.5f
                )
            }
        }
        if (normalDamage > 0) {
            entity.forceHurt(
                if (isHeadshot)
                    causeGunFireHeadshotDamage(this.level().registryAccess(), this, this.owner)
                else
                    causeGunFireDamage(this.level().registryAccess(), this, this.owner),
                normalDamage * headShotModifier
            )
            entity.invulnerableTime = 0
        }
    }

    /**
     * Builders
     */
    fun shooter(shooter: Entity?): ProjectileEntity {
        this.owner = shooter
        return this
    }

    fun damage(damage: Float): ProjectileEntity {
        this.damageValue = damage
        return this
    }

    fun velocity(velocity: Float): ProjectileEntity {
        this.velocityValue = velocity
        return this
    }

    fun headShot(headShot: Float): ProjectileEntity {
        this.headShotValue = headShot
        return this
    }

    fun legShot(legShot: Float): ProjectileEntity {
        this.legShotValue = legShot
        return this
    }

    fun beast(): ProjectileEntity {
        this.beastValue = true
        return this
    }

    fun fireBullet(fireLevel: Int, dragonBreath: Boolean): ProjectileEntity {
        this.fireLevelValue = fireLevel
        this.dragonBreathValue = dragonBreath
        return this
    }

    fun zoom(zoom: Boolean): ProjectileEntity {
        this.isZoomValue = zoom
        return this
    }

    fun bypassArmorRate(bypassArmorRate: Float): ProjectileEntity {
        this.bypassArmorRateValue = bypassArmorRate
        return this
    }

    fun effect(mobEffectInstances: List<MobEffectInstance>): ProjectileEntity {
        this.mobEffects.addAll(mobEffectInstances)
        return this
    }

    // ===== IBulletProperties 复合方法 =====

    override fun setRGB(rgb: FloatArray) {
        this.entityData.set(COLOR_R, rgb[0])
        this.entityData.set(COLOR_G, rgb[1])
        this.entityData.set(COLOR_B, rgb[2])
    }

    override fun getRGB(): FloatArray = floatArrayOf(
        this.entityData.get(COLOR_R),
        this.entityData.get(COLOR_G),
        this.entityData.get(COLOR_B)
    )

    override fun setEffects(effects: List<MobEffectInstance>) {
        this.mobEffects.addAll(effects)
    }

    override fun setCustomGravity(gravity: Float) {
        this.gravity = gravity
    }

    // ===== Builder methods (return ProjectileEntity for chaining) =====
    fun setFireBullet(fireLevel: Int, dragonBreath: Boolean) {
        this.fireLevelValue = fireLevel
        this.dragonBreathValue = dragonBreath
    }

    fun knockback(knockback: Float): ProjectileEntity {
        this.knockbackValue = knockback
        return this
    }

    fun forceKnockback(): ProjectileEntity {
        this.forceKnockbackValue = true
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

    companion object {
        @JvmField
        val PROJECTILE_TARGETS_FAST =
            Predicate { input: Entity? -> input != null && input.isPickable && !input.isSpectator && input.isAlive }

        @JvmField
        val COLOR_R: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(ProjectileEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val COLOR_G: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(ProjectileEntity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val COLOR_B: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(ProjectileEntity::class.java, EntityDataSerializers.FLOAT)

        private val IGNORE_LIST = Predicate { input: BlockState ->
            input.`is`(ModTags.Blocks.BULLET_IGNORE) && !(input.`is`(Blocks.IRON_DOOR) || input.`is`(Blocks.IRON_TRAPDOOR))
        }
    }
}
