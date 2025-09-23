package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.config.server.SeekConfig;
import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity;
import com.atsuishio.superbwarfare.entity.projectile.SwarmDroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.world.TDMSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.LAST_DRIVER_UUID;

public class SeekTool {

    public static List<Entity> getVehicleWithinRange(Player player, Level level, double range) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> e.position().distanceTo(player.getEyePosition()) <= range
                        && e instanceof VehicleEntity)
                .toList();
    }

    public static List<Entity> getEntityWithinRange(Player player, Level level, double range) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> e.position().distanceTo(player.getEyePosition()) <= range)
                .toList();
    }

    public static List<Entity> getEntityWithinRange(Entity entity, Level level, double range) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> e.position().distanceTo(entity.getEyePosition()) <= range)
                .toList();
    }

    public static List<Entity> getTeammate(Player player, Level level) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> friendlyToPlayer(player, e))
                .toList();
    }

    public static boolean friendlyToPlayer(Entity e, Entity entity) {
        if (teamFilter(e, entity)) return true;
        if (entity instanceof OwnableEntity ownableEntity && ownableEntity.getOwner() != null && teamFilter(e, ownableEntity.getOwner()))
            return true;
        if (e instanceof Player player && teammateDrone(entity, player)) return true;

        List<Entity> entities = entity.getPassengers();
        for (var passenger : entities) {
            if (teamFilter(e, passenger)) {
                return true;
            }
        }

        if (entity instanceof VehicleEntity vehicle) {
            Entity lastDriver = EntityFindUtil.findEntity(vehicle.level(), vehicle.getEntityData().get(LAST_DRIVER_UUID));
            return lastDriver != null && teamFilter(e, lastDriver);
        }

        return false;
    }

    public static boolean teamFilter(Entity e, Entity entity) {
        if (e == null) return false;
        if (entity == null) return false;
        return e == entity || (entity.getTeam() != null && !TDMSavedData.enabledTDM(entity) && entity.getTeam() == e.getTeam());
    }

    public static boolean teammateDrone(Entity e, Player player) {
        ItemStack stack = player.getMainHandItem();
        DroneEntity drone2 = null;
        if (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked")) {
            drone2 = EntityFindUtil.findDrone(player.level(), stack.getOrCreateTag().getString("LinkedDrone"));
        }

        return e instanceof DroneEntity drone
                && drone != drone2
                && drone.getController() != null
                && teamFilter(e, drone.getController());
    }

    public static Entity seekEntity(Entity entity, Level level, double seekRange, double seekAngle) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> {
                    if (e.distanceTo(entity) <= seekRange && calculateAngle(e, entity) < seekAngle
                            && e != entity
                            && baseFilter(e)
                            && smokeFilter(e)
                            && e.getVehicle() == null
                    ) {
                        return level.clip(new ClipContext(entity.getEyePosition(), e.getEyePosition(),
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() != HitResult.Type.BLOCK;
                    }
                    return false;
                }).min(Comparator.comparingDouble(e -> calculateAngle(e, entity))).orElse(null);
    }

    public static Entity seekCustomSizeEntity(Entity entity, Level level, double seekRange, double seekAngle, double size, boolean checkOnGround) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> {
                    if (e.distanceTo(entity) <= seekRange && calculateAngle(e, entity) < seekAngle
                            && e != entity
                            && baseFilter(e)
                            && (!checkOnGround || isOnGround(e, 10))
                            && e.getBoundingBox().getSize() >= size
                            && smokeFilter(e)
                            && e.getVehicle() == null
                    ) {
                        return level.clip(new ClipContext(entity.getEyePosition(), e.getEyePosition(),
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() != HitResult.Type.BLOCK;
                    }
                    return false;
                }).min(Comparator.comparingDouble(e -> calculateAngle(e, entity))).orElse(null);
    }

    public static Entity seekLivingEntity(Entity entity, Level level, double seekRange, double seekAngle) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> {
                    if (e.distanceTo(entity) <= seekRange && calculateAngle(e, entity) < seekAngle
                            && e != entity
                            && baseFilter(e)
                            && smokeFilter(e)
                            && e.getVehicle() == null
                            && !(e instanceof SwarmDroneEntity swarmDrone && swarmDrone.getOwner() != entity)
                            && !friendlyToPlayer(entity, e)) {
                        return level.clip(new ClipContext(entity.getEyePosition(), e.getEyePosition(),
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() != HitResult.Type.BLOCK;
                    }
                    return false;
                }).min(Comparator.comparingDouble(e -> calculateAngle(e, entity))).orElse(null);
    }

    public static List<Entity> seekLivingEntities(Entity entity, Level level, double seekRange, double seekAngle) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> {
                    if (e.distanceTo(entity) <= seekRange && calculateAngle(e, entity) < seekAngle
                            && e != entity
                            && baseFilter(e)
                            && smokeFilter(e)
                            && e.getVehicle() == null
                            && !friendlyToPlayer(entity, e)) {
                        return level.clip(new ClipContext(entity.getEyePosition(), e.getEyePosition(),
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() != HitResult.Type.BLOCK;
                    }
                    return false;
                }).toList();
    }

    public static List<Entity> seekCustomSizeEntities(Entity entity, Level level, double seekRange, double seekAngle, double size, boolean checkOnGround) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> {
                    if (e.distanceTo(entity) <= seekRange && calculateAngle(e, entity) < seekAngle
                            && e != entity
                            && e.getBoundingBox().getSize() >= size
                            && baseFilter(e)
                            && (!checkOnGround || isOnGround(e, 10))
                            && smokeFilter(e)
                            && e.getVehicle() == null
                            && !friendlyToPlayer(entity, e)) {
                        return level.clip(new ClipContext(entity.getEyePosition(), e.getEyePosition(),
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() != HitResult.Type.BLOCK;
                    }
                    return false;
                }).toList();
    }

    public static Entity vehicleSeekEntity(VehicleEntity vehicle, Level level, double seekRange, double seekAngle) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> {
                    if (e.distanceTo(vehicle) <= seekRange && calculateAngleVehicle(e, vehicle) < seekAngle
                            && e != vehicle
                            && baseFilter(e)
                            && smokeFilter(e)
                            && e.getVehicle() == null
                            && !friendlyToPlayer(vehicle, e)) {
                        return level.clip(new ClipContext(vehicle.getNewEyePos(1), vehicle.getNewEyePos(1),
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle)).getType() != HitResult.Type.BLOCK;
                    }
                    return false;
                }).min(Comparator.comparingDouble(e -> calculateAngleVehicle(e, vehicle))).orElse(null);
    }

    public static List<Entity> seekLivingEntitiesThroughWall(Entity entity, Level level, double seekRange, double seekAngle) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> e.distanceTo(entity) <= seekRange && calculateAngle(e, entity) < seekAngle
                        && e != entity
                        && baseFilter(e)
                        && e.getVehicle() == null
                        && !friendlyToPlayer(entity, e)).toList();
    }

    public static Entity seekEntityThroughWall(Entity entity, Level level, double seekRange, double seekAngle) {
        return seekLivingEntitiesThroughWall(entity, level, seekRange, seekAngle)
                .stream().min(Comparator.comparingDouble(e -> calculateAngle(e, entity))).orElse(null);
    }

    public static List<Entity> getEntitiesWithinRange(BlockPos pos, Level level, double range) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> e.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= range * range
                        && baseFilter(e) && smokeFilter(e) && !e.getType().is(ModTags.EntityTypes.DECOY))
                .toList();
    }

    private static double calculateAngle(Entity entityA, Entity entityB) {
        Vec3 start = new Vec3(entityA.getX() - entityB.getX(), entityA.getY() - entityB.getY(), entityA.getZ() - entityB.getZ());
        Vec3 end = entityB.getLookAngle();
        return VectorTool.calculateAngle(start, end);
    }

    private static double calculateAngleVehicle(Entity entityA, VehicleEntity entityB) {
        Vec3 entityBEyePos = entityB.getNewEyePos(1);
        Vec3 start = new Vec3(entityA.getX() - entityBEyePos.x, entityA.getY() - entityBEyePos.y, entityA.getZ() - entityBEyePos.z);
        Vec3 end = entityB.getBarrelVector(1);
        return VectorTool.calculateAngle(start, end);
    }

    public static boolean baseFilter(Entity entity) {
        return entity.isAlive()
                && !(entity instanceof HangingEntity || (entity instanceof Projectile && !entity.getType().is(ModTags.EntityTypes.DESTROYABLE_PROJECTILE)))
                && !(entity instanceof Player player && player.isSpectator())
                && !isInBlackList(entity);
    }

    public static boolean isOnGround(Entity entity) {
        return isOnGround(entity, 0);
    }

    /**
     * 判断实体是否位于离地面n米的范围内
     */
    public static boolean isOnGround(Entity entity, double height) {
        Level level = entity.level();

        double y = entity.getY();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        // 如果实体已低于世界底部或高于顶部
        if (y < minY || y > maxY) {
            return false;
        }

        boolean[] onGround = {false};
        AABB aabb = entity.getBoundingBox().expandTowards(0, -height, 0);
        BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
            if (pos.getY() < minY || pos.getY() > maxY) return;

            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                onGround[0] = true;
            }
        });
        return entity.onGround() || entity.isInWater() || onGround[0];
    }

    public static boolean smokeFilter(Entity pEntity) {
        var Box = pEntity.getBoundingBox().inflate(8);

        var entities = pEntity.level().getEntities(EntityTypeTest.forClass(Entity.class), Box,
                        entity -> entity instanceof SmokeDecoyEntity)
                .stream().toList();

        boolean result = true;

        for (var e : entities) {
            if (e != null) {
                result = false;
                break;
            }
        }

        return result;
    }

    public static boolean isInBlackList(Entity entity) {
        var type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (type == null) return false;
        return SeekConfig.SEEK_BLACKLIST.get().contains(type.toString());
    }
}
