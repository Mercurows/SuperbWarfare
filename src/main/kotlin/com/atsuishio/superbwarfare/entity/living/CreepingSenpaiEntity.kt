package com.atsuishio.superbwarfare.entity.living

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.animation.entity.CreepingSenpaiAnimationInstance
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.resource.model.EntityModelReloadListener
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.Mth
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.MoveControl
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.RandomStrollGoal
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.ai.navigation.PathNavigation
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.pathfinder.Path
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.network.NetworkHooks
import kotlin.math.abs

open class CreepingSenpaiEntity(type: EntityType<CreepingSenpaiEntity>, level: Level) : Monster(type, level) {
    open val animationInstance: CreepingSenpaiAnimationInstance? =
        if (this.level().isClientSide) CreepingSenpaiAnimationInstance(this) else null
    open val modelInstance = EntityModelReloadListener.getModel(MODEL)?.createInstance()

    open var attachedFace by ATTACHED_FACE
    var renderStartFace: Direction = Direction.UP
        private set
    var renderTargetFace: Direction = Direction.UP
        private set
    var renderFaceChangeTick: Long = 0L
        private set
    private var navigationTargetSince = 0L
    private var lastNavigationTarget: BlockPos? = null
    private var navigationRetargetCooldown = 0

    init {
        xpReward = 40
        setNoAi(false)
        this.moveControl = ClimbingMoveControl()
    }

    override fun defineSynchedData() {
        super.defineSynchedData()
        this.entityData.define(ATTACHED_FACE, Direction.UP)
    }

    override fun createNavigation(level: Level): PathNavigation {
        return CreepingSenpaiNavigation(this, level)
    }

    override fun getStandingEyeHeight(poseIn: Pose, sizeIn: EntityDimensions): Float =
        if (isDeadOrDying) DEAD_DIMENSIONS.height * 0.85f else 0.65f

    /**
     * 死亡后把碰撞箱压到贴地。
     *
     * 爬行先辈死后尸体同样要在地上躺满 27 秒（`tickDeath()` 到 `deathTime == 540` 才 `remove()`），
     * 存活时 1.0×0.9 的箱子（注册值见 `ModEntities`）立在那儿整段时间都在挡路 ——
     * 死亡动画里它早就翻倒躺平了（`upper` 绕 Z 转到 183°）。
     *
     * 箱子只变矮不变宽，且实体的 Y 是箱子底面（脚底），所以新箱子恒为旧箱子的子集，
     * 不会因为变矮卡进上方方块里。`baseTick()` 本来就每 tick 调 `refreshDimensions()`，
     * 尺寸变化会立刻生效；双端都算得出来（`isDeadOrDying()` 读的是同步过的血量）。
     *
     * ⚠ 贴天花板（`attachedFace == DOWN`）时 `aiStep()` 里的 `maintainSurfaceAttachment()`
     * 会用 `bbHeight` 反推贴地高度，但那个分支第一行就判断了 `attachedFace != Direction.DOWN` 直接返回，
     * 而 `updateSurfaceAttachment()` 在 `isDeadOrDying` 时会把 `attachedFace` 归回 `UP`，
     * 所以死亡后不会出现"箱子变矮 → 尸体自己往上飘"。
     */
    override fun getDimensions(pose: Pose): EntityDimensions =
        if (isDeadOrDying) DEAD_DIMENSIONS.scale(scale) else super.getDimensions(pose)

    /**
     * 死亡后不再推开其他实体。
     *
     * `LivingEntity.aiStep()` → `pushEntities()` 每 tick 把周围 `isPushable()` 的实体挑出来交给
     * `doPush()`，默认实现是 `entityIn.push(this)` —— 也就是**尸体把玩家/生物推走**。
     * 反方向（玩家推尸体）本来就不会发生：尸体 `isAlive()` 为 false，`isPushable()` 跟着为 false，
     * `EntitySelector.pushableBy()` 压根不会把尸体选进来。所以只要堵住这一边。
     *
     * 存活时保持原版行为（会推挤别人），只在死亡后才吞掉。
     */
    override fun doPush(entityIn: Entity) {
        if (!isDeadOrDying) super.doPush(entityIn)
    }

    override fun getAddEntityPacket(): Packet<ClientGamePacketListener?> {
        return NetworkHooks.getEntitySpawningPacket(this)
    }

    override fun registerGoals() {
        super.registerGoals()
        this.goalSelector.addGoal(1, object : MeleeAttackGoal(this, 1.4, false) {
            override fun getAttackReachSqr(entity: LivingEntity): Double {
                return (this.mob.bbWidth * this.mob.bbWidth + entity.bbWidth).toDouble()
            }
        })
        this.targetSelector.addGoal(2, HurtByTargetGoal(this).setAlertOthers())
        this.goalSelector.addGoal(3, RandomLookAroundGoal(this))
        this.goalSelector.addGoal(4, FloatGoal(this))
        this.goalSelector.addGoal(5, RandomStrollGoal(this, 0.8))
        this.targetSelector.addGoal(6, CreepingSenpaiTargetGoal())
    }

