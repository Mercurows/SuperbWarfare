package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.entity.OBBEntity
import com.atsuishio.superbwarfare.entity.living.DPSGeneratorEntity
import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback
import com.atsuishio.superbwarfare.entity.mixin.OBBHitter
import com.atsuishio.superbwarfare.init.ModDataComponents
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.HitboxHelper.getBoundingBox
import com.atsuishio.superbwarfare.tools.HitboxHelper.getVelocity
import com.atsuishio.superbwarfare.tools.OBB.Companion.vec3ToVector3d
import com.atsuishio.superbwarfare.tools.OBB.Companion.vector3dToVec3
import com.atsuishio.superbwarfare.world.phys.EntityResult
import net.minecraft.core.BlockPos
import net.minecraft.core.BlockPos.MutableBlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.util.*
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Predicate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * 高级命中检测接口 — 标记一个投射物具有不同于原版的命中判定逻辑，
 * 包括 OBB 碰撞检测、爆头/打腿判定、穿甲伤害计算、多目标穿透等。
 *
 * 实现此接口的实体需要提供这些高级命中判定方法
 */
interface IAdvancedHitDetection {
    /**
     * 在路径上查找所有可命中实体
     */
    fun findEntitiesOnPath(startVec: Vec3, endVec: Vec3): MutableList<EntityResult> {
        if (this !is Projectile) return mutableListOf()
        val hitEntities: MutableList<EntityResult> = arrayListOf()
        val entities = this.level().getEntities(
            this,
            this.boundingBox
                .expandTowards(this.deltaMovement)
                .inflate(1.0),
            PROJECTILE_TARGETS
        )
        for (entity in entities) {
            if (entity == this.owner || this.owner != null
                && (entity == this.owner!!.vehicle || entity.rootVehicle === this.owner!!.rootVehicle)
            ) continue

            if (entity is TargetEntity && entity.getEntityData().get(TargetEntity.DOWN_TIME) > 0) continue
            if (entity is DPSGeneratorEntity && entity.getEntityData().get(DPSGeneratorEntity.DOWN_TIME) > 0) continue

            val result = this.getHitResult(entity, startVec, endVec) ?: continue
            hitEntities.add(result)
        }
        return hitEntities
    }

