package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.NBTTool
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.players.OldUsersConverter
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.*
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.*
import net.neoforged.neoforge.items.ItemHandlerHelper
import java.util.*
import kotlin.math.min
import kotlin.math.sqrt

open class C4Entity : Entity, OwnableEntity {
    protected var inGround: Boolean = false
    protected var onEntity: Boolean = false
    private var lastState: BlockState? = null

    constructor(type: EntityType<C4Entity>, level: Level) : super(type, level)

    @JvmOverloads
    constructor(owner: LivingEntity?, level: Level, isControllable: Boolean = false) : super(
        ModEntities.C4.get(),
        level
    ) {
        if (owner != null) {
            this.setOwnerUUID(owner.getUUID())
        }
        this.entityData.set(IS_CONTROLLABLE, isControllable)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        with(builder) {
            define(OWNER_UUID, Optional.empty())
            define(LAST_ATTACKER_UUID, "undefined")
            define(TARGET_UUID, "undefined")
            define(IS_CONTROLLABLE, false)
            define(BOMB_TICK, 0)
        }
    }

    fun setOwnerUUID(pUuid: UUID?) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(pUuid))
    }

    override fun getOwnerUUID(): UUID? {
        return this.entityData.get(OWNER_UUID).orElse(null)
    }

    public override fun addAdditionalSaveData(compound: CompoundTag) {
        compound.putString("Target", this.entityData.get(TARGET_UUID))
        compound.putString("LastAttacker", this.entityData.get(LAST_ATTACKER_UUID))
        compound.putBoolean("IsControllable", this.entityData.get(IS_CONTROLLABLE))
        compound.putInt("BombTick", this.entityData.get(BOMB_TICK))

        if (this.lastState != null) {
            compound.put("InBlockState", NbtUtils.writeBlockState(this.lastState!!))
        }

        if (this.ownerUUID != null) {
            compound.putUUID("Owner", this.ownerUUID!!)
        }
    }

    public override fun readAdditionalSaveData(compound: CompoundTag) {
        if (compound.contains("LastAttacker")) {
            this.entityData.set(LAST_ATTACKER_UUID, compound.getString("LastAttacker"))
        }

        if (compound.contains("Target")) {
            this.entityData.set(TARGET_UUID, compound.getString("Target"))
        }

        if (compound.contains("InBlockState", 10)) {
            this.lastState = NbtUtils.readBlockState(
                this.level().holderLookup(Registries.BLOCK),
                compound.getCompound("InBlockState")
            )
        }

        if (compound.contains("IsControllable")) {
            this.entityData.set(IS_CONTROLLABLE, compound.getBoolean("IsControllable"))
        }

        if (compound.contains("BombTick")) {
            this.entityData.set(BOMB_TICK, compound.getInt("BombTick"))
        }

        var uuid: UUID?
        if (compound.hasUUID("Owner")) {
            uuid = compound.getUUID("Owner")
        } else {
            val s = compound.getString("Owner")

            try {
                uuid = if (this.server == null) {
                    UUID.fromString(s)
                } else {
                    OldUsersConverter.convertMobOwnerIfNecessary(this.server!!, s)
                }
            } catch (exception: Exception) {
                Mod.LOGGER.error("Couldn't load owner UUID of {}: {}", this, exception)
                uuid = null
            }
        }

        if (uuid != null) {
            try {
                this.setOwnerUUID(uuid)
            } catch (_: Throwable) {
            }
        }
    }

    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        if (this.owner === player && player.isShiftKeyDown) {
            if (!this.level().isClientSide()) {
                this.discard()
            }

            if (!player.abilities.instabuild) {
                ItemHandlerHelper.giveItemToPlayer(player, this.itemStack)
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide())
        }
        return InteractionResult.PASS
    }

    override fun tick() {
        super.tick()

        if (!this.entityData.get(IS_CONTROLLABLE)) {
            val bombTick = this.entityData.get(BOMB_TICK)

            if (bombTick >= ExplosionConfig.C4_EXPLOSION_COUNTDOWN.get()) {
                this.explode()
            }

            val countdown = ExplosionConfig.C4_EXPLOSION_COUNTDOWN.get()
            if (countdown - bombTick > 39 && bombTick % ((20 * (countdown - bombTick)) / countdown + 1) == 0) {
                this.level().playSound(null, this.onPos, ModSounds.C4_BEEP.get(), SoundSource.PLAYERS, 1f, 1f)
            }

            if (bombTick == countdown - 39) {
                this.level().playSound(null, this.onPos, ModSounds.C4_FINAL.get(), SoundSource.PLAYERS, 2f, 1f)
            }
            this.entityData.set(BOMB_TICK, bombTick + 1)
        }

        var motion = this.deltaMovement
        if (this.xRotO == 0f && this.yRotO == 0f && !this.inGround) {
            val d0 = motion.horizontalDistance()
            this.yRot = (Mth.atan2(motion.x, motion.z) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
            this.xRot = (Mth.atan2(motion.y, d0) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
            this.yRotO = this.yRot
            this.xRotO = this.xRot
        }

        val blockpos = this.blockPosition()
        val blockstate = this.level().getBlockState(blockpos)
        if (!blockstate.isAir) {
            val voxelShape = blockstate.getCollisionShape(this.level(), blockpos)
            if (!voxelShape.isEmpty) {
                val vec31 = this.position()

                for (aabb in voxelShape.toAabbs()) {
                    if (aabb.move(blockpos).contains(vec31)) {
                        this.inGround = true
                        break
                    }
                }
            }
        }

        if (this.inGround) {
            if (this.lastState !== blockstate && this.shouldFall()) {
                this.startFalling()
            }
        } else if (!this.onEntity) {
            val position = this.position()
            var nextPosition = position.add(motion)
            var hitResult: HitResult? = this.level()
                .clip(ClipContext(position, nextPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
            if (hitResult!!.type != HitResult.Type.MISS) {
                nextPosition = hitResult.getLocation()
            }

            while (!this.isRemoved) {
                val entityHitResult = this.findHitEntity(position, nextPosition)
                if (entityHitResult != null) {
                    hitResult = entityHitResult
                }

                if (hitResult != null && hitResult.type != HitResult.Type.MISS) {
                    this.onHit(hitResult)
                    this.hasImpulse = true
                    break
                }

                if (entityHitResult == null) {
                    break
                }

                hitResult = null
            }

            if (this.isRemoved) {
                return
            }

            motion = this.deltaMovement
            val pX = motion.x
            val pY = motion.y
            val pZ = motion.z

            val nX = this.x + pX
            val nY = this.y + pY
            val nZ = this.z + pZ

            this.updateRotation()

            var f = 0.99f
            if (this.isInWater) {
                repeat(3) {
                    this.level()
                        .addParticle(ParticleTypes.BUBBLE, nX - pX * 0.25, nY - pY * 0.25, nZ - pZ * 0.25, pX, pY, pZ)
                }

                f = this.waterInertia
            }

            this.deltaMovement = motion.scale(f.toDouble())
            if (!this.isNoGravity) {
                val vec34 = this.deltaMovement
                this.setDeltaMovement(vec34.x, vec34.y - 0.05, vec34.z)
            }

            this.setPos(nX, nY, nZ)
            this.checkInsideBlocks()
        } else {
            val target = EntityFindUtil.findEntity(level(), entityData.get(TARGET_UUID))
            if (target != null) {
                this.setPos(target.x, target.y + target.bbHeight, target.z)
            } else {
                this.onEntity = false
            }
        }

        this.refreshDimensions()
    }

    private fun shouldFall(): Boolean {
        return this.inGround && this.level().noCollision((AABB(this.position(), this.position())).inflate(0.06))
    }

    private fun startFalling() {
        this.inGround = false
        val vec3 = this.deltaMovement
        this.deltaMovement = vec3.multiply(
            (this.random.nextFloat() * 0.2f).toDouble(),
            (this.random.nextFloat() * 0.2f).toDouble(),
            (this.random.nextFloat() * 0.2f).toDouble()
        )
    }

    override fun move(pType: MoverType, pPos: Vec3) {
        super.move(pType, pPos)
        if (pType != MoverType.SELF && this.shouldFall()) {
            this.startFalling()
        }
    }

    fun look(pTarget: Vec3) {
        val d0 = pTarget.x
        val d1 = pTarget.y
        val d2 = pTarget.z
        val d3 = sqrt(d0 * d0 + d2 * d2)
        xRot = Mth.wrapDegrees((-(Mth.atan2(d1, d3) * 57.2957763671875)).toFloat())
        yHeadRot = yRot
        this.xRotO = xRot
        this.yRotO = yRot
    }

    protected fun updateRotation() {
        if (deltaMovement.length() > 0.05 && !inGround && !onEntity) {
            val vec3 = this.deltaMovement
            val d0 = vec3.horizontalDistance()
            this.xRot = lerpRotation(
                this.xRotO,
                (Mth.atan2(vec3.y, d0) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
            )
            this.yRot = lerpRotation(
                this.yRotO,
                (Mth.atan2(vec3.x, vec3.z) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
            )
        }
    }

    protected fun findHitEntity(pStartVec: Vec3, pEndVec: Vec3): EntityHitResult? {
        return ProjectileUtil.getEntityHitResult(
            this.level(),
            this,
            pStartVec,
            pEndVec,
            this.boundingBox.expandTowards(this.deltaMovement).inflate(1.0)
        ) { this.canHitEntity(it) }
    }

    protected fun canHitEntity(pTarget: Entity): Boolean {
        if (!pTarget.canBeHitByProjectile()) {
            return false
        } else {
            val entity: Entity? = this.owner
            return entity == null
                    || (entity === pTarget && this.tickCount > 2)
                    || !entity.isPassengerOfSameVehicle(pTarget)
        }
    }

    protected fun onHit(pResult: HitResult) {
        when (pResult.type) {
            HitResult.Type.BLOCK -> this.onHitBlock(pResult as BlockHitResult)
            HitResult.Type.ENTITY -> this.onHitEntity(pResult as EntityHitResult)
            else -> {}
        }
    }

    protected fun onHitEntity(pResult: EntityHitResult) {
        val entity = pResult.entity
        if (tickCount < 2 || entity === this.vehicle || entity is C4Entity) return
        this.entityData.set(TARGET_UUID, entity.getStringUUID())
        this.onEntity = true
        this.deltaMovement = this.deltaMovement.multiply(0.0, 0.0, 0.0)
        this.xRot = -90f
        this.xRotO = this.xRot
    }

    protected fun onHitBlock(pResult: BlockHitResult) {
        this.lastState = this.level().getBlockState(pResult.blockPos)
        val vec3 = pResult.getLocation().subtract(this.x, this.y, this.z)
        this.deltaMovement = vec3
        val vec31 = vec3.normalize().scale(0.05)
        this.setPosRaw(this.x - vec31.x, this.y - vec31.y, this.z - vec31.z)

        this.look(Vec3.atLowerCornerOf(pResult.direction.normal))
        this.yRot = (pResult.direction.get2DDataValue() * 90).toFloat()

        val resultPos = pResult.blockPos
        val state = this.level().getBlockState(resultPos)
        val event = state.block.getSoundType(state, this.level(), resultPos, this).breakSound
        val speed = this.deltaMovement.length()
        if (speed > 0.1) {
            val volume = min(4f, speed.toFloat() / 4f + 0.5f)
            this.level().playSound(
                null,
                pResult.getLocation().x,
                pResult.getLocation().y,
                pResult.getLocation().z,
                event,
                SoundSource.AMBIENT,
                volume,
                1f
            )
        }
        this.inGround = true
    }

    fun explode() {
        var pos = position()

        if (onEntity) {
            val target = EntityFindUtil.findEntity(level(), entityData.get(TARGET_UUID))
            if (target != null) {
                pos = target.position()
            }
        }

        if (this.level() is ServerLevel && ExplosionConfig.EXPLOSION_DESTROY.get() && ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
            val aabb = AABB(pos, pos).inflate(2.0)
            BlockPos.betweenClosedStream(aabb).toList().forEach {
                val hard = this.level().getBlockState(it).block.defaultDestroyTime()
                if (hard != -1f) {
                    this.level().destroyBlock(it, true)
                }
            }
        }

        CustomExplosion.Builder(this)
            .attacker(this.owner)
            .damage(ExplosionConfig.C4_EXPLOSION_DAMAGE.get().toFloat())
            .radius(ExplosionConfig.C4_EXPLOSION_RADIUS.get().toFloat())
            .position(pos)
            .withParticleType(ParticleTool.ParticleType.HUGE)
            .explode()

        this.discard()
    }

    override fun getDimensions(pPose: Pose): EntityDimensions {
        return super.getDimensions(pPose).scale(0.5f)
    }

    protected val waterInertia: Float
        get() = 0.6f

    override fun isPickable(): Boolean {
        return true
    }

    val itemStack: ItemStack
        get() {
            val stack = ItemStack(ModItems.C4_BOMB.get())
            if (this.getEntityData().get(IS_CONTROLLABLE)) {
                val tag = NBTTool.getTag(stack)
                tag.putBoolean("Control", true)
                NBTTool.saveTag(stack, tag)
            }
            return stack
        }

    fun defuse() {
        this.discard()
        val entity = ItemEntity(this.level(), this.x, this.y, this.z, this.itemStack)
        if (!this.level().isClientSide) {
            this.level().addFreshEntity(entity)
        }
    }

    val bombTick: Int
        get() = this.entityData.get(BOMB_TICK)

    companion object {
        @JvmField
        protected val OWNER_UUID: EntityDataAccessor<Optional<UUID>> =
            SynchedEntityData.defineId(C4Entity::class.java, EntityDataSerializers.OPTIONAL_UUID)

        @JvmField
        protected val LAST_ATTACKER_UUID: EntityDataAccessor<String> =
            SynchedEntityData.defineId(C4Entity::class.java, EntityDataSerializers.STRING)

        @JvmField
        protected val TARGET_UUID: EntityDataAccessor<String> =
            SynchedEntityData.defineId(C4Entity::class.java, EntityDataSerializers.STRING)

        @JvmField
        val IS_CONTROLLABLE: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(C4Entity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val BOMB_TICK: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(C4Entity::class.java, EntityDataSerializers.INT)

        const val DEFAULT_DEFUSE_PROGRESS: Int = 100

        @JvmStatic
        protected fun lerpRotation(pCurrentRotation: Float, pTargetRotation: Float): Float {
            var pCurrentRotation = pCurrentRotation
            while (pTargetRotation - pCurrentRotation < -180f) {
                pCurrentRotation -= 360f
            }

            while (pTargetRotation - pCurrentRotation >= 180f) {
                pCurrentRotation += 360f
            }

            return Mth.lerp(0.2f, pCurrentRotation, pTargetRotation)
        }
    }
}
