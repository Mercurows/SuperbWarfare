package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModTags
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.*
import net.neoforged.neoforge.entity.PartEntity
import java.util.function.Predicate
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object TraceTool {
    /** 单次视线检测允许的最大距离；外部传入的超长距离会被截断，避免几百万格的体素遍历。 */
    private const val MAX_TRACE_DISTANCE = 1024.0

    /** 实体碰撞箱单边上限；超过这个尺寸通常不是正常实体（物理结构/投影代理等），不参与视线检测。 */
    private const val MAX_SANE_ENTITY_SIZE = 512.0

    private fun isFinite(vec: Vec3): Boolean =
        vec.x.isFinite() && vec.y.isFinite() && vec.z.isFinite()

    /**
     * 归一化视线方向
     */
    private fun safeDirection(direction: Vec3): Vec3? {
        if (!isFinite(direction)) return null
        val lengthSqr = direction.lengthSqr()
        if (!lengthSqr.isFinite() || lengthSqr < 1.0E-8) return null
        return direction.scale(1.0 / sqrt(lengthSqr))
    }

    private fun isSaneTraceTarget(entity: Entity, origin: Vec3, maxDistance: Double): Boolean {
        val pos = entity.position()
        if (!isFinite(pos)) return false

        val box = entity.boundingBox
        if (!box.minX.isFinite() || !box.minY.isFinite() || !box.minZ.isFinite() ||
            !box.maxX.isFinite() || !box.maxY.isFinite() || !box.maxZ.isFinite()
        ) return false

        val maxSize = max(max(box.xsize, box.ysize), box.zsize)
        if (maxSize > MAX_SANE_ENTITY_SIZE) return false
        val limit = maxDistance + maxSize
        return pos.distanceToSqr(origin) <= limit * limit
    }

    @JvmStatic
    fun findLookingEntity(entity: Entity?, entityReach: Double): Entity? {
        if (entity == null) return null
        val reach = entityReach.coerceIn(0.0, MAX_TRACE_DISTANCE)
        var distance = reach * reach
        val eyePos = entity.getEyePosition(1.0f)
        var hitResult = entity.pick(reach, 1.0f, false)
        if (hitResult.type != HitResult.Type.MISS) {
            distance = hitResult.getLocation().distanceToSqr(eyePos)
            val blockReach = 5.0
            if (distance > blockReach * blockReach) {
                val pos = hitResult.getLocation()
                hitResult = BlockHitResult.miss(
                    pos,
                    Direction.getNearest(eyePos.x, eyePos.y, eyePos.z),
                    BlockPos.containing(pos)
                )
            }
        }
        val viewVec = entity.getViewVector(1f)
        val toVec = eyePos.add(viewVec.x * reach, viewVec.y * reach, viewVec.z * reach)
        val aabb = entity.boundingBox.expandTowards(viewVec.scale(reach)).inflate(1.0)
        val entityHitResult = ProjectileUtil.getEntityHitResult(
            entity, eyePos, toVec, aabb,
            {
                !it.isSpectator && entity.vehicle !== it && it.isAlive &&
                        isSaneTraceTarget(it, eyePos, reach) && SeekTool.NOT_IN_SMOKE.test(it)
            },
            distance
        )
        if (entityHitResult != null) {
            val targetPos = entityHitResult.getLocation()
            val distanceToTarget = eyePos.distanceToSqr(targetPos)
            if (distanceToTarget > distance || distanceToTarget > reach * reach) {
                hitResult = BlockHitResult.miss(
                    targetPos,
                    Direction.getNearest(viewVec.x, viewVec.y, viewVec.z),
                    BlockPos.containing(targetPos)
                )
            } else if (distanceToTarget < distance) {
                hitResult = entityHitResult
            }
        }
        if (hitResult.type == HitResult.Type.ENTITY) {
            return (hitResult as EntityHitResult).entity
        }
        return null
    }

    @JvmStatic
    fun findMeleeEntity(entity: Entity, entityReach: Double): Entity? {
        val reach = entityReach.coerceIn(0.0, MAX_TRACE_DISTANCE)
        val distance = reach * reach
        val eyePos = entity.getEyePosition(1.0f)
        var hitResult = entity.pick(reach, 1.0f, false)

        val viewVec = entity.getViewVector(1f)
        val toVec = eyePos.add(viewVec.x * reach, viewVec.y * reach, viewVec.z * reach)
        val aabb = entity.boundingBox.expandTowards(viewVec.scale(reach)).inflate(1.0)
        val entityHitResult = ProjectileUtil.getEntityHitResult(
            entity,
            eyePos,
            toVec,
            aabb,
            { !it.isSpectator && entity.vehicle !== it && it.isAlive && isSaneTraceTarget(it, eyePos, reach) },
            distance
        )
        if (entityHitResult != null) {
            hitResult = entityHitResult
        }
        if (hitResult.type == HitResult.Type.ENTITY) {
            return (hitResult as EntityHitResult).entity
        }
        return null
    }

    @JvmStatic
    fun vehicleFindLookingPos(
        shooter: Entity,
        vehicle: VehicleEntity,
        eye: Vec3,
        entityReach: Double,
        partialTick: Float
    ): Vec3? {
        val distance = entityReach * entityReach
        var hitResult = pickNew(eye, 512.0, vehicle)

        val viewVec = vehicle.getViewVec(shooter, partialTick)
        val toVec = eye.add(viewVec.x * entityReach, viewVec.y * entityReach, viewVec.z * entityReach)
        val aabb = vehicle.boundingBox.expandTowards(viewVec.scale(entityReach)).inflate(1.0)
        val entityHitResult = ProjectileUtil.getEntityHitResult(
            vehicle, eye, toVec, aabb,
            {
                !it.isSpectator && it.isAlive && SeekTool.BASIC_FILTER.test(it)
                        && !it.type.`is`(ModTags.EntityTypes.DECOY) && SeekTool.NOT_IN_SMOKE.test(it)
                        && it !== shooter && (it !is Projectile)
            }, distance
        )
        if (entityHitResult != null) {
            hitResult = entityHitResult
        }

        if (hitResult.type == HitResult.Type.ENTITY) {
            return hitResult.location
        }
        return null
    }

    @JvmStatic
    fun playerFindLookingPos(player: Entity, target: Entity, entityReach: Double): Vec3? {
        val reach = entityReach.coerceIn(0.0, MAX_TRACE_DISTANCE)
        val eyePos = player.eyePosition
        if (!isSaneTraceTarget(target, eyePos, reach)) return null

        val distance = reach * reach
        var hitResult = player.pick(reach, 1.0f, false)

        val viewVec = safeDirection(player.getViewVector(1f)) ?: return null
        val toVec = eyePos.add(viewVec.x * reach, viewVec.y * reach, viewVec.z * reach)
        val aabb = target.boundingBox.expandTowards(viewVec.scale(reach)).inflate(1.0)
        val entityHitResult = ProjectileUtil.getEntityHitResult(
            player.level(),
            player,
            eyePos,
            toVec,
            aabb,
            { true },
            distance.toFloat()
        )
        if (entityHitResult != null) {
            hitResult = entityHitResult
        }
        if (hitResult.type == HitResult.Type.ENTITY) {
            return hitResult.location
        }
        return null
    }

    @JvmStatic
    fun cameraFindLookingEntity(player: Player, pos: Vec3, viewVec: Vec3, entityReach: Double): Entity? {
        val reach = entityReach.coerceIn(0.0, MAX_TRACE_DISTANCE)
        val view = safeDirection(viewVec) ?: return null
        var distance = reach * reach
        var hitResult = pickNew(pos, reach, view, player)

        if (hitResult.type != HitResult.Type.MISS) {
            distance = hitResult.getLocation().distanceToSqr(pos)
            val blockReach = 5.0
            if (distance > blockReach * blockReach) {
                hitResult = BlockHitResult.miss(
                    hitResult.getLocation(),
                    Direction.getNearest(pos.x, pos.y, pos.z),
                    BlockPos.containing(hitResult.getLocation())
                )
            }
        }

        val toVec = pos.add(view.x * reach, view.y * reach, view.z * reach)
        val aabb = player.boundingBox.expandTowards(view.scale(reach)).inflate(1.0)
        val entityHitResult = ProjectileUtil.getEntityHitResult(
            player, pos, toVec, aabb,
            {
                !it.isSpectator && it.isAlive
                        && (it !is Projectile) && SeekTool.BASIC_FILTER.test(it)
                        && !it.type.`is`(ModTags.EntityTypes.DECOY) && isSaneTraceTarget(it, pos, reach)
                        && SeekTool.NOT_IN_SMOKE.test(it)
                        && it !== player && it !== player.vehicle
            }, distance
        )
        if (entityHitResult != null) {
            val targetPos = entityHitResult.getLocation()
            val distanceToTarget = pos.distanceToSqr(targetPos)
            if (distanceToTarget > distance || distanceToTarget > reach * reach) {
                hitResult = BlockHitResult.miss(
                    targetPos,
                    Direction.getNearest(viewVec.x, viewVec.y, viewVec.z),
                    BlockPos.containing(targetPos)
                )
            } else if (distanceToTarget < distance) {
                hitResult = entityHitResult
            }
        }
        if (hitResult.type == HitResult.Type.ENTITY) {
            return (hitResult as EntityHitResult).entity
        }
        return null
    }

    @JvmStatic
    fun findLookDecoy(player: Player, pos: Vec3, viewVec: Vec3, entityReach: Double): Entity? {
        val reach = entityReach.coerceIn(0.0, MAX_TRACE_DISTANCE)
        val view = safeDirection(viewVec) ?: return null
        var distance = reach * reach
        var hitResult = pickNew(pos, reach, view, player)

        if (hitResult.type != HitResult.Type.MISS) {
            distance = hitResult.getLocation().distanceToSqr(pos)
            val blockReach = 5.0
            if (distance > blockReach * blockReach) {
                hitResult = BlockHitResult.miss(
                    hitResult.getLocation(),
                    Direction.getNearest(pos.x, pos.y, pos.z),
                    BlockPos.containing(hitResult.getLocation())
                )
            }
        }

        val toVec = pos.add(view.x * reach, view.y * reach, view.z * reach)
        val aabb = player.boundingBox.expandTowards(view.scale(reach)).inflate(2.0)
        val entityHitResult = ProjectileUtil.getEntityHitResult(
            player,
            pos,
            toVec,
            aabb,
            { it.type.`is`(ModTags.EntityTypes.DECOY) && isSaneTraceTarget(it, pos, reach) },
            distance
        )
        if (entityHitResult != null) {
            val targetPos = entityHitResult.getLocation()
            val distanceToTarget = pos.distanceToSqr(targetPos)
            if (distanceToTarget > distance || distanceToTarget > reach * reach) {
                hitResult = BlockHitResult.miss(
                    targetPos,
                    Direction.getNearest(view.x, view.y, view.z),
                    BlockPos.containing(targetPos)
                )
            } else if (distanceToTarget < distance) {
                hitResult = entityHitResult
            }
        }
        if (hitResult.type == HitResult.Type.ENTITY) {
            return (hitResult as EntityHitResult).entity
        }
        return null
    }

    @JvmStatic
    fun pickNew(pos: Vec3, pHitDistance: Double, vehicle: VehicleEntity): HitResult {
        val direction = safeDirection(vehicle.getBarrelVector(1f))
            ?: return BlockHitResult.miss(pos, Direction.DOWN, BlockPos.containing(pos))
        val distance = pHitDistance.coerceIn(0.0, MAX_TRACE_DISTANCE)
        val vec32 = pos.add(direction.scale(distance))
        return vehicle.level().clip(ClipContext(pos, vec32, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, vehicle))
    }

    @JvmStatic
    fun pickNew(pos: Vec3, pHitDistance: Double, viewVec: Vec3, entity: Entity): HitResult {
        val direction = safeDirection(viewVec)
            ?: return BlockHitResult.miss(pos, Direction.DOWN, BlockPos.containing(pos))
        val distance = pHitDistance.coerceIn(0.0, MAX_TRACE_DISTANCE)
        val vec32 = pos.add(direction.scale(distance))
        return entity.level().clip(ClipContext(pos, vec32, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, entity))
    }

    @JvmStatic
    fun getBlocksAlongRay(start: Vec3, direction: Vec3, maxDistance: Double): MutableList<BlockPos> {
        val blocks = arrayListOf<BlockPos>()

        // 标准化方向向量
        val normalizedDir = direction.normalize()

        // DDA算法参数
        val step = 0.1 // 步长（越小精度越高）
        var distance = 0.0
        var lastPos: BlockPos? = null

        while (distance <= maxDistance) {
            val currentPos = start.add(normalizedDir.scale(distance))
            val blockPos = BlockPos(
                floor(currentPos.x).toInt(),
                floor(currentPos.y).toInt(),
                floor(currentPos.z).toInt()
            )

            // 避免重复添加同一方块
            if (lastPos == null || lastPos != blockPos) {
                blocks.add(blockPos)
                lastPos = blockPos
            }

            distance += step
        }

        return blocks
    }

    /**
     * 获取从起点开始，沿方向向量射线上的所有实体
     *
     * @param world           世界对象
     * @param start           射线起点
     * @param direction       方向向量 (不需要标准化，但长度会影响射线速度)
     * @param filterPredicate 可选的实体过滤器 (例如，排除发射者本身，只选择特定类型的实体)
     * @return 一个包含射线击中的所有实体的列表，以及它们与射线交点的最近距离。
     */
    @JvmStatic
    fun getEntitiesAlongVector(
        world: Level,
        start: Vec3,
        direction: Vec3,
        filterPredicate: Predicate<Entity>
    ): MutableList<RayTraceResultEntity> {
        val hitEntities = arrayListOf<RayTraceResultEntity>()
        val maxDistance = direction.length()

        // 1. 标准化方向向量并计算终点
        val normalizedDirection = direction.normalize()
        val end = start.add(normalizedDirection.scale(maxDistance))

        // 2. 创建一个从起点到终点的AABB进行粗筛，减少需要精确检测的实体数量
        val rayBoundingBox = AABB(start, end).inflate(1.0) // 适当扩大边界框

        // 3. 获取在这个粗筛AABB内的所有实体。
        val entitiesInWorld = world.getEntities(null as Entity?, rayBoundingBox, filterPredicate)

        // 4. 遍历这些实体，进行精确的射线与碰撞箱相交测试
        for (entity in entitiesInWorld) {
            // 忽略实体部件（如末影龙的各个部分，它们通常由父实体处理）
            if (entity is PartEntity<*>) {
                continue
            }

            // 获取实体当前tick的碰撞箱
            var entityBoundingBox = entity.boundingBox
            // 可选：稍微扩大碰撞箱，避免因精度问题错过
            entityBoundingBox = entityBoundingBox.inflate(0.3)

            // 进行射线与实体碰撞箱的相交测试
            val distanceToHit = rayIntersectsAABB(start, normalizedDirection, entityBoundingBox, maxDistance)

            if (distanceToHit != null) {
                val hitVec = start.add(normalizedDirection.scale(distanceToHit)) // 计算实际交点坐标
                hitEntities.add(RayTraceResultEntity(entity, distanceToHit, hitVec))
            }
        }

        // 5. 根据距离排序，返回从近到远的列表
        hitEntities.sortBy { it.distance }
        return hitEntities
    }

    /**
     * 射线与轴向包围盒（AABB）的相交测试
     * 使用经典的SLAB方法
     *
     * @param start   射线起点
     * @param dir     标准化后的射线方向
     * @param box     实体的AABB
     * @param maxDist 射线最大长度
     * @return 如果相交，返回相交的最近距离值t；否则返回null
     */
    private fun rayIntersectsAABB(start: Vec3, dir: Vec3, box: AABB, maxDist: Double): Double? {
        var tMin = 0.0
        var tMax = maxDist

        // 分别检查X轴
        val invDx = 1 / dir.x
        var t0x = (box.minX - start.x) * invDx
        var t1x = (box.maxX - start.x) * invDx

        if (invDx < 0) {
            val temp = t0x
            t0x = t1x
            t1x = temp
        }

        tMin = max(tMin, t0x)
        tMax = min(tMax, t1x)

        if (tMax <= tMin) {
            return null
        }

        // 检查Y轴
        val invDy = 1 / dir.y
        var t0y = (box.minY - start.y) * invDy
        var t1y = (box.maxY - start.y) * invDy

        if (invDy < 0) {
            val temp = t0y
            t0y = t1y
            t1y = temp
        }

        tMin = max(tMin, t0y)
        tMax = min(tMax, t1y)

        if (tMax <= tMin) {
            return null
        }

        // 检查Z轴
        val invDz = 1 / dir.z
        var t0z = (box.minZ - start.z) * invDz
        var t1z = (box.maxZ - start.z) * invDz

        if (invDz < 0) {
            val temp = t0z
            t0z = t1z
            t1z = temp
        }

        tMin = max(tMin, t0z)
        tMax = min(tMax, t1z)

        if (tMax <= tMin) {
            return null
        }

        // 返回最近的交点距离参数t
        return tMin
    }

    /**
     * 用于存储射线检测结果的数据结构
     */
    class RayTraceResultEntity(
        val entity: Entity?, // 从起点到交点的距离
        val distance: Double, // 射线与实体碰撞箱的交点
        val hitVec: Vec3?
    ) {
        override fun toString(): String {
            return "RayTraceResultEntity{" +
                    "entity=" + entity +
                    ", distance=" + distance +
                    ", hitVec=" + hitVec +
                    '}'
        }
    }
}