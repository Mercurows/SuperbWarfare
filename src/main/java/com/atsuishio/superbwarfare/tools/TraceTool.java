package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class TraceTool {


    public static Entity findLookingEntity(Entity entity, double entityReach) {
        double distance = entityReach * entityReach;
        Vec3 eyePos = entity.getEyePosition(1.0f);
        HitResult hitResult = entity.pick(entityReach, 1.0f, false);
        if (hitResult.getType() != HitResult.Type.MISS) {
            distance = hitResult.getLocation().distanceToSqr(eyePos);
            double blockReach = 5;
            if (distance > blockReach * blockReach) {
                Vec3 pos = hitResult.getLocation();
                hitResult = BlockHitResult.miss(pos, Direction.getNearest(eyePos.x, eyePos.y, eyePos.z), BlockPos.containing(pos));
            }
        }
        Vec3 viewVec = entity.getViewVector(1);
        Vec3 toVec = eyePos.add(viewVec.x * entityReach, viewVec.y * entityReach, viewVec.z * entityReach);
        AABB aabb = entity.getBoundingBox().expandTowards(viewVec.scale(entityReach)).inflate(1);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(entity, eyePos, toVec, aabb,
                p -> !p.isSpectator() && entity.getVehicle() != p && p.isAlive() && SeekTool.NOT_IN_SMOKE.test(p), distance);
        if (entityhitresult != null) {
            Vec3 targetPos = entityhitresult.getLocation();
            double distanceToTarget = eyePos.distanceToSqr(targetPos);
            if (distanceToTarget > distance || distanceToTarget > entityReach * entityReach) {
                hitResult = BlockHitResult.miss(targetPos, Direction.getNearest(viewVec.x, viewVec.y, viewVec.z), BlockPos.containing(targetPos));
            } else if (distanceToTarget < distance) {
                hitResult = entityhitresult;
            }
        }
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            return ((EntityHitResult) hitResult).getEntity();
        }
        return null;
    }

    public static Entity findMeleeEntity(Entity entity, double entityReach) {
        double distance = entityReach * entityReach;
        Vec3 eyePos = entity.getEyePosition(1.0f);
        HitResult hitResult = entity.pick(entityReach, 1.0f, false);

        Vec3 viewVec = entity.getViewVector(1);
        Vec3 toVec = eyePos.add(viewVec.x * entityReach, viewVec.y * entityReach, viewVec.z * entityReach);
        AABB aabb = entity.getBoundingBox().expandTowards(viewVec.scale(entityReach)).inflate(1);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(entity, eyePos, toVec, aabb, p -> !p.isSpectator() && entity.getVehicle() != p && p.isAlive(), distance);
        if (entityhitresult != null) {
            hitResult = entityhitresult;
        }
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            return ((EntityHitResult) hitResult).getEntity();
        }
        return null;
    }

    public static Vec3 vehicleFindLookingPos(Entity shooter, VehicleEntity vehicle, Vec3 eye, double entityReach, float partialTick) {
        double distance = entityReach * entityReach;
        HitResult hitResult = pickNew(eye, 512, vehicle);

        Vec3 viewVec = vehicle.getViewVec(shooter, partialTick);
        Vec3 toVec = eye.add(viewVec.x * entityReach, viewVec.y * entityReach, viewVec.z * entityReach);
        AABB aabb = vehicle.getBoundingBox().expandTowards(viewVec.scale(entityReach)).inflate(1);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(vehicle, eye, toVec, aabb,
                p -> !p.isSpectator() && p.isAlive() && SeekTool.BASIC_FILTER.test(p) && !p.getType().is(ModTags.EntityTypes.DECOY) && SeekTool.NOT_IN_SMOKE.test(p) && p != shooter && !(p instanceof Projectile), distance);
        if (entityhitresult != null) {
            hitResult = entityhitresult;
        }

        if (hitResult.getType() == HitResult.Type.ENTITY) {
            return hitResult.getLocation();
        }
        return null;
    }

    public static Vec3 playerFindLookingPos(Entity pEntity, Entity entity, double entityReach) {
        double distance = entityReach * entityReach;
        HitResult hitResult = pEntity.pick(entityReach, 1.0f, false);

        Vec3 viewVec = pEntity.getViewVector(1);
        Vec3 toVec = pEntity.getEyePosition().add(viewVec.x * entityReach, viewVec.y * entityReach, viewVec.z * entityReach);
        AABB aabb = entity.getBoundingBox().expandTowards(viewVec.scale(entityReach)).inflate(1);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(pEntity.level(), pEntity, pEntity.getEyePosition(), toVec, aabb, p -> true, (float) distance);
        if (entityhitresult != null) {
            hitResult = entityhitresult;

        }
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            return hitResult.getLocation();
        }
        return null;
    }

    public static Entity droneFindLookingEntity(Entity entity, Vec3 pos, double entityReach, float ticks) {
        double distance = entityReach * entityReach;
        HitResult hitResult = entity.pick(entityReach, 1.0f, false);

        Vec3 viewVec = entity.getViewVector(ticks);
        Vec3 toVec = pos.add(viewVec.x * entityReach, viewVec.y * entityReach, viewVec.z * entityReach);
        AABB aabb = entity.getBoundingBox().expandTowards(viewVec.scale(entityReach)).inflate(1);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(entity, pos, toVec, aabb, p -> !p.isSpectator()
                && p.isAlive()
                && !(p instanceof Projectile)
                && SeekTool.BASIC_FILTER.test(p)
                && !p.getType().is(ModTags.EntityTypes.DECOY)
                && SeekTool.NOT_IN_SMOKE.test(p)
                && p != entity
                && p != entity.getVehicle(), distance);
        if (entityhitresult != null) {
            hitResult = entityhitresult;

        }
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            return ((EntityHitResult) hitResult).getEntity();
        }
        return null;
    }

    public static Entity camerafFindLookingEntity(Player player, Vec3 pos, Vec3 viewVec, double entityReach) {
        double distance = entityReach * entityReach;
        HitResult hitResult = pickNew(pos, entityReach, viewVec, player);

        if (hitResult.getType() != HitResult.Type.MISS) {
            distance = hitResult.getLocation().distanceToSqr(pos);
            double blockReach = 5;
            if (distance > blockReach * blockReach) {
                hitResult = BlockHitResult.miss(hitResult.getLocation(), Direction.getNearest(pos.x, pos.y, pos.z), BlockPos.containing(hitResult.getLocation()));
            }
        }

        Vec3 toVec = pos.add(viewVec.x * entityReach, viewVec.y * entityReach, viewVec.z * entityReach);
        AABB aabb = player.getBoundingBox().expandTowards(viewVec.scale(entityReach)).inflate(1);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(player, pos, toVec, aabb, p -> !p.isSpectator()
                && p.isAlive()
                && !(p instanceof Projectile)
                && SeekTool.BASIC_FILTER.test(p)
                && !p.getType().is(ModTags.EntityTypes.DECOY)
                && SeekTool.NOT_IN_SMOKE.test(p)
                && p != player
                && p != player.getVehicle(), distance);
        if (entityhitresult != null) {
            Vec3 targetPos = entityhitresult.getLocation();
            double distanceToTarget = pos.distanceToSqr(targetPos);
            if (distanceToTarget > distance || distanceToTarget > entityReach * entityReach) {
                hitResult = BlockHitResult.miss(targetPos, Direction.getNearest(viewVec.x, viewVec.y, viewVec.z), BlockPos.containing(targetPos));
            } else if (distanceToTarget < distance) {
                hitResult = entityhitresult;
            }
        }
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            return ((EntityHitResult) hitResult).getEntity();
        }
        return null;
    }

    public static Entity findLookDecoy(Player player, Vec3 pos, Vec3 viewVec, double entityReach) {
        double distance = entityReach * entityReach;
        HitResult hitResult = pickNew(pos, entityReach, viewVec, player);

        if (hitResult.getType() != HitResult.Type.MISS) {
            distance = hitResult.getLocation().distanceToSqr(pos);
            double blockReach = 5;
            if (distance > blockReach * blockReach) {
                hitResult = BlockHitResult.miss(hitResult.getLocation(), Direction.getNearest(pos.x, pos.y, pos.z), BlockPos.containing(hitResult.getLocation()));
            }
        }

        Vec3 toVec = pos.add(viewVec.x * entityReach, viewVec.y * entityReach, viewVec.z * entityReach);
        AABB aabb = player.getBoundingBox().expandTowards(viewVec.scale(entityReach)).inflate(2);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(player, pos, toVec, aabb, p -> p.getType().is(ModTags.EntityTypes.DECOY), distance);
        if (entityhitresult != null) {
            Vec3 targetPos = entityhitresult.getLocation();
            double distanceToTarget = pos.distanceToSqr(targetPos);
            if (distanceToTarget > distance || distanceToTarget > entityReach * entityReach) {
                hitResult = BlockHitResult.miss(targetPos, Direction.getNearest(viewVec.x, viewVec.y, viewVec.z), BlockPos.containing(targetPos));
            } else if (distanceToTarget < distance) {
                hitResult = entityhitresult;
            }
        }
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            return ((EntityHitResult) hitResult).getEntity();
        }
        return null;
    }

    public static HitResult pickNew(Vec3 pos, double pHitDistance, VehicleEntity vehicle) {
        Vec3 vec31 = vehicle.getBarrelVector(1);
        Vec3 vec32 = pos.add(vec31.x * pHitDistance, vec31.y * pHitDistance, vec31.z * pHitDistance);
        return vehicle.level().clip(new ClipContext(pos, vec32, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, vehicle));
    }

    public static HitResult pickNew(Vec3 pos, double pHitDistance, Vec3 viewVec, Entity entity) {
        Vec3 vec32 = pos.add(viewVec.x * pHitDistance, viewVec.y * pHitDistance, viewVec.z * pHitDistance);
        return entity.level().clip(new ClipContext(pos, vec32, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, entity));
    }

    public static List<BlockPos> getBlocksAlongRay(Vec3 start, Vec3 direction, double maxDistance) {
        List<BlockPos> blocks = new ArrayList<>();

        // 标准化方向向量
        Vec3 normalizedDir = direction.normalize();

        // DDA算法参数
        double step = 0.1; // 步长（越小精度越高）
        double distance = 0;
        BlockPos lastPos = null;

        while (distance <= maxDistance) {
            Vec3 currentPos = start.add(normalizedDir.scale(distance));
            BlockPos blockPos = new BlockPos(
                    (int) Math.floor(currentPos.x),
                    (int) Math.floor(currentPos.y),
                    (int) Math.floor(currentPos.z)
            );

            // 避免重复添加同一方块
            if (lastPos == null || !lastPos.equals(blockPos)) {
                blocks.add(blockPos);
                lastPos = blockPos;
            }

            distance += step;
        }

        return blocks;
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
    public static List<RayTraceResultEntity> getEntitiesAlongVector(Level world, Vec3 start, Vec3 direction, Predicate<Entity> filterPredicate) {
        List<RayTraceResultEntity> hitEntities = new ArrayList<>();
        double maxDistance = direction.length();

        // 1. 标准化方向向量并计算终点
        Vec3 normalizedDirection = direction.normalize();
        Vec3 end = start.add(normalizedDirection.scale(maxDistance));

        // 2. 创建一个从起点到终点的AABB进行粗筛，减少需要精确检测的实体数量
        AABB rayBoundingBox = new AABB(start, end).inflate(1); // 适当扩大边界框

        // 3. 获取在这个粗筛AABB内的所有实体。
        List<Entity> entitiesInWorld = world.getEntities((Entity) null, rayBoundingBox, filterPredicate);

        // 4. 遍历这些实体，进行精确的射线与碰撞箱相交测试
        for (Entity entity : entitiesInWorld) {
            // 忽略实体部件（如末影龙的各个部分，它们通常由父实体处理）
            if (entity instanceof PartEntity) {
                continue;
            }

            // 获取实体当前tick的碰撞箱
            AABB entityBoundingBox = entity.getBoundingBox();
            // 可选：稍微扩大碰撞箱，避免因精度问题错过
            entityBoundingBox = entityBoundingBox.inflate(0.3);

            // 进行射线与实体碰撞箱的相交测试
            Double distanceToHit = rayIntersectsAABB(start, normalizedDirection, entityBoundingBox, maxDistance);

            if (distanceToHit != null) {
                Vec3 hitVec = start.add(normalizedDirection.scale(distanceToHit)); // 计算实际交点坐标
                hitEntities.add(new RayTraceResultEntity(entity, distanceToHit, hitVec));
            }
        }

        // 5. 根据距离排序，返回从近到远的列表
        hitEntities.sort(Comparator.comparingDouble(o -> o.distance));
        return hitEntities;
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
    private static Double rayIntersectsAABB(Vec3 start, Vec3 dir, AABB box, double maxDist) {
        double tMin = 0;
        double tMax = maxDist;

        // 分别检查X轴
        double invDx = 1 / dir.x;
        double t0x = (box.minX - start.x) * invDx;
        double t1x = (box.maxX - start.x) * invDx;

        if (invDx < 0) {
            double temp = t0x;
            t0x = t1x;
            t1x = temp;
        }

        tMin = Math.max(tMin, t0x);
        tMax = Math.min(tMax, t1x);

        if (tMax <= tMin) {
            return null;
        }

        // 检查Y轴
        double invDy = 1 / dir.y;
        double t0y = (box.minY - start.y) * invDy;
        double t1y = (box.maxY - start.y) * invDy;

        if (invDy < 0) {
            double temp = t0y;
            t0y = t1y;
            t1y = temp;
        }

        tMin = Math.max(tMin, t0y);
        tMax = Math.min(tMax, t1y);

        if (tMax <= tMin) {
            return null;
        }

        // 检查Z轴
        double invDz = 1 / dir.z;
        double t0z = (box.minZ - start.z) * invDz;
        double t1z = (box.maxZ - start.z) * invDz;

        if (invDz < 0) {
            double temp = t0z;
            t0z = t1z;
            t1z = temp;
        }

        tMin = Math.max(tMin, t0z);
        tMax = Math.min(tMax, t1z);

        if (tMax <= tMin) {
            return null;
        }

        // 返回最近的交点距离参数t
        return tMin;
    }

    /**
     * 用于存储射线检测结果的数据结构
     */
    public static class RayTraceResultEntity {
        public final Entity entity;
        public final double distance; // 从起点到交点的距离
        public final Vec3 hitVec;     // 射线与实体碰撞箱的交点

        public RayTraceResultEntity(Entity entity, double distance, Vec3 hitVec) {
            this.entity = entity;
            this.distance = distance;
            this.hitVec = hitVec;
        }

        @Override
        public String toString() {
            return "RayTraceResultEntity{" +
                    "entity=" + entity +
                    ", distance=" + distance +
                    ", hitVec=" + hitVec +
                    '}';
        }
    }
}
