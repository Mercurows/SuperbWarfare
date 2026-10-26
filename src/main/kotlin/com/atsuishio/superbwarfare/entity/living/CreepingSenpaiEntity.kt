package com.atsuishio.superbwarfare.entity.living

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.animation.entity.CreepingSenpaiAnimationInstance
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.resource.model.EntityModelReloadListener
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.control.MoveControl
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.RandomStrollGoal
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation
import net.minecraft.world.entity.ai.navigation.PathNavigation
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import java.util.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

open class CreepingSenpaiEntity(type: EntityType<CreepingSenpaiEntity>, level: Level) : Monster(type, level) {
    open val animationInstance = if (this.level().isClientSide) CreepingSenpaiAnimationInstance(this) else null
    open val modelInstance = EntityModelReloadListener.getModel(MODEL)?.createInstance()

    open var attachedFace by ATTACHED_FACE
    var surfaceHeading by SURFACE_HEADING
    private val surfaceProbe = SurfaceProbe()
    private var surfaceAttached = false
    private var surfaceVelocity = Vec3.ZERO
    private var detachUntilTick = 0

    init {
        xpReward = 40
        isNoAi = false
        this.moveControl = SurfaceMoveControl()
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)

        with(builder) {
            define(ATTACHED_FACE, Direction.UP)
            define(SURFACE_HEADING, 0F)
        }
    }

    override fun createNavigation(level: Level): PathNavigation {
        return SurfaceNavigation(level)
    }

    override fun getEyeY(): Double =
        if (isDeadOrDying) DEAD_DIMENSIONS.height * 0.85 else 0.65

    override fun getDefaultDimensions(pose: Pose): EntityDimensions =
        if (isDeadOrDying) DEAD_DIMENSIONS.scale(scale) else super.getDimensions(pose)

    override fun doPush(entityIn: Entity) {
        if (!isDeadOrDying) super.doPush(entityIn)
    }

    override fun registerGoals() {
        super.registerGoals()
        this.goalSelector.addGoal(0, FloatGoal(this))
        this.goalSelector.addGoal(1, SurfaceAttackGoal())
        this.targetSelector.addGoal(2, SurfaceRetaliationGoal().setAlertOthers())
        this.goalSelector.addGoal(3, RandomLookAroundGoal(this))
        this.goalSelector.addGoal(5, RandomStrollGoal(this, 0.8))
        this.targetSelector.addGoal(6, CreepingSenpaiTargetGoal())
    }

    override fun onClimbable(): Boolean {
        return surfaceAttached && this.attachedFace != Direction.UP
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
        this.refreshDimensions()
    }

    override fun aiStep() {
        if (!this.level().isClientSide) {
            surfaceVelocity = Vec3.ZERO
            updateSurfaceAttachment()
        }
        super.aiStep()
        this.updateSwingTime()
    }

    override fun travel(travelVector: Vec3) {
        if (!level().isClientSide && surfaceAttached && isAlive && !isInWaterOrBubble && !isInLava && !isNoAi) {
            // 原版 travel 会钳制攀爬速度并在撞墙时向上推，六面移动由此处统一执行。
            // 跟随路点时准确停在转角，避免惯性把身体推离墙面或切进外角。
            val residual = if (surfaceVelocity.lengthSqr() > 1.0E-8) Vec3.ZERO else deltaMovement.scale(0.35)
            // 地面保留轻微向下的接触力，让原版 onGround 和脚步声判定正常工作。
            val groundPull = if (attachedFace == Direction.UP && !isNoGravity) Vec3(0.0, -0.02, 0.0) else Vec3.ZERO
            deltaMovement = surfaceVelocity.add(residual).add(groundPull)
            fallDistance = 0f
            move(MoverType.SELF, deltaMovement)
            deltaMovement = deltaMovement.scale(0.4)
            calculateEntityAnimation(true)
        } else {
            super.travel(travelVector)
        }
    }

    override fun knockback(strength: Double, x: Double, z: Double) {
        super.knockback(strength, x, z)
        if (surfaceAttached) {
            detachUntilTick = tickCount + 6
            surfaceAttached = false
            attachedFace = Direction.UP
            isNoGravity = false
            navigation.stop()
        }
    }

    override fun tickDeath() {
        ++this.deathTime
        if (this.deathTime == 540) {
            this.remove(RemovalReason.KILLED)
            this.dropExperience(this)
        }
    }

    private inner class CreepingSenpaiTargetGoal : NearestAttackableTargetGoal<Player>(
        this@CreepingSenpaiEntity, Player::class.java, false, false
    ) {
        init {
            // 发现目标仍需可见；保留目标由追击 Goal 统一管理，避免原版的第二套视线超时。
            targetConditions.ignoreLineOfSight().selector {
                distanceToSqr(it) <= getAttributeValue(Attributes.FOLLOW_RANGE).let { range -> range * range } &&
                        hasTargetSight(it)
            }
        }

        override fun getTargetSearchArea(targetDistance: Double): AABB =
            mob.boundingBox.inflate(targetDistance, targetDistance, targetDistance)

        override fun canContinueToUse(): Boolean =
            this@CreepingSenpaiEntity.target != null && super.canContinueToUse()
    }

    private inner class SurfaceRetaliationGoal : HurtByTargetGoal(this@CreepingSenpaiEntity) {
        override fun canContinueToUse(): Boolean = this@CreepingSenpaiEntity.target?.let {
            canAttack(it) && !isAlliedTo(it) &&
                    distanceToSqr(it) <= getAttributeValue(Attributes.FOLLOW_RANGE).let { range -> range * range }
        } == true
    }

    private class TargetMemory(initialBox: AABB, tick: Int) {
        var rememberedBox = initialBox
            private set
        private var lastSeen = tick
        private var lastProgress = tick

        fun observe(box: AABB, tick: Int) {
            rememberedBox = box
            lastSeen = tick
        }

        fun progress(tick: Int) {
            lastProgress = tick
        }

        fun expired(tick: Int): Boolean = tick - max(lastSeen, lastProgress) > TARGET_MEMORY_TICKS
    }

    private object SurfaceSight {
        fun canSee(
            body: AABB, eye: Vec3, face: Direction, targetBox: AABB, targetEye: Vec3,
            clear: (Vec3, Vec3) -> Boolean
        ): Boolean {
            // 贴墙时眼睛可能被屋檐遮住；采样点始终留在真实碰撞箱内，不能从墙的另一侧透视。
            val normal = Vec3.atLowerCornerOf(face.normal)
            val extent = if (face.axis.isHorizontal) min(body.xsize, body.zsize) / 2.0 else body.ysize / 2.0
            val sources = listOf(
                eye, Vec3(body.center.x, body.maxY - 0.05, body.center.z),
                body.center.add(normal.scale(max(0.0, extent - 0.05)))
            ).distinct()
            val targets = listOf(
                targetEye, targetBox.center,
                Vec3(targetBox.center.x, targetBox.minY + 0.1, targetBox.center.z)
            )
            return sources.any { from -> targets.any { to -> clear(from, to) } }
        }
    }

    private fun clearSight(from: Vec3, to: Vec3): Boolean =
        level().clip(
            ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
            )
        ).type == HitResult.Type.MISS

    private fun hasTargetSight(enemy: LivingEntity): Boolean = sensing.hasLineOfSight(enemy) ||
            SurfaceSight.canSee(
                boundingBox,
                eyePosition,
                attachedFace,
                enemy.boundingBox,
                enemy.eyePosition,
                ::clearSight
            )

    private inner class SurfaceAttackGoal : Goal() {
        private var victim: LivingEntity? = null
        private var memory: TargetMemory? = null
        private var attackCooldown = 0
        private var failures = 0

        init {
            flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
        }

        override fun canUse(): Boolean = target?.let {
            it.isAlive && canAttack(it)
        } == true

        override fun canContinueToUse(): Boolean = canUse()

        override fun requiresUpdateEveryTick(): Boolean = true

        override fun start() {
            victim = target
            memory = victim?.let { TargetMemory(it.boundingBox, tickCount) }
            attackCooldown = 0
            failures = 0
            navigation.stop()
            isAggressive = true
        }

        override fun stop() {
            navigation.stop()
            isAggressive = false
            victim = null
            memory = null
        }

        override fun tick() {
            val enemy = target ?: return
            if (victim !== enemy) {
                start()
            }
            val tracking = memory ?: return
            val nav = navigation as SurfaceNavigation
            val visible = hasTargetSight(enemy)
            if (visible) tracking.observe(enemy.boundingBox, tickCount)
            if (nav.madeProgress) {
                tracking.progress(tickCount)
                failures = 0
                nav.madeProgress = false
            }
            if (tracking.expired(tickCount) ||
                distanceToSqr(enemy) > getAttributeValue(Attributes.FOLLOW_RANGE).let { it * it }
            ) {
                target = null
                navigation.stop()
                return
            }

            val box = tracking.rememberedBox
            lookControl.setLookAt(box.center.x, box.center.y, box.center.z)
            if (attackCooldown > 0) attackCooldown--
            if (visible && canAttackFrom(position(), enemy.boundingBox)) {
                nav.stop()
                faceSurfaceDirection(enemy.boundingBox.center.subtract(boundingBox.center))
                failures = 0
                if (attackCooldown == 0) {
                    swing(InteractionHand.MAIN_HAND)
                    doHurtTarget(enemy)
                    attackCooldown = 20
                }
                return
            }

            if (nav.failed) {
                failures++
                nav.failed = false
                if (failures >= 3) {
                    // 暂时无路可达只降低重试频率；可见目标仍然保持锁定。
                    nav.delayRetries(60)
                    failures = 0
                }
            }
            nav.pursue(box, 1.4)
        }
    }

    private fun bodyAt(feet: Vec3): AABB {
        val halfWidth = bbWidth / 2.0
        return AABB(
            feet.x - halfWidth, feet.y, feet.z - halfWidth,
            feet.x + halfWidth, feet.y + bbHeight, feet.z + halfWidth
        )
    }

    private fun faceSurfaceDirection(delta: Vec3) {
        val outward = Vec3.atLowerCornerOf(attachedFace.normal)
        val forward = if (attachedFace.axis.isHorizontal) Vec3(0.0, 1.0, 0.0) else Vec3(0.0, 0.0, 1.0)
        val right = forward.cross(outward)
        if (delta.subtract(outward.scale(delta.dot(outward))).lengthSqr() > 1.0E-6) {
            surfaceHeading = (Mth.atan2(delta.dot(right), delta.dot(forward)) * 180.0 / Math.PI).toFloat()
        }
    }

    private fun canAttackFrom(feet: Vec3, targetBox: AABB, reach: Double = 1.0): Boolean {
        val own = bodyAt(feet)
        val dx = max(0.0, max(own.minX - targetBox.maxX, targetBox.minX - own.maxX))
        val dy = max(0.0, max(own.minY - targetBox.maxY, targetBox.minY - own.maxY))
        val dz = max(0.0, max(own.minZ - targetBox.maxZ, targetBox.minZ - own.maxZ))
        if (dx * dx + dy * dy + dz * dz > reach * reach) return false
        return level().clip(
            ClipContext(
                feet.add(0.0, eyeHeight.toDouble(), 0.0), targetBox.center,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this
            )
        ).type == HitResult.Type.MISS
    }

    private fun updateSurfaceAttachment() {
        if (!isAlive || isNoAi || isPassenger || isInWaterOrBubble || isInLava || tickCount < detachUntilTick) {
            surfaceAttached = false
            attachedFace = Direction.UP
            isNoGravity = false
            return
        }
        val waypoint = (navigation as SurfaceNavigation).currentWaypoint()
        // 外角中间路点可能只接触方块棱边；仅在该过渡仍有效且实体没有偏离路线时维持附着。
        val transitioning = waypoint != null && waypoint.corner &&
                waypoint.position.distanceToSqr(position()) < 4.0 && surfaceProbe.valid(waypoint.node) &&
                surfaceProbe.valid(waypoint.from) && surfaceProbe.clearSegment(position(), waypoint.position)
        val preferred = waypoint?.node?.face
        val face = when {
            preferred != null && surfaceProbe.touches(boundingBox, preferred) -> preferred
            surfaceProbe.touches(boundingBox, attachedFace) -> attachedFace
            transitioning -> attachedFace
            else -> Direction.entries.firstOrNull { surfaceProbe.touches(boundingBox, it) }
        }
        surfaceAttached = face != null
        attachedFace = face ?: Direction.UP
        isNoGravity = surfaceAttached && (attachedFace != Direction.UP || transitioning)
    }

    private inner class SurfaceProbe {
        private fun loaded(pos: BlockPos): Boolean = level().hasChunk(pos.x shr 4, pos.z shr 4)

        private fun collisionBoxes(pos: BlockPos): List<AABB> {
            if (!loaded(pos) || level().isOutsideBuildHeight(pos)) return emptyList()
            return level().getBlockState(pos).getCollisionShape(
                level(), pos,
                CollisionContext.of(this@CreepingSenpaiEntity)
            ).toAabbs().map { it.move(pos) }
        }

        fun nodes(pos: BlockPos, face: Direction): List<SurfaceNode> =
            SurfaceGeometry(bbWidth.toDouble(), bbHeight.toDouble(), ::collisionBoxes).nodes(pos, face)

        fun clear(feet: Vec3): Boolean {
            val box = bodyAt(feet).deflate(1.0E-5)
            val min = BlockPos.containing(box.minX, box.minY, box.minZ)
            val max = BlockPos.containing(box.maxX, box.maxY, box.maxZ)
            return loaded(min) && loaded(max) &&
                    box.minY >= level().minBuildHeight && box.maxY <= level().maxBuildHeight &&
                    level().worldBorder.isWithinBounds(box) &&
                    BlockPos.betweenClosed(min, max).all { level().getFluidState(it).isEmpty } &&
                    level().getBlockCollisions(this@CreepingSenpaiEntity, box).none()
        }

        fun valid(node: SurfaceNode): Boolean = nodes(node.pos, node.face).contains(node) &&
                clear(node.feet())

        fun clearSegment(from: Vec3, to: Vec3): Boolean {
            val steps = max(1, ceil(from.distanceTo(to) / 0.2).toInt())
            return (0..steps).all { clear(from.lerp(to, it.toDouble() / steps)) }
        }

        fun touches(box: AABB, normal: Direction): Boolean {
            val probe = when (normal) {
                Direction.UP -> AABB(
                    box.minX + 0.05, box.minY - 0.12, box.minZ + 0.05,
                    box.maxX - 0.05, box.minY + 0.01, box.maxZ - 0.05
                )

                Direction.DOWN -> AABB(
                    box.minX + 0.05, box.maxY - 0.01, box.minZ + 0.05,
                    box.maxX - 0.05, box.maxY + 0.12, box.maxZ - 0.05
                )

                Direction.EAST -> AABB(
                    box.minX - 0.12, box.minY + 0.05, box.minZ + 0.05,
                    box.minX + 0.01, box.maxY - 0.05, box.maxZ - 0.05
                )

                Direction.WEST -> AABB(
                    box.maxX - 0.01, box.minY + 0.05, box.minZ + 0.05,
                    box.maxX + 0.12, box.maxY - 0.05, box.maxZ - 0.05
                )

                Direction.SOUTH -> AABB(
                    box.minX + 0.05, box.minY + 0.05, box.minZ - 0.12,
                    box.maxX - 0.05, box.maxY - 0.05, box.minZ + 0.01
                )

                Direction.NORTH -> AABB(
                    box.minX + 0.05, box.minY + 0.05, box.maxZ - 0.01,
                    box.maxX - 0.05, box.maxY - 0.05, box.maxZ + 0.12
                )
            }
            return level().getBlockCollisions(this@CreepingSenpaiEntity, probe).any()
        }

        fun start(): SurfaceNode? {
            val origin = BlockPos.containing(boundingBox.center)
            return BlockPos.betweenClosed(origin.offset(-1, -1, -1), origin.offset(1, 1, 1))
                .flatMap { pos -> Direction.entries.flatMap { nodes(pos.immutable(), it) } }
                .filter {
                    clear(it.feet()) && touches(boundingBox, it.face) &&
                            it.feet().distanceToSqr(position()) < 2.25
                }
                .sortedBy { it.feet().distanceToSqr(position()) + if (it.face == attachedFace) 0.0 else 0.1 }
                .firstOrNull { clearSegment(position(), it.feet()) }
        }
    }

    /** 格子、附着面和实际落脚点共同标识节点，同一格内可同时存在不同高度的表面。 */
    private data class SurfaceNode(
        val pos: BlockPos,
        val face: Direction,
        val anchor: Vec3,
        val supportPos: BlockPos
    ) {
        fun feet(): Vec3 = anchor
    }

    /** 按碰撞形状的实际表面生成节点，不要求支撑方块填满整个格子。 */
    private class SurfaceGeometry(
        private val width: Double,
        private val height: Double,
        private val collisionBoxes: (BlockPos) -> List<AABB>
    ) {
        fun nodes(pos: BlockPos, face: Direction): List<SurfaceNode> = buildList {
            val supports = listOf(pos.relative(face.opposite), pos).flatMap {
                if (face.axis.isHorizontal) listOf(it, it.below()) else listOf(it)
            }
            for (support in supports) {
                for (box in collisionBoxes(support)) {
                    val feet = when (face) {
                        Direction.UP -> Vec3(pos.x + 0.5, box.maxY, pos.z + 0.5)
                        Direction.DOWN -> Vec3(pos.x + 0.5, box.minY - height, pos.z + 0.5)
                        Direction.EAST -> Vec3(box.maxX + width / 2.0, pos.y + (1.0 - height) / 2.0, pos.z + 0.5)
                        Direction.WEST -> Vec3(box.minX - width / 2.0, pos.y + (1.0 - height) / 2.0, pos.z + 0.5)
                        Direction.SOUTH -> Vec3(pos.x + 0.5, pos.y + (1.0 - height) / 2.0, box.maxZ + width / 2.0)
                        Direction.NORTH -> Vec3(pos.x + 0.5, pos.y + (1.0 - height) / 2.0, box.minZ - width / 2.0)
                    }
                    val cellY = if (face == Direction.UP) feet.y else feet.y + height / 2.0
                    if (BlockPos.containing(feet.x + 1.0E-6, cellY + 1.0E-6, feet.z + 1.0E-6) != pos) continue
                    val body = AABB(
                        feet.x - width / 2.0, feet.y, feet.z - width / 2.0,
                        feet.x + width / 2.0, feet.y + height, feet.z + width / 2.0
                    )
                    val overlapX = min(body.maxX, box.maxX) - max(body.minX, box.minX)
                    val overlapY = min(body.maxY, box.maxY) - max(body.minY, box.minY)
                    val overlapZ = min(body.maxZ, box.maxZ) - max(body.minZ, box.minZ)
                    val supported = when (face.axis) {
                        Direction.Axis.X -> overlapY > 0.05 && overlapZ > 0.05
                        Direction.Axis.Y -> overlapX > 0.05 && overlapZ > 0.05
                        Direction.Axis.Z -> overlapX > 0.05 && overlapY > 0.05
                    }
                    if (supported) add(SurfaceNode(pos.immutable(), face, feet, support.immutable()))
                }
            }
        }.distinct()
    }

    private data class SurfaceWaypoint(
        val position: Vec3,
        val node: SurfaceNode,
        val from: SurfaceNode,
        val corner: Boolean = false
    )

    private data class SurfaceEdge(val to: SurfaceNode, val points: List<SurfaceWaypoint>)
    private data class SearchEntry(val node: SurfaceNode, val cost: Double, val score: Double)

    /** 只依赖几何回调，所有 Minecraft 世界读取均在服务端 tick 内完成。 */
    private class SurfaceSearch(
        private val start: SurfaceNode,
        private val destination: Vec3,
        private val range: Double,
        private val resolve: (BlockPos, Direction) -> List<SurfaceNode>,
        private val canOccupy: (SurfaceNode) -> Boolean,
        private val canTraverse: (Vec3, Vec3) -> Boolean,
        private val isGoal: (SurfaceNode) -> Boolean
    ) {
        private val open = PriorityQueue<SearchEntry>(compareBy { it.score })
        private val costs = mutableMapOf(start to 0.0)
        private val parents = mutableMapOf<SurfaceNode, Pair<SurfaceNode, SurfaceEdge>>()
        private val validity = mutableMapOf<SurfaceNode, Boolean>()
        private val surfaces = mutableMapOf<Pair<BlockPos, Direction>, List<SurfaceNode>>()
        private var visited = 0
        var done = false
            private set
        var result: List<SurfaceWaypoint>? = null
            private set

        init {
            open.add(SearchEntry(start, 0.0, heuristic(start)))
        }

        private fun heuristic(node: SurfaceNode): Double =
            max(0.0, node.feet().distanceTo(destination) - 3.0)

        private fun valid(node: SurfaceNode): Boolean = validity.getOrPut(node) {
            node.pos.distSqr(start.pos) <= range * range && canOccupy(node)
        }

        private fun nodes(pos: BlockPos, face: Direction): List<SurfaceNode> =
            surfaces.getOrPut(pos to face) { resolve(pos, face) }

        private fun edge(from: SurfaceNode, to: SurfaceNode, corner: Vec3? = null): SurfaceEdge? {
            if (!valid(to)) return null
            val end = to.feet()
            val points = if (corner == null) listOf(SurfaceWaypoint(end, to, from, from.face != to.face)) else
                listOf(SurfaceWaypoint(corner, to, from, true), SurfaceWaypoint(end, to, from, true))
            var previous = from.feet()
            for (point in points) {
                if (!canTraverse(previous, point.position)) return null
                previous = point.position
            }
            return SurfaceEdge(to, points)
        }

        private fun neighbors(node: SurfaceNode): List<SurfaceEdge> = buildList {
            val from = node.feet()
            val normal = Vec3.atLowerCornerOf(node.face.normal)
            for (tangent in Direction.entries) {
                if (tangent.axis == node.face.axis) continue
                // 相邻格可有半砖、台阶等高度差；先离开旧表面，再沿新表面移动。
                for (offset in -1..1) {
                    for (next in nodes(node.pos.relative(tangent).relative(node.face, offset), node.face)) {
                        val to = next.feet()
                        val shift = to.subtract(from).dot(normal)
                        if (kotlin.math.abs(shift) > 1.0 + 1.0E-6) continue
                        val corner = when {
                            shift > 1.0E-6 -> from.add(normal.scale(shift))
                            shift < -1.0E-6 -> to.subtract(normal.scale(shift))
                            else -> null
                        }
                        edge(node, next, corner)?.let { add(it) }
                    }
                }
                // 同格内角，以及绕同一碰撞方块棱边的外角。
                for (next in nodes(node.pos, tangent)) {
                    edge(node, next)?.let { add(it) }
                }
                for (cell in listOf(node.supportPos, node.supportPos.relative(tangent))) {
                    for (outside in nodes(cell, tangent)) {
                        if (outside.supportPos != node.supportPos) continue
                        val to = outside.feet()
                        val corner = Vec3(
                            if (node.face.axis == Direction.Axis.X) from.x else to.x,
                            if (node.face.axis == Direction.Axis.Y) from.y else if (tangent.axis == Direction.Axis.Y) to.y else from.y,
                            if (node.face.axis == Direction.Axis.Z) from.z else to.z
                        )
                        edge(node, outside, corner)?.let { add(it) }
                    }
                }
            }
        }

        fun step(budget: Int) {
            repeat(budget) {
                if (done) return
                val current = open.poll()
                if (current == null || visited++ >= MAX_SEARCH_NODES) {
                    done = true
                    return
                }
                if (current.cost > costs.getValue(current.node) + 1.0E-6) return@repeat
                if (isGoal(current.node)) {
                    val edges = mutableListOf<SurfaceEdge>()
                    var node = current.node
                    while (node != start) {
                        val (parent, edge) = parents.getValue(node)
                        edges.add(edge)
                        node = parent
                    }
                    result = listOf(SurfaceWaypoint(start.feet(), start, start)) +
                            edges.asReversed().flatMap { it.points }
                    done = true
                    return
                }
                for (edge in neighbors(current.node)) {
                    var previous = current.node.feet()
                    var distance = 0.0
                    for (point in edge.points) {
                        distance += previous.distanceTo(point.position)
                        previous = point.position
                    }
                    val cost = current.cost + distance + if (edge.to.face == current.node.face) 0.0 else 0.35
                    if (cost + 1.0E-6 >= (costs[edge.to] ?: Double.POSITIVE_INFINITY)) continue
                    costs[edge.to] = cost
                    parents[edge.to] = current.node to edge
                    open.add(SearchEntry(edge.to, cost, cost + heuristic(edge.to)))
                }
            }
        }
    }

    private inner class SurfaceNavigation(level: Level) : GroundPathNavigation(this@CreepingSenpaiEntity, level) {
        private var search: SurfaceSearch? = null
        private var route = emptyList<SurfaceWaypoint>()
        private var index = 0
        private var destination: Vec3? = null
        private var attackBox: AABB? = null
        private var nextSearchTick = 0
        private var progressTick = 0
        private var bestDistance = Double.POSITIVE_INFINITY
        var failed = false
        var madeProgress = false

        fun currentWaypoint(): SurfaceWaypoint? = route.getOrNull(index)

        fun pursue(box: AABB, speed: Double) {
            request(box.center, speed, box)
        }

        fun delayRetries(ticks: Int) {
            nextSearchTick = max(nextSearchTick, tickCount + ticks)
        }

        override fun moveTo(entity: Entity, speed: Double): Boolean = request(entity.position(), speed, null)

        override fun moveTo(x: Double, y: Double, z: Double, speed: Double): Boolean =
            request(Vec3(x, y, z), speed, null)

        private fun request(wanted: Vec3, speed: Double, box: AABB?): Boolean {
            speedModifier = speed
            val moved = destination?.distanceToSqr(wanted)?.let { it > 2.25 } ?: true
            if (tickCount < nextSearchTick || search != null || !moved && !isDone) return !isDone
            val start = surfaceProbe.start()
            destination = wanted
            attackBox = box
            nextSearchTick = tickCount + 20
            if (start == null) {
                failed = true
                return false
            }
            // 搜索期间保留旧路线，完成后才替换，移动目标不会让实体不断停下。
            search = SurfaceSearch(
                start, wanted, getAttributeValue(Attributes.FOLLOW_RANGE), surfaceProbe::nodes,
                { surfaceProbe.clear(it.feet()) }, surfaceProbe::clearSegment
            ) { node ->
                val feet = node.feet()
                if (box != null) canAttackFrom(feet, box, 0.85) else feet.distanceToSqr(wanted) <= 1.0
            }
            return true
        }

        override fun tick() {
            if (!isAlive || isNoAi || isPassenger || isInWaterOrBubble || isInLava || tickCount < detachUntilTick) {
                stop()
                return
            }
            search?.let { pending ->
                pending.step(SEARCH_NODES_PER_TICK)
                if (pending.done) {
                    search = null
                    val result = pending.result
                    if (result == null) {
                        failed = true
                    } else {
                        // 搜索时实体可能沿旧路前进；只接入仍然可以直接、安全到达的最近路点。
                        val startIndex = result.indices.filter {
                            result[it].position.distanceToSqr(position()) < 2.25 &&
                                    surfaceProbe.clearSegment(position(), result[it].position)
                        }.minByOrNull { result[it].position.distanceToSqr(position()) }
                        if (startIndex == null) {
                            nextSearchTick = tickCount
                        } else {
                            route = result
                            index = startIndex
                            resetProgress()
                        }
                    }
                }
            }
            var next = currentWaypoint() ?: return
            if (position().distanceToSqr(next.position) < 1.0E-6) {
                if (index > 0 && next.position.distanceToSqr(route[index - 1].position) > 0.0625) {
                    madeProgress = true
                }
                index++
                resetProgress()
                next = currentWaypoint() ?: return
            }
            if (!surfaceProbe.valid(next.node) || !surfaceProbe.valid(next.from) ||
                !surfaceProbe.clearSegment(position(), next.position)
            ) {
                invalidate()
                return
            }
            val distance = position().distanceToSqr(next.position)
            if (distance < bestDistance - 0.01) {
                if (bestDistance.isFinite()) madeProgress = true
                bestDistance = distance
                progressTick = tickCount
            } else if (tickCount - progressTick > 40) {
                invalidate()
                return
            }
            if (surfaceProbe.touches(boundingBox, next.node.face)) {
                attachedFace = next.node.face
                surfaceAttached = true
            }
            moveControl.setWantedPosition(next.position.x, next.position.y, next.position.z, speedModifier)
        }

        private fun resetProgress() {
            bestDistance = Double.POSITIVE_INFINITY
            progressTick = tickCount
        }

        private fun invalidate() {
            route = emptyList()
            search = null
            nextSearchTick = tickCount + 10
            failed = true
        }

        override fun isDone(): Boolean = search == null && index >= route.size

        override fun getTargetPos(): BlockPos? = destination?.let(BlockPos::containing)

        override fun recomputePath() {
            val wanted = destination ?: return
            search = null
            route = emptyList()
            index = 0
            nextSearchTick = tickCount
            request(wanted, speedModifier, attackBox)
        }

        override fun stop() {
            super.stop()
            search = null
            route = emptyList()
            index = 0
            destination = null
            attackBox = null
            failed = false
            madeProgress = false
            surfaceVelocity = Vec3.ZERO
        }
    }

    private inner class SurfaceMoveControl : MoveControl(this@CreepingSenpaiEntity) {
        override fun tick() {
            if (!surfaceAttached || isInWaterOrBubble || isInLava) {
                super.tick()
                return
            }
            val moving = hasWanted()
            operation = Operation.WAIT
            speed = 0f
            setXxa(0f)
            setYya(0f)
            setZza(0f)
            if (!moving || !isAlive) return
            val delta = Vec3(wantedX, wantedY, wantedZ).subtract(position())
            val speed = speedModifier * getAttributeValue(Attributes.MOVEMENT_SPEED)
            surfaceVelocity = delta.normalize().scale(min(speed, delta.length()))
            val normal = attachedFace
            faceSurfaceDirection(delta)
            if (!normal.axis.isHorizontal && delta.horizontalDistanceSqr() > 1.0E-6) {
                val yaw = (Mth.atan2(delta.z, delta.x) * 180.0 / Math.PI - 90.0).toFloat()
                yRot = rotlerp(yRot, yaw, 45f)
                setYHeadRot(yRot)
                setYBodyRot(yRot)
            }
        }
    }

    companion object {
        @JvmField
        val ATTACHED_FACE: EntityDataAccessor<Direction> =
            SynchedEntityData.defineId(CreepingSenpaiEntity::class.java, EntityDataSerializers.DIRECTION)

        private val SURFACE_HEADING: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(CreepingSenpaiEntity::class.java, EntityDataSerializers.FLOAT)

        private const val TARGET_MEMORY_TICKS = 160
        private const val SEARCH_NODES_PER_TICK = 96
        private const val MAX_SEARCH_NODES = 4096

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
