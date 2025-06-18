package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

import java.util.ArrayList;
import java.util.List;

import static com.atsuishio.superbwarfare.tools.SeekTool.smokeFilter;

public class TraceTool {

    public static boolean laserHeadshot = false;

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
        Vec3 viewVec = entity.getViewVector(1.0F);
        Vec3 toVec = eyePos.add(viewVec.x * entityReach, viewVec.y * entityReach, viewVec.z * entityReach);
        AABB aabb = entity.getBoundingBox().expandTowards(viewVec.scale(entityReach)).inflate(1.0D, 1.0D, 1.0D);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(entity, eyePos, toVec, aabb, p -> !p.isSpectator() && entity.getVehicle() != p && p.isAlive() && smokeFilter(p), distance);
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

        Vec3 viewVec = entity.getViewVector(1.0F);
        Vec3 toVec = eyePos.add(viewVec.x * entityReach, viewVec.y * entityReach, viewVec.z * entityReach);
        AABB aabb = entity.getBoundingBox().expandTowards(viewVec.scale(entityReach)).inflate(1.0D, 1.0D, 1.0D);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(entity, eyePos, toVec, aabb, p -> !p.isSpectator() && entity.getVehicle() != p && p.isAlive(), distance);
        if (entityhitresult != null) {
            hitResult = entityhitresult;

        }
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            return ((EntityHitResult) hitResult).getEntity();
        }
        return null;
    }

    public static Entity laserfindLookingEntity(Entity player, double entityReach) {

        double distance = entityReach * entityReach;
        Vec3 eyePos = player.getEyePosition(1.0f);
        HitResult hitResult = player.pick(entityReach, 1.0f, false);

        Vec3 viewVec = player.getViewVector(1.0F);
        Vec3 toVec = eyePos.add(viewVec.x * entityReach, viewVec.y * entityReach, viewVec.z * entityReach);
        AABB aabb = player.getBoundingBox().expandTowards(viewVec.scale(entityReach)).inflate(1.0D, 1.0D, 1.0D);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(player, eyePos, toVec, aabb, p -> !p.isSpectator() && player.getVehicle() != p && p.isAlive() && smokeFilter(p), distance);
        if (entityhitresult != null) {
            Vec3 hitVec = entityhitresult.getLocation();
            if (checkNoClip(player, hitVec)) {
                hitResult = entityhitresult;
            }

            if (hitResult.getType() == HitResult.Type.ENTITY && ((EntityHitResult) hitResult).getEntity().isAlive()) {
                if (((EntityHitResult) hitResult).getEntity() instanceof LivingEntity living) {
                    laserHeadshot = living.getEyeY() - 0.4 < hitVec.y && hitVec.y < living.getEyeY() + 0.5;
                } else {
                    laserHeadshot = false;
                }
                return ((EntityHitResult) hitResult).getEntity();
            }
        }

        return null;
    }

    public static boolean checkNoClip(Entity entity, Vec3 target) {
        return entity.level().clip(new ClipContext(entity.getEyePosition(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() != HitResult.Type.BLOCK;
    }

    public static Entity vehiclefFindLookingEntity(VehicleEntity vehicle, Vec3 eye, double entityReach) {
        double distance = entityReach * entityReach;
        HitResult hitResult = pickNew(eye, 512, vehicle);

        Vec3 viewVec = vehicle.getBarrelVector(1);
        Vec3 toVec = eye.add(viewVec.x * entityReach, viewVec.y * entityReach, viewVec.z * entityReach);
        AABB aabb = vehicle.getBoundingBox().expandTowards(viewVec.scale(entityReach)).inflate(1.0D, 1.0D, 1.0D);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(vehicle, eye, toVec, aabb,
                p -> !p.isSpectator() && p.isAlive() && SeekTool.baseFilter(p) && !p.getType().is(ModTags.EntityTypes.DECOY) && smokeFilter(p), distance);
        if (entityhitresult != null) {
            hitResult = entityhitresult;

        }
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            return ((EntityHitResult) hitResult).getEntity();
        }
        return null;
    }

    public static Entity droneFindLookingEntity(Entity entity, Vec3 pos, double entityReach, float ticks) {
        double distance = entityReach * entityReach;
        HitResult hitResult = entity.pick(entityReach, 1.0f, false);

        Vec3 viewVec = entity.getViewVector(ticks);
        Vec3 toVec = pos.add(viewVec.x * entityReach, viewVec.y * entityReach, viewVec.z * entityReach);
        AABB aabb = entity.getBoundingBox().expandTowards(viewVec.scale(entityReach)).inflate(1.0D, 1.0D, 1.0D);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(entity, pos, toVec, aabb, p -> !p.isSpectator()
                && p.isAlive()
                && !(p instanceof Projectile)
                && SeekTool.baseFilter(p)
                && !p.getType().is(ModTags.EntityTypes.DECOY)
                && smokeFilter(p)
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

    public static Entity camerafFindLookingEntity(Player player, Vec3 pos, double entityReach, float ticks) {
        double distance = entityReach * entityReach;
        HitResult hitResult = player.pick(entityReach, 1.0f, false);

        Vec3 viewVec = player.getViewVector(ticks);
        Vec3 toVec = pos.add(viewVec.x * entityReach, viewVec.y * entityReach, viewVec.z * entityReach);
        AABB aabb = player.getBoundingBox().expandTowards(viewVec.scale(entityReach)).inflate(1.0D, 1.0D, 1.0D);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(player, pos, toVec, aabb, p -> !p.isSpectator()
                && p.isAlive()
                && !(p instanceof Projectile)
                && SeekTool.baseFilter(p)
                && !p.getType().is(ModTags.EntityTypes.DECOY)
                && smokeFilter(p)
                && p != player
                && p != player.getVehicle(), distance);
        if (entityhitresult != null) {
            hitResult = entityhitresult;

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

    public static List<BlockPos> getBlocksAlongRay(Vec3 start, Vec3 direction, double maxDistance) {
        List<BlockPos> blocks = new ArrayList<>();

        // 标准化方向向量
        Vec3 normalizedDir = direction.normalize();
        Vec3 end = start.add(normalizedDir.scale(maxDistance));

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
}
