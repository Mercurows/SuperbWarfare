package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.entity.OBBEntity
import com.atsuishio.superbwarfare.entity.living.DPSGeneratorEntity
import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback
import com.atsuishio.superbwarfare.entity.mixin.OBBHitter
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.misc.TranscriptItem
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
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
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
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.VoxelShape
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

            val tags = stack.getOrCreateTag().getList(TranscriptItem.TAG_SCORES, Tag.TAG_COMPOUND.toInt())

            val queue: Queue<CompoundTag> = ArrayDeque<CompoundTag>()
            for (i in tags.indices) {
                queue.add(tags.getCompound(i))
            }

            val tag = CompoundTag()
            tag.putInt("Score", score)
            tag.putDouble("Distance", distance)
            queue.offer(tag)

            while (queue.size > size) {
                queue.poll()
            }

            val newTags = ListTag()
            newTags.addAll(queue)

            stack.getOrCreateTag().put(TranscriptItem.TAG_SCORES, newTags)
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
                var collisionHit: Vec3? = null
                for (obb in entity.getOBBs()) {
                    if (obb.part == OBB.Part.COLLISION) {
                        // Save COLLISION OBB for fallback, but check BODY OBBs first
                        val obbVec = obb.clip(vec3ToVector3d(startVec), vec3ToVector3d(endVec)).orElse(null)
                        if (obbVec != null) {
                            collisionHit = vector3dToVec3(obbVec)
                        }
                        continue
                    }
                    val obbVec = obb.clip(vec3ToVector3d(startVec), vec3ToVector3d(endVec)).orElse(null)
                    if (obbVec != null) {
                        val hitPos = vector3dToVec3(obbVec)
                        OBBHitter.getInstance(projectile).`sbw$setCurrentHitPart`(obb.part)
                        return hitPos
                    }
                }
                // Fallback: use COLLISION OBB if no BODY OBB was hit (covers gaps on large vehicles)
                return collisionHit
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