    override fun getMobType(): MobType = MobType.ILLAGER

    override fun onClimbable(): Boolean {
        return this.attachedFace != Direction.UP
    }

    public override fun getAmbientSound(): SoundEvent? {
        return ModSounds.IDLE.get()
    }

    public override fun playStepSound(pos: BlockPos, blockIn: BlockState) {
        this.playSound(ModSounds.STEP.get(), 0.25f, 1f)
    }

    public override fun getHurtSound(ds: DamageSource): SoundEvent {
        return ModSounds.OUCH.get()
    }

    public override fun getDeathSound(): SoundEvent {
        return ModSounds.GROWL.get()
    }

    override fun baseTick() {
        super.baseTick()
        if (this.level().isClientSide) {
            val current = this.attachedFace
            if (current != this.renderTargetFace) {
                this.renderStartFace = this.renderTargetFace
                this.renderTargetFace = current
                this.renderFaceChangeTick = this.tickCount.toLong()
            }
        }
        this.refreshDimensions()
    }

    override fun aiStep() {
        if (!this.level().isClientSide) {
            this.updateSurfaceAttachment()
            if (this.navigationRetargetCooldown > 0) {
                this.navigationRetargetCooldown--
            }
        }
        super.aiStep()
        if (!this.level().isClientSide) {
            this.updateNavigationTimeout()
            this.maintainSurfaceAttachment()
        }
        this.updateSwingTime()
    }

    override fun tickDeath() {
        ++this.deathTime
        if (this.deathTime == 540) {
            this.remove(RemovalReason.KILLED)
            this.dropExperience()
        }
    }

    fun renderFaceProgress(partialTick: Float): Float {
        val elapsed = this.tickCount.toFloat() + partialTick - this.renderFaceChangeTick.toFloat()
        return Mth.clamp(elapsed / RENDER_FACE_TRANSITION_TICKS, 0f, 1f)
    }

    private fun updateNavigationTimeout() {
        val navigation = this.navigation
        val target = navigation.targetPos

        if (!navigation.isInProgress || target == null) {
            this.lastNavigationTarget = null
            return
        }

        if (target != this.lastNavigationTarget) {
            this.lastNavigationTarget = target
            this.navigationTargetSince = this.tickCount.toLong()
            return
        }

        if (this.tickCount.toLong() - this.navigationTargetSince < NAVIGATION_TIMEOUT_TICKS) {
            return
        }

        if (this.position().distanceToSqr(Vec3.atBottomCenterOf(target)) < 2.25) {
            return
        }

        navigation.stop()
        this.setTarget(null)
        this.navigationRetargetCooldown = NAVIGATION_RETARGET_COOLDOWN
        this.lastNavigationTarget = null
    }

    inner class CreepingSenpaiTargetGoal :
        NearestAttackableTargetGoal<Player>(this@CreepingSenpaiEntity, Player::class.java, false, false) {
        override fun canUse(): Boolean {
            return if (this@CreepingSenpaiEntity.navigationRetargetCooldown > 0) {
                false
            } else {
                super.canUse()
            }
        }

        override fun getTargetSearchArea(targetDistance: Double): AABB {
            return this.mob.boundingBox.inflate(targetDistance, targetDistance, targetDistance)
        }
    }

    private fun updateSurfaceAttachment() {
        if (this.onGround() || this.isDeadOrDying) {
            this.attachedFace = Direction.UP
            this.isNoGravity = false
            return
        }

        val face = this.findAttachedFace()
        this.attachedFace = face
        this.isNoGravity = face != Direction.UP

        if (face.axis.isHorizontal) {
            val yaw = faceWallYaw(face)
            this.yRot = yaw
            this.setYHeadRot(yaw)
            this.setYBodyRot(yaw)
        }
    }

    private fun findAttachedFace(): Direction {
        val level = this.level()
        val box = this.boundingBox
        val midY = (box.minY + box.maxY) / 2.0

        val ceilingPos = BlockPos.containing(this.x, box.maxY + 0.1, this.z)
        if (level.getBlockState(ceilingPos).isCollisionShapeFullBlock(level, ceilingPos)) {
            return Direction.DOWN
        }

        for (direction in Direction.entries) {
            if (!direction.axis.isHorizontal) continue
            val sampleX = this.x + direction.stepX * (box.xsize / 2.0 + 0.1)
            val sampleZ = this.z + direction.stepZ * (box.zsize / 2.0 + 0.1)
            val samplePos = BlockPos.containing(sampleX, midY, sampleZ)
            if (level.getBlockState(samplePos).isCollisionShapeFullBlock(level, samplePos)) {
                return direction.opposite
            }
        }

        val floorPos = BlockPos.containing(this.x, box.minY - 0.1, this.z)
        if (this.onGround() || level.getBlockState(floorPos).isCollisionShapeFullBlock(level, floorPos)) {
            return Direction.UP
        }

        return Direction.UP
    }

