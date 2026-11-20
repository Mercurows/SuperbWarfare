package com.atsuishio.superbwarfare.compat.sable

import com.atsuishio.superbwarfare.compat.CompatHolder
import com.atsuishio.superbwarfare.entity.projectile.IAdvancedHitDetection
import com.atsuishio.superbwarfare.tools.CustomExplosion
import dev.ryanhcode.sable.companion.SableCompanion
import dev.ryanhcode.sable.companion.math.BoundingBox3d
import dev.ryanhcode.sable.sublevel.SubLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Position
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.ClipContext.Block
import net.minecraft.world.level.ClipContext.Fluid
import net.minecraft.world.level.ExplosionDamageCalculator
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.neoforged.fml.ModList
import org.joml.Vector3d
import java.util.function.Predicate

object SableCompatHandler {

    @JvmStatic
    fun hasMod(): Boolean {
        return ModList.get().isLoaded(CompatHolder.SABLE)
    }

    @JvmStatic
    fun collectSubLevelBlockCollisions(level: Level, worldSearchBox: AABB, targetList: MutableList<AABB>) {
        try {
            val searchBounds = BoundingBox3d(
                worldSearchBox.minX, worldSearchBox.minY, worldSearchBox.minZ,
                worldSearchBox.maxX, worldSearchBox.maxY, worldSearchBox.maxZ
            )
            val subLevels = SableCompanion.INSTANCE.getAllIntersecting(level, searchBounds)
            for (subLevelAccess in subLevels) {
                val subLevel = subLevelAccess as? SubLevel ?: continue
                val subLevelWorld = subLevel.level ?: continue
                val pose = subLevelAccess.logicalPose()

                val localCenter = JOMLConversion.toJOML(worldSearchBox.center).let {
                    pose.transformPositionInverse(it)
                }
                val hw = worldSearchBox.xsize / 2.0
                val hh = worldSearchBox.ysize / 2.0
                val hd = worldSearchBox.zsize / 2.0
                val localSearchBox = AABB(
                    localCenter.x - hw, localCenter.y - hh, localCenter.z - hd,
                    localCenter.x + hw, localCenter.y + hh, localCenter.z + hd
                )

                for (shape in subLevelWorld.getBlockCollisions(null, localSearchBox)) {
                    for (aabb in shape.toAabbs()) {
                        targetList.add(transformAabbToWorld(pose, aabb))
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * 带Sable物理结构支持的射线方块碰撞检测。
     * 当安装Sable mod后，投射物能够正确判定物理体（SubLevel等）上面的方块碰撞。
     *
     * 工作原理：
     * 1. 先执行原版世界的射线检测（rayTraceBlocks）
     * 2. 查找射线路径上与Sable物理结构相交的SubLevel
     * 3. 将射线变换到SubLevel的局部空间，在其内部执行射线检测
     * 4. 将命中结果变换回世界空间，与原版结果比较取最近者
     *
     * @param world 主世界Level
     * @param context 射线上下文（包含起点、终点、碰撞模式、流体模式等）
     * @param ignorePredicate 忽略的方块状态判定，与IAdvancedHitDetection.rayTraceBlocks的语义一致
     * @return 最近的方块命中结果（包含Sable物理结构上的方块），若无命中则返回HitResult.miss
     */
    @JvmStatic
    fun rayTraceBlocksWithSable(
        world: Level,
        context: ClipContext,
        ignorePredicate: Predicate<BlockState>
    ): BlockHitResult {
        // 1. 先执行原版世界的射线检测
        val vanillaResult = IAdvancedHitDetection.rayTraceBlocks(world, context, ignorePredicate)

        val startVec = context.from
        val endVec = context.to
        var closestResult = vanillaResult
        var closestDistSqr = if (vanillaResult.type == HitResult.Type.MISS)
            Double.MAX_VALUE
        else
            startVec.distanceToSqr(vanillaResult.location)

        // 2. 查找射线路径上与Sable物理结构相交的SubLevel
        try {
            val rayBox = AABB(
                minOf(startVec.x, endVec.x), minOf(startVec.y, endVec.y), minOf(startVec.z, endVec.z),
                maxOf(startVec.x, endVec.x), maxOf(startVec.y, endVec.y), maxOf(startVec.z, endVec.z)
            )
            val searchBounds = BoundingBox3d(
                rayBox.minX, rayBox.minY, rayBox.minZ,
                rayBox.maxX, rayBox.maxY, rayBox.maxZ
            )
            val subLevels = SableCompanion.INSTANCE.getAllIntersecting(world, searchBounds)

            for (subLevelAccess in subLevels) {
                val subLevel = subLevelAccess as? SubLevel ?: continue
                val subLevelWorld = subLevel.level ?: continue
                val pose = subLevelAccess.logicalPose()

                // 将射线端点变换到SubLevel的局部空间
                val localStartJOML = pose.transformPositionInverse(
                    Vector3d(startVec.x, startVec.y, startVec.z)
                )
                val localEndJOML = pose.transformPositionInverse(
                    Vector3d(endVec.x, endVec.y, endVec.z)
                )
                val localStart = Vec3(localStartJOML.x, localStartJOML.y, localStartJOML.z)
                val localEnd = Vec3(localEndJOML.x, localEndJOML.y, localEndJOML.z)

                // 在SubLevel的局部空间内进行射线检测
                // 传入null作为entity：投射物实体不在SubLevel的World中，但碰撞形状检测不依赖具体实体
                val localContext = ClipContext(
                    localStart, localEnd, Block.COLLIDER, Fluid.NONE, CollisionContext.empty()
                )
                val localResult = IAdvancedHitDetection.rayTraceBlocks(
                    subLevelWorld, localContext, ignorePredicate
                )

                if (localResult.type != HitResult.Type.MISS) {
                    // 将命中点变换回世界空间
                    val localHit = localResult.location
                    val worldHitJOML = pose.transformPosition(
                        Vector3d(localHit.x, localHit.y, localHit.z)
                    )
                    val worldHit = Vec3(worldHitJOML.x, worldHitJOML.y, worldHitJOML.z)
                    val distSqr = startVec.distanceToSqr(worldHit)

                    if (distSqr < closestDistSqr) {
                        closestDistSqr = distSqr

                        // 变换面法线方向到世界空间（仅旋转，去除平移分量）
                        val origin = pose.transformPosition(Vector3d(0.0, 0.0, 0.0))
                        val localDir = Vector3d(
                            localResult.direction.stepX.toDouble(),
                            localResult.direction.stepY.toDouble(),
                            localResult.direction.stepZ.toDouble()
                        )
                        val worldDirJOML = pose.transformPosition(localDir)
                        val worldDir = Vector3d(
                            worldDirJOML.x - origin.x,
                            worldDirJOML.y - origin.y,
                            worldDirJOML.z - origin.z
                        )
                        val worldDirection = Direction.getNearest(worldDir.x, worldDir.y, worldDir.z)

                        // 构造世界空间中的BlockHitResult
                        // BlockPos使用世界坐标近似，用于后续粒子/音效等处理
                        closestResult = BlockHitResult(
                            worldHit,
                            worldDirection,
                            BlockPos.containing(worldHit),
                            localResult.isInside
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        return closestResult
    }

    /**
     * 对Sable物理结构（SubLevel等）上的方块执行爆炸破坏处理。
     *
     * 工作原理：
     * 1. 查找爆炸范围内所有相交的SubLevel
     * 2. 对每个SubLevel，将爆炸中心变换到局部空间
     * 3. 在局部空间内遍历方块，变换回世界空间检查扁平化距离
     * 4. 复用与CustomExplosion一致的抗力/伤害计算公式
     * 5. 对通过判定的方块，调用onExplosionHit在SubLevel世界中摧毁并生成掉落物
     * 6. 将掉落物位置变换到世界空间，在主世界中生成物品实体
     *
     * @param world 主世界Level（用于查找SubLevel和生成掉落物）
     * @param center 爆炸中心的世界坐标
     * @param radius 爆炸半径
     * @param damage 爆炸伤害值（影响方块破坏力度计算）
     * @param explosion 爆炸实例（用于shouldBlockExplode和onExplosionHit回调）
     * @param damageCalculator 爆炸伤害计算器
     */
    @JvmStatic
    fun processExplosionOnSubLevels(
        world: Level,
        center: Vec3,
        radius: Float,
        damage: Float,
        explosion: CustomExplosion,
        damageCalculator: ExplosionDamageCalculator,
        blockDestructionRadius: Float?,
        spherical: Boolean,
        dropBlocks: Boolean
    ) {
        try {
            val reach = blockDestructionRadius ?: (0.6f * radius)
            val verticalScale = if (spherical) 1f else 3f
            val searchAABB = AABB(
                center.x - reach, center.y - reach, center.z - reach,
                center.x + reach, center.y + reach, center.z + reach
            )
            val searchBounds = BoundingBox3d(
                searchAABB.minX, searchAABB.minY, searchAABB.minZ,
                searchAABB.maxX, searchAABB.maxY, searchAABB.maxZ
            )
            val subLevels = SableCompanion.INSTANCE.getAllIntersecting(world, searchBounds)

            for (subLevelAccess in subLevels) {
                val subLevel = subLevelAccess as? SubLevel ?: continue
                val subLevelWorld = subLevel.level ?: continue
                val pose = subLevelAccess.logicalPose()

                // 将爆炸中心变换到SubLevel的局部空间
                val localCenterJOML = pose.transformPositionInverse(
                    Vector3d(center.x, center.y, center.z)
                )
                val localCenter = Vec3(localCenterJOML.x, localCenterJOML.y, localCenterJOML.z)

                // Intersect the explosion bounds with this plot before reading any block states.
                val bounds = subLevel.plot.boundingBox ?: continue
                val min = BlockPos.containing(localCenter.x - reach, localCenter.y - reach, localCenter.z - reach)
                val max = BlockPos.containing(localCenter.x + reach, localCenter.y + reach, localCenter.z + reach)
                val minX = maxOf(min.x, bounds.minX())
                val minY = maxOf(min.y, bounds.minY())
                val minZ = maxOf(min.z, bounds.minZ())
                val maxX = minOf(max.x, bounds.maxX())
                val maxY = minOf(max.y, bounds.maxY())
                val maxZ = minOf(max.z, bounds.maxZ())
                if (minX > maxX || minY > maxY || minZ > maxZ) continue

                val random = subLevelWorld.random
                val qualified = mutableListOf<BlockPos>()
                for (localBlockPos in BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
                    val blockState = subLevelWorld.getBlockState(localBlockPos)
                    if (blockState.isAir) continue
                    var resistance = blockState.block.defaultDestroyTime()
                    if (resistance < 0f) continue
                    val soundType = blockState.getSoundType(subLevelWorld, localBlockPos, explosion.directSourceEntity)
                    if (soundType === SoundType.METAL || soundType === SoundType.COPPER ||
                        soundType === SoundType.NETHERITE_BLOCK) resistance *= 3f

                    val worldBlockCenter = pose.transformPosition(
                        Vector3d(localBlockPos.x + 0.5, localBlockPos.y + 0.5, localBlockPos.z + 0.5)
                    )
                    val dx = worldBlockCenter.x - center.x
                    val dy = worldBlockCenter.y - center.y
                    val dz = worldBlockCenter.z - center.z
                    val distanceSquared = dx * dx + dy * dy + dz * dz
                    val blockDistanceSquared = distanceSquared + (verticalScale - 1f) * dy * dy
                    val effectiveRadius = blockDestructionRadius?.toDouble() ?: run {
                        var legacy = 0.4 * radius
                        if (distanceSquared > radius * radius * 0.15) {
                            legacy += (random.nextDouble() - 0.5) * radius * 0.2
                        }
                        legacy * 1.2
                    }
                    val falloff = 1.0 - blockDistanceSquared / (effectiveRadius * effectiveRadius)
                    if (falloff <= 0.0) continue
                    val force = (radius * (0.25f + random.nextFloat() * 0.15f) * 0.02f * damage * falloff).toFloat()
                    if (force > resistance && damageCalculator.shouldBlockExplode(
                            explosion, subLevelWorld, localBlockPos, blockState, force
                        )) {
                        qualified.add(localBlockPos.immutable())
                    }
                }

                // 摧毁SubLevel上的方块，并将掉落物生成在主世界中
                for (localBlockPos in qualified) {
                    val blockState = subLevelWorld.getBlockState(localBlockPos)
                    if (blockState.isAir) continue
                    if (!dropBlocks) {
                        explosion.removeBlockWithoutDrops(subLevelWorld, localBlockPos, blockState)
                        continue
                    }

                    // 计算掉落物应生成的世界坐标
                    val localDropCenter = Vector3d(
                        localBlockPos.x + 0.5, localBlockPos.y + 0.5, localBlockPos.z + 0.5
                    )
                    val worldDropCenter = pose.transformPosition(localDropCenter)
                    val worldDropPos = BlockPos.containing(
                        worldDropCenter.x, worldDropCenter.y, worldDropCenter.z
                    )

                    // 收集掉落物，然后在主世界中生成
                    val dropList = mutableListOf<Pair<ItemStack, BlockPos>>()
                    blockState.onExplosionHit(subLevelWorld, localBlockPos, explosion) { stack, _ ->
                        addOrAppendStack(dropList, stack, worldDropPos)
                    }

                    for ((stack, pos) in dropList) {
                        net.minecraft.world.level.block.Block.popResource(world, pos, stack)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * 合并可堆叠的掉落物，与CustomExplosion.addOrAppendStack逻辑一致
     */
    private fun addOrAppendStack(
        drops: MutableList<Pair<ItemStack, BlockPos>>,
        stack: ItemStack,
        pos: BlockPos
    ) {
        for (i in drops.indices) {
            val pair = drops[i]
            val itemstack = pair.first
            if (ItemEntity.areMergable(itemstack, stack)) {
                drops[i] = Pair(
                    ItemEntity.merge(itemstack, stack, 16),
                    pair.second
                )
                if (stack.isEmpty) {
                    return
                }
            }
        }
        drops.add(Pair(stack, pos))
    }

    @JvmStatic
    fun isOnSubLevelGround(level: Level, testObbAabb: AABB): Boolean {
        return try {
            val searchBounds = BoundingBox3d(
                testObbAabb.minX, testObbAabb.minY, testObbAabb.minZ,
                testObbAabb.maxX, testObbAabb.maxY, testObbAabb.maxZ
            )
            val subLevels = SableCompanion.INSTANCE.getAllIntersecting(level, searchBounds)
            for (subLevelAccess in subLevels) {
                val subLevel = subLevelAccess as? SubLevel ?: continue
                val subLevelWorld = subLevel.level ?: continue
                val pose = subLevelAccess.logicalPose()

                val localCenter = JOMLConversion.toJOML(testObbAabb.center).let {
                    pose.transformPositionInverse(it)
                }
                val localBox = AABB(
                    localCenter.x - testObbAabb.xsize / 2.0,
                    localCenter.y - testObbAabb.ysize / 2.0,
                    localCenter.z - testObbAabb.zsize / 2.0,
                    localCenter.x + testObbAabb.xsize / 2.0,
                    localCenter.y + testObbAabb.ysize / 2.0,
                    localCenter.z + testObbAabb.zsize / 2.0
                )

                for (pos in BlockPos.betweenClosedStream(
                    BlockPos.containing(localBox.minX, localBox.minY, localBox.minZ),
                    BlockPos.containing(localBox.maxX, localBox.maxY, localBox.maxZ)
                )) {
                    val state = subLevelWorld.getBlockState(pos)
                    if (!state.isAir) {
                        val shape = state.getCollisionShape(subLevelWorld, pos)
                        if (!shape.isEmpty) return true
                    }
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun transformAabbToWorld(pose: dev.ryanhcode.sable.companion.math.Pose3dc, local: AABB): AABB {
        val corners = arrayOf(
            Vector3d(local.minX, local.minY, local.minZ),
            Vector3d(local.minX, local.minY, local.maxZ),
            Vector3d(local.minX, local.maxY, local.minZ),
            Vector3d(local.minX, local.maxY, local.maxZ),
            Vector3d(local.maxX, local.minY, local.minZ),
            Vector3d(local.maxX, local.minY, local.maxZ),
            Vector3d(local.maxX, local.maxY, local.minZ),
            Vector3d(local.maxX, local.maxY, local.maxZ),
        )
        var wxMin = Double.MAX_VALUE
        var wyMin = Double.MAX_VALUE
        var wzMin = Double.MAX_VALUE
        var wxMax = -Double.MAX_VALUE
        var wyMax = -Double.MAX_VALUE
        var wzMax = -Double.MAX_VALUE
        for (c in corners) {
            val w = pose.transformPosition(c)
            if (w.x < wxMin) wxMin = w.x
            if (w.y < wyMin) wyMin = w.y
            if (w.z < wzMin) wzMin = w.z
            if (w.x > wxMax) wxMax = w.x
            if (w.y > wyMax) wyMax = w.y
            if (w.z > wzMax) wzMax = w.z
        }
        return AABB(wxMin, wyMin, wzMin, wxMax, wyMax, wzMax)
    }

    private object JOMLConversion {
        fun toJOML(pos: Position): Vector3d {
            return Vector3d(pos.x(), pos.y(), pos.z())
        }
    }
}
