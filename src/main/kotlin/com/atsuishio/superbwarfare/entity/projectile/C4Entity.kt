package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.OBBEntity
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.*
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
import net.minecraftforge.items.ItemHandlerHelper
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.*
import kotlin.math.min
import kotlin.math.sqrt

open class C4Entity : Entity, OwnableEntity {
    protected var inGround: Boolean = false
    var onEntity by ON_ENTITY
    private var lastState: BlockState? = null

    // Previous-tick quaternion for interpolation
    protected var qxO = 0f
    protected var qyO = 0f
    protected var qzO = 0f
    protected var qwO = 1f

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

    override fun defineSynchedData() {
        this.entityData.define(OWNER_UUID, Optional.empty())
        this.entityData.define(LAST_ATTACKER_UUID, "undefined")
        this.entityData.define(TARGET_UUID, "undefined")
        this.entityData.define(IS_CONTROLLABLE, false)
        this.entityData.define(BOMB_TICK, 0)
        this.entityData.define(STICKY_ON_OBB, false)
        this.entityData.define(STICKY_OBB_LOCAL_POS, Vector3f())
        this.entityData.define(STICKY_OBB_FACE, 0)
        this.entityData.define(STICKY_OBB_INDEX, -1)
        this.entityData.define(STICKY_Y_OFFSET, 0f)
        this.entityData.define(ON_ENTITY, false)
        this.entityData.define(QUATERNION_VEC, Vector3f())
        this.entityData.define(QUATERNION_W, 1f)
    }