    private fun maintainSurfaceAttachment() {
        if (this.attachedFace != Direction.DOWN) return

        val ceilingPos = BlockPos.containing(this.x, this.boundingBox.maxY + 0.05, this.z)
        if (!this.level().getBlockState(ceilingPos).isCollisionShapeFullBlock(this.level(), ceilingPos)) return

        val desiredY = ceilingPos.y.toDouble() - this.bbHeight - 0.01
        val diff = desiredY - this.y
        if (abs(diff) > 0.02) {
            this.move(MoverType.SELF, Vec3(0.0, diff.coerceIn(-0.12, 0.12), 0.0))
        }
        this.setDeltaMovement(this.deltaMovement.x, 0.0, this.deltaMovement.z)
    }

    private fun faceWallYaw(face: Direction): Float {
        val radians = Mth.atan2(face.stepX.toDouble(), (-face.stepZ).toDouble())
        return (radians * (180.0 / Math.PI)).toFloat()
    }

    inner class ClimbingMoveControl : MoveControl(this@CreepingSenpaiEntity) {
        override fun tick() {
            val entity = this@CreepingSenpaiEntity
            val face = entity.attachedFace

            if (face == Direction.UP) {
                entity.isNoGravity = false
                super.tick()
                return
            }

            if (!this.hasWanted() || entity.isDeadOrDying) {
                entity.setSpeed(0f)
                entity.setYya(0f)
                return
            }

            val toTarget = Vec3(this.wantedX, this.wantedY, this.wantedZ).subtract(entity.position())
            val normal = Vec3.atLowerCornerOf(face.normal)
            val surfaceMove = if (face == Direction.DOWN) {
                Vec3(toTarget.x, 0.0, toTarget.z)
            } else {
                toTarget.subtract(normal.scale(toTarget.dot(normal)))
            }

            if (surfaceMove.lengthSqr() < 1.0E-6) {
                entity.setSpeed(0f)
                entity.setYya(0f)
                return
            }

            val speed = (this.speedModifier * entity.getAttributeValue(Attributes.MOVEMENT_SPEED)).toFloat()
            entity.deltaMovement = surfaceMove.normalize().scale(speed.toDouble())
            entity.setSpeed(0f)
            entity.setYya(0f)

            if (face == Direction.DOWN) {
                val targetYaw =
                    (Mth.atan2(toTarget.z, toTarget.x) * (180.0 / Math.PI) - 90.0).toFloat()
                val yaw = this.rotlerp(entity.yRot, targetYaw, 90f)
                entity.yRot = yaw
                entity.setYHeadRot(yaw)
                entity.setYBodyRot(yaw)
            }
        }
    }

    companion object {
        @JvmField
        val ATTACHED_FACE: EntityDataAccessor<Direction> =
            SynchedEntityData.defineId(CreepingSenpaiEntity::class.java, EntityDataSerializers.DIRECTION)

        const val RENDER_FACE_TRANSITION_TICKS: Float = 8f
        const val NAVIGATION_TIMEOUT_TICKS: Long = 300L
        const val NAVIGATION_RETARGET_COOLDOWN: Int = 40

        fun createAttributes(): AttributeSupplier.Builder {
            return createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
        }

        /** 死亡后贴地的碰撞箱（注册时的存活箱是 1.0×0.9）。 */
        val DEAD_DIMENSIONS: EntityDimensions = EntityDimensions.scalable(1.0f, 0.4f)

        val MODEL = loc("models/bedrock/entity/creeping_senpai.geo.json")
    }

}

private class CreepingSenpaiNavigation(mob: Mob, level: Level) : WallClimberNavigation(mob, level) {
    private var stopped = false

    override fun createPath(pPos: BlockPos, pAccuracy: Int): Path? {
        this.stopped = false
        return super.createPath(pPos, pAccuracy)
    }

    override fun createPath(pEntity: Entity, pAccuracy: Int): Path? {
        this.stopped = false
        return super.createPath(pEntity, pAccuracy)
    }

    override fun moveTo(pEntity: Entity, pSpeed: Double): Boolean {
        this.stopped = false
        return super.moveTo(pEntity, pSpeed)
    }

    override fun stop() {
        this.stopped = true
        super.stop()
    }

    override fun tick() {
        if (this.stopped) return
        super.tick()
    }
}