    /**
     * 对单个实体执行 OBB + AABB 混合碰撞检测，返回命中结果（含爆头/打腿信息）
     *
     * Based on TaC-Z
     */
    fun getHitResult(entity: Entity, startVec: Vec3, endVec: Vec3): EntityResult? {
        if (this !is Projectile) return null

        val expandHeight = if (entity is Player && !entity.isCrouching) 0.0625 else 0.0
        var hitPos: Vec3? = null
        if (entity is OBBEntity && !entity.enableAABB()) {
            var closestDistSqr = Double.MAX_VALUE
            var closestHitPos: Vec3? = null
            var closestHitPart: OBB.Part? = null
            var collisionHitPos: Vec3? = null
            var collisionDistSqr = Double.MAX_VALUE

            for (obb in entity.getOBBs()) {
                val obbVec = obb.clip(startVec.toVector3d(), endVec.toVector3d()).orElse(null) ?: continue
                val vec = obbVec.toVec3()
                val distSqr = startVec.distanceToSqr(vec)

                if (obb.part == OBB.Part.COLLISION) {
                    if (distSqr < collisionDistSqr) {
                        collisionDistSqr = distSqr
                        collisionHitPos = vec
                    }
                } else {
                    if (distSqr < closestDistSqr) {
                        closestDistSqr = distSqr
                        closestHitPos = vec
                        closestHitPart = obb.part
                    }
                }
            }

            // 优先使用非 COLLISION 命中，否则回退到 COLLISION（覆盖大型载具的间隙）
            if (closestHitPos != null) {
                hitPos = closestHitPos
                val acc = OBBHitter.getInstance(this)
                acc.`sbw$setCurrentHitPart`(closestHitPart!!)
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
                }
            } else if (collisionHitPos != null) {
                hitPos = collisionHitPos
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

    /**
     * 执行带穿甲/爆头倍率的伤害
     */
    fun performDamage(entity: Entity, damage: Float, isHeadshot: Boolean)

    /**
     * 执行命中生物后添加药水效果
     */
    fun performAddEffect(entity: Entity, damage: Float, isHeadshot: Boolean) {
        if (this !is IBulletProperties) return
        if (entity.level().isClientSide) return
        if (this.getEffects().isNotEmpty() && entity is LivingEntity) {
            this.getEffects().forEach {
                entity.addEffect(it)
            }
        }
    }

    /**
     * 执行命中后的伤害施加与击退
     */
    fun performOnHit(entity: Entity, damage: Float, headshot: Boolean, knockback: Double) {
        if (this !is IBulletProperties) return
        if (this !is Projectile) return

        if (entity is LivingEntity) {
            if (this.isForceKnockback()) {
                val vec3 = this.deltaMovement.multiply(1.0, 0.0, 1.0).normalize()
                entity.addDeltaMovement(vec3.scale(knockback))
                performDamage(entity, damage, headshot)
            } else {
                val iCustomKnockback = ICustomKnockback.getInstance(entity)
                iCustomKnockback.`superbWarfare$setKnockbackStrength`(knockback)
                performDamage(entity, damage, headshot)
                iCustomKnockback.`superbWarfare$resetKnockbackStrength`()
            }
            performAddEffect(entity, damage, headshot)
        } else {
            performDamage(entity, damage, headshot)
        }
    }

    /**
     * 记录靶环分数（用于训练场计分）
     */
    fun recordHitScore(direction: Direction, hitVec: Vec3) {
        if (this !is Projectile) return

        val shooter = this.owner as? Player ?: return
        val score = getRings(direction, hitVec)
        val distance = shooter.position().distanceTo(hitVec)

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
            queue.offer(com.mojang.datafixers.util.Pair(score, distance))

            while (queue.size > size) {
                queue.poll()
            }

            stack.set(
                ModDataComponents.TRANSCRIPT_SCORE,
                queue.toList()
            )
        }
    }

    companion object {
        @JvmField
        val PROJECTILE_TARGETS =
            Predicate { input: Entity? -> input != null && input.isPickable && !input.isSpectator && input.isAlive }

        /**
         * 静态 OBB raycast —— 返回世界空间的命中位置，若射线不与任何非COLLISION OBB相交则返回null。
         * 供 FastThrowableProjectile 子类绕过原版命中检测时使用。
         */
        @JvmStatic
        fun clipObb(projectile: Entity, entity: Entity, startVec: Vec3, endVec: Vec3): Vec3? {
            if (entity is OBBEntity && !entity.enableAABB()) {
                var closestDistSqr = Double.MAX_VALUE
                var closestHitPos: Vec3? = null
                var closestHitPart: OBB.Part? = null
                var collisionHitPos: Vec3? = null
                var collisionDistSqr = Double.MAX_VALUE

                for (obb in entity.getOBBs()) {
                    val obbVec = obb.clip(vec3ToVector3d(startVec), vec3ToVector3d(endVec)).orElse(null) ?: continue
                    val hitPos = vector3dToVec3(obbVec)
                    val distSqr = startVec.distanceToSqr(hitPos)

                    if (obb.part == OBB.Part.COLLISION) {
                        if (distSqr < collisionDistSqr) {
                            collisionDistSqr = distSqr
                            collisionHitPos = hitPos
                        }
                    } else {
                        if (distSqr < closestDistSqr) {
                            closestDistSqr = distSqr
                            closestHitPos = hitPos
                            closestHitPart = obb.part
                        }
                    }
                }

                if (closestHitPos != null) {
                    OBBHitter.getInstance(projectile).`sbw$setCurrentHitPart`(closestHitPart!!)
                    return closestHitPos
                }
                // Fallback: use COLLISION OBB if no non-COLLISION OBB was hit (covers gaps on large vehicles)
                return collisionHitPos
            }
            return null
        }

        @JvmStatic
        fun getRings(direction: Direction, hitVec: Vec3): Int {
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

            return max(1, ceil(10.0 * ((0.5 - v) / 0.5).coerceIn(0.0, 1.0)).toInt())
        }

        @JvmStatic
        fun rayTraceBlocks(
            world: Level,
            context: ClipContext,
            ignorePredicate: Predicate<BlockState>
        ): BlockHitResult {
            // 1. Vanilla ray trace against world blocks
            val vanillaHit = rayTraceVanillaBlocks(world, context, ignorePredicate)

            return vanillaHit
        }

        @JvmStatic
        fun rayTraceBlocksWithFluid(
            world: Level,
            context: ClipContext,
            ignorePredicate: Predicate<BlockState>
        ): Pair<BlockHitResult, BlockHitResult> {
            val blockStateGetter = CachedBlockStateGetter(world)
            var blockHit: BlockHitResult? = null
            var fluidAnyHit: BlockHitResult? = null

            val vanillaResult = performRayTrace(
                context,
                { rayTraceContext, blockPos ->
                    val clipResult = clipAt(world, blockStateGetter, rayTraceContext, blockPos, ignorePredicate)
                    if (blockHit == null && clipResult.blockHit != null) {
                        blockHit = clipResult.blockHit
                    }
                    if (fluidAnyHit == null) {
                        fluidAnyHit = chooseClosest(rayTraceContext.from, clipResult.blockHit, clipResult.fluidHit)
                    }
                    if (blockHit != null && fluidAnyHit != null) {
                        return@performRayTrace Pair(blockHit, fluidAnyHit!!)
                    }
                    null
                },
                { rayTraceContext ->
                    Pair(
                        blockHit ?: missResult(rayTraceContext),
                        fluidAnyHit ?: missResult(rayTraceContext)
                    )
                }
            )

            val shipHit = null

            return Pair(
                applyShipHit(context, vanillaResult.first, shipHit),
                applyShipHit(context, vanillaResult.second, shipHit)
            )
        }

        private fun rayTraceVanillaBlocks(
            world: Level,
            context: ClipContext,
            ignorePredicate: Predicate<BlockState>
        ): BlockHitResult {
            val blockStateGetter = CachedBlockStateGetter(world)
            return performRayTrace(
                context,
                { rayTraceContext, blockPos ->
                    val clipResult = clipAt(world, blockStateGetter, rayTraceContext, blockPos, ignorePredicate)
                    chooseClosest(rayTraceContext.from, clipResult.blockHit, clipResult.fluidHit)
                },
                { rayTraceContext -> missResult(rayTraceContext) }
            )
        }

        private fun clipAt(
            world: Level,
            blockStateGetter: CachedBlockStateGetter,
            rayTraceContext: ClipContext,
            blockPos: BlockPos,
            ignorePredicate: Predicate<BlockState>
        ): BlockClipResult {
            val blockState = blockStateGetter.get(blockPos)
            if (ignorePredicate.test(blockState)) {
                return BlockClipResult(null, null)
            }

            val startVec = rayTraceContext.from
            val endVec = rayTraceContext.to
            val blockShape = rayTraceContext.getBlockShape(blockState, world, blockPos)
            val blockResult = world.clipWithInteractionOverride(startVec, endVec, blockPos, blockShape, blockState)

            val fluidState = blockState.fluidState
            val fluidResult = if (fluidState.isEmpty) {
                null
            } else {
                rayTraceContext.getFluidShape(fluidState, world, blockPos).clip(startVec, endVec, blockPos)
            }

            return BlockClipResult(blockResult, fluidResult)
        }

        private fun chooseClosest(
            from: Vec3,
            blockResult: BlockHitResult?,
            fluidResult: BlockHitResult?
        ): BlockHitResult? {
            if (blockResult == null) return fluidResult
            if (fluidResult == null) return blockResult
            val blockDistance = from.distanceToSqr(blockResult.location)
            val fluidDistance = from.distanceToSqr(fluidResult.location)
            return if (blockDistance <= fluidDistance) blockResult else fluidResult
        }

        private fun missResult(rayTraceContext: ClipContext): BlockHitResult {
            val vec3 = rayTraceContext.from.subtract(rayTraceContext.to)
            return BlockHitResult.miss(
                rayTraceContext.to,
                Direction.getNearest(vec3.x, vec3.y, vec3.z),
                BlockPos.containing(rayTraceContext.to)
            )
        }

        private fun applyShipHit(
            context: ClipContext,
            vanillaHit: BlockHitResult,
            shipHit: Pair<Vec3, BlockPos>?
        ): BlockHitResult {
            if (shipHit == null) return vanillaHit

            val (shipHitPos, _) = shipHit
            val shipDistSqr = context.from.distanceToSqr(shipHitPos)
            val vanillaDistSqr = if (vanillaHit.type != HitResult.Type.MISS)
                context.from.distanceToSqr(vanillaHit.location)
            else
                Double.MAX_VALUE

            if (shipDistSqr < vanillaDistSqr) {
                val dir = context.from.subtract(shipHitPos)
                val projectileBlockPos = BlockPos.containing(context.from)
                return BlockHitResult(
                    shipHitPos,
                    Direction.getNearest(dir.x, dir.y, dir.z),
                    projectileBlockPos,
                    false
                )
            }
            return vanillaHit
        }

        private class CachedBlockStateGetter(private val world: Level) {
            private var chunkX = Int.MIN_VALUE
            private var chunkZ = Int.MIN_VALUE
            private var chunk: LevelChunk? = null

            fun get(blockPos: BlockPos): BlockState {
                if (world.isOutsideBuildHeight(blockPos)) {
                    return Blocks.VOID_AIR.defaultBlockState()
                }

                val x = blockPos.x shr 4
                val z = blockPos.z shr 4
                if (x != chunkX || z != chunkZ || chunk == null) {
                    chunkX = x
                    chunkZ = z
                    chunk = world.getChunk(x, z)
                }
                return chunk!!.getBlockState(blockPos)
            }
        }

        private class BlockClipResult(
            val blockHit: BlockHitResult?,
            val fluidHit: BlockHitResult?
        )

        @JvmStatic
        fun <T> performRayTrace(
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