    fun setOwnerUUID(pUuid: UUID?) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(pUuid))
    }

    override fun getOwnerUUID(): UUID? {
        return this.entityData.get(OWNER_UUID).orElse(null)
    }

    // Quaternion synched data delegates
    var quaternionVec by QUATERNION_VEC
    var quaternionW by QUATERNION_W

    open fun setQuaternion(quaternion: Quaterniond) {
        quaternionVec = Vector3f(quaternion.x.toFloat(), quaternion.y.toFloat(), quaternion.z.toFloat())
        quaternionW = quaternion.w.toFloat()
    }

    open fun getQuaternion(tickDelta: Float) = Quaternionf(
        Mth.lerp(tickDelta, qxO, quaternionVec.x()),
        Mth.lerp(tickDelta, qyO, quaternionVec.y()),
        Mth.lerp(tickDelta, qzO, quaternionVec.z()),
        Mth.lerp(tickDelta, qwO, quaternionW)
    )

    override fun baseTick() {
        // Track previous quaternion for interpolation
        qxO = quaternionVec.x()
        qyO = quaternionVec.y()
        qzO = quaternionVec.z()
        qwO = quaternionW
        super.baseTick()
    }

    public override fun addAdditionalSaveData(compound: CompoundTag) {
        compound.putString("Target", this.entityData.get(TARGET_UUID))
        compound.putString("LastAttacker", this.entityData.get(LAST_ATTACKER_UUID))
        compound.putBoolean("IsControllable", this.entityData.get(IS_CONTROLLABLE))
        compound.putInt("BombTick", this.entityData.get(BOMB_TICK))

        val localPos = this.entityData.get(STICKY_OBB_LOCAL_POS)
        compound.putFloat("StickyObbLocalX", localPos.x())
        compound.putFloat("StickyObbLocalY", localPos.y())
        compound.putFloat("StickyObbLocalZ", localPos.z())
        compound.putInt("StickyObbFace", this.entityData.get(STICKY_OBB_FACE))
        compound.putInt("StickyObbIndex", this.entityData.get(STICKY_OBB_INDEX))
        compound.putFloat("StickyYOffset", this.entityData.get(STICKY_Y_OFFSET))
        compound.putBoolean("InGround", this.inGround)

        val qVec = this.entityData.get(QUATERNION_VEC)
        compound.putFloat("Qx", qVec.x())
        compound.putFloat("Qy", qVec.y())
        compound.putFloat("Qz", qVec.z())
        compound.putFloat("Qw", this.entityData.get(QUATERNION_W))

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

        if (compound.contains("StickyOnObb")) {
            this.entityData.set(STICKY_ON_OBB, compound.getBoolean("StickyOnObb"))
            this.entityData.set(
                STICKY_OBB_LOCAL_POS,
                Vector3f(
                    compound.getFloat("StickyObbLocalX"),
                    compound.getFloat("StickyObbLocalY"),
                    compound.getFloat("StickyObbLocalZ")
                )
            )
            this.entityData.set(STICKY_OBB_FACE, compound.getInt("StickyObbFace"))
            this.entityData.set(STICKY_OBB_INDEX, compound.getInt("StickyObbIndex"))
        }

        if (compound.contains("StickyYOffset")) {
            this.entityData.set(STICKY_Y_OFFSET, compound.getFloat("StickyYOffset"))
        }

        if (compound.contains("InGround")) {
            this.inGround = compound.getBoolean("InGround")
        }

        if (compound.contains("Qx")) {
            this.entityData.set(
                QUATERNION_VEC,
                Vector3f(compound.getFloat("Qx"), compound.getFloat("Qy"), compound.getFloat("Qz"))
            )
            this.entityData.set(QUATERNION_W, compound.getFloat("Qw"))
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
        if (this.getOwner() === player && player.isShiftKeyDown) {
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
        if (!this.onEntity && !this.inGround && !blockstate.isAir) {
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
                nextPosition = hitResult.location
            }

            while (!this.isRemoved) {
                val entityHitResult = this.findHitEntity(position, nextPosition.add(motion))
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
                if (entityData.get(STICKY_ON_OBB) && target is OBBEntity && !target.enableAABB()) {
                    // OBB mode: reconstruct position from OBB-local coordinates on both sides
                    val obbs = target.getOBBs()
                    val obbIndex = entityData.get(STICKY_OBB_INDEX)
                    if (obbIndex in 0 until obbs.size) {
                        val obb = obbs[obbIndex]
                        val localPosVec = entityData.get(STICKY_OBB_LOCAL_POS)
                        val localX = localPosVec.x().toDouble()
                        val localY = localPosVec.y().toDouble()
                        val localZ = localPosVec.z().toDouble()
                        val faceIndex = entityData.get(STICKY_OBB_FACE)

                        val localPos = Vector3d(localX, localY, localZ)
                        obb.rotation.transform(localPos)
                        val obbCenter = obb.center
                        this.setPos(obbCenter.x + localPos.x, obbCenter.y + localPos.y, obbCenter.z + localPos.z)

                        weldToObb(obb, faceIndex)
                    } else {
                        this.entityData.set(STICKY_ON_OBB, false)
                        val aabb = target.boundingBox
                        this.setPos(target.x, aabb.maxY, target.z)
                        this.setQuaternion(eulerToQuat(this.yRot, -90f))
                    }
                } else {
                    // AABB mode: stick to top of entity's collision box
                    val yOffset = entityData.get(STICKY_Y_OFFSET).toDouble()
                    this.setPos(target.x, target.y + yOffset, target.z)
                }
            } else {
                // Target not loaded yet (e.g. world reload) — keep C4 in place, don't detach
            }
        }

        // Sync quaternion from Euler angles for non-OBB modes
        if (!this.onEntity || !entityData.get(STICKY_ON_OBB)) {
            this.setQuaternion(eulerToQuat(this.yRot, this.xRot))
        }

        this.refreshDimensions()
    }

    /**
     * Builds the render quaternion from Minecraft Euler angles.
     * Matches the old renderer: poseStack applies mulPose(Y(-yaw)) first, then mulPose(X(pitch+90)).
     * In the pose stack this is mat(qY) * mat(qX) = mat(qY * qX), so the quaternion is qY * qX.
     */
    private fun eulerToQuat(yaw: Float, pitch: Float): Quaterniond {
        return Quaterniond()
            .rotateY(Math.toRadians((-yaw).toDouble()))
            .rotateX(Math.toRadians((pitch + 90.0).toDouble()))
    }

    /**
     * Computes the OBB-local quaternion that aligns C4's flat face with the given OBB face.
     * @param faceIndex from OBB.getEmbeddingFace(): ±1=X, ±2=Y, ±3=Z
     */
    private fun faceIndexToLocalQuat(faceIndex: Int): Quaterniond {
        val nx = if (kotlin.math.abs(faceIndex) == 1) Math.signum(faceIndex.toDouble()) else 0.0
        val ny = if (kotlin.math.abs(faceIndex) == 2) Math.signum(faceIndex.toDouble()) else 0.0
        val nz = if (kotlin.math.abs(faceIndex) == 3) Math.signum(faceIndex.toDouble()) else 0.0

        val yRot = Mth.atan2(-nx, nz) * (180.0 / Math.PI)
        val horizontalDist = sqrt(nx * nx + nz * nz)
        val xRot = -Mth.atan2(ny, horizontalDist) * (180.0 / Math.PI)

        return eulerToQuat(yRot.toFloat(), xRot.toFloat())
    }

    /**
     * Determines which OBB face the hit point lies on, using OBB-local coordinates.
     * Returns the same format as OBB.getEmbeddingFace(): ±1=X, ±2=Y, ±3=Z.
     */
    private fun computeFaceIndex(localPos: Vector3d, extents: Vector3d): Int {
        val dx = extents.x - kotlin.math.abs(localPos.x)
        val dy = extents.y - kotlin.math.abs(localPos.y)
        val dz = extents.z - kotlin.math.abs(localPos.z)

        // Find the axis with smallest distance to surface
        var index = 1
        var min = dx
        if (dy < min) { min = dy; index = 2 }
        if (dz < min) { index = 3 }

        // Determine which side (positive or negative face)
        val sign = when (index) {
            1 -> if (localPos.x < 0.0) -1 else 1
            2 -> if (localPos.y < 0.0) -1 else 1
            else -> if (localPos.z < 0.0) -1 else 1
        }
        return index * sign
    }

    /**
     * Welds the C4's quaternion to the OBB: worldQuat = obb.rotation * localFaceQuat.
     * This ensures C4 follows ALL OBB rotations, including spin around the face normal.
     */
    private fun weldToObb(obb: OBB, faceIndex: Int) {
        val localFaceQuat = faceIndexToLocalQuat(faceIndex)
        val worldQuat = Quaterniond(obb.rotation).mul(localFaceQuat)
        this.setQuaternion(worldQuat)
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
        setYHeadRot(yRot)
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
            val entity: Entity? = this.getOwner()
            return entity == null
                    || (entity === pTarget && this.tickCount > 2)
                    || !entity.isPassengerOfSameVehicle(pTarget)
        }
    }

    protected fun onHit(pResult: HitResult) {
        when (pResult.type) {
            HitResult.Type.ENTITY -> this.onHitEntity(pResult as EntityHitResult)
            HitResult.Type.BLOCK -> this.onHitBlock(pResult as BlockHitResult)
            else -> {}
        }
    }

    protected fun onHitEntity(pResult: EntityHitResult) {
        val entity = pResult.entity
        if (tickCount < 2 || entity === this.vehicle || entity is C4Entity) return

        // Save ray info before zeroing motion
        val rayStart = this.position().subtract(this.deltaMovement)
        val rayEnd = rayStart.add(this.deltaMovement.scale(3.0))

        // Set basic attachment on both sides so tick enters entity-attach branch
        this.entityData.set(TARGET_UUID, entity.stringUUID)
        this.onEntity = true
        this.deltaMovement = Vec3.ZERO

        // AABB fallback position (used on client, overwritten by server if OBB succeeds)
        val aabb = entity.boundingBox
        this.entityData.set(STICKY_ON_OBB, false)
        this.entityData.set(STICKY_Y_OFFSET, (aabb.maxY - entity.y).toFloat())
        this.setPos(entity.x, aabb.maxY, entity.z)
        this.xRot = -90f
        this.xRotO = this.xRot

        // OBB detection: SERVER ONLY to avoid client/server desync from stale OBB data
        if (this.level() is ServerLevel && entity is OBBEntity && !entity.enableAABB()) {
            val obbs = entity.getOBBs()
            var closestHit: Triple<OBB, Vec3, Double>? = null

            for (obb in obbs) {
                if (obb.part == OBB.Part.COLLISION) continue
                val hitPos = OBB.rayIntersect(obb, rayStart, rayEnd)
                if (hitPos != null) {
                    val distSqr = rayStart.distanceToSqr(hitPos)
                    if (closestHit == null || distSqr < closestHit.third) {
                        closestHit = Triple(obb, hitPos, distSqr)
                    }
                }
            }

            if (closestHit != null) {
                val (obb, hitPos) = closestHit

                // Convert hit point to OBB-local coordinates
                val obbCenter = obb.center
                val inverseRot = Quaterniond(obb.rotation).conjugate()
                val localPos = Vector3d(hitPos.x - obbCenter.x, hitPos.y - obbCenter.y, hitPos.z - obbCenter.z)
                inverseRot.transform(localPos)

                val faceIndex = computeFaceIndex(localPos, obb.extents)
                val obbIndex = obbs.indexOf(obb)

                this.entityData.set(STICKY_ON_OBB, true)
                this.entityData.set(STICKY_OBB_LOCAL_POS, Vector3f(localPos.x.toFloat(), localPos.y.toFloat(), localPos.z.toFloat()))
                this.entityData.set(STICKY_OBB_FACE, faceIndex)
                this.entityData.set(STICKY_OBB_INDEX, obbIndex)

                // Override position with precise OBB hit point
                this.setPos(hitPos.x, hitPos.y, hitPos.z)
                weldToObb(obb, faceIndex)
            }
        }
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
                target.forceHurt(
                    ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), this, this.owner),
                    ExplosionConfig.C4_EXPLOSION_DAMAGE.get().toFloat() * 0.5f
                )
                target.invulnerableTime = 0
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
            .attacker(this.getOwner())
            .damage(ExplosionConfig.C4_EXPLOSION_DAMAGE.get().toFloat())
            .radius(ExplosionConfig.C4_EXPLOSION_RADIUS.get().toFloat())
            .position(pos)
            .withParticleType(ParticleTool.ParticleType.HUGE)
            .explode()

        this.discard()
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
                stack.getOrCreateTag().putBoolean("Control", true)
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

        @JvmField
        val STICKY_ON_OBB: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(C4Entity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val STICKY_OBB_LOCAL_POS: EntityDataAccessor<Vector3f> =
            SynchedEntityData.defineId(C4Entity::class.java, EntityDataSerializers.VECTOR3)

        @JvmField
        val STICKY_OBB_FACE: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(C4Entity::class.java, EntityDataSerializers.INT)

        @JvmField
        val STICKY_OBB_INDEX: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(C4Entity::class.java, EntityDataSerializers.INT)

        @JvmField
        val STICKY_Y_OFFSET: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(C4Entity::class.java, EntityDataSerializers.FLOAT)

        @JvmField
        val ON_ENTITY: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(C4Entity::class.java, EntityDataSerializers.BOOLEAN)

        @JvmField
        val QUATERNION_VEC: EntityDataAccessor<Vector3f> =
            SynchedEntityData.defineId(C4Entity::class.java, EntityDataSerializers.VECTOR3)

        @JvmField
        val QUATERNION_W: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(C4Entity::class.java, EntityDataSerializers.FLOAT)

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
