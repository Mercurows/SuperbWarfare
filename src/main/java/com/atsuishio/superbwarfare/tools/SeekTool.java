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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.LAST_DRIVER_UUID;

public class SeekTool {

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
        var tag = NBTTool.getTag(stack);
        if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) {
            drone2 = EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone"));
        }

        return e instanceof DroneEntity drone
                && drone != drone2
                && drone.getController() != null
                && teamFilter(e, drone.getController());
    }

    @Deprecated(forRemoval = true)
    public static Entity seekEntity(Entity entity, Level level, double seekRange, double seekAngle) {
//        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
//                .filter(e -> {
//                    if (e.distanceTo(entity) <= seekRange && calculateAngle(e, entity) < seekAngle
//                            && e != entity
//                            && baseFilter(e)
//                            && smokeFilter(e)
//                            && e.getVehicle() == null
//                    ) {
//                        return level.clip(new ClipContext(entity.getEyePosition(), e.getEyePosition(),
//                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() != HitResult.Type.BLOCK;
//                    }
//                    return false;
//                }).min(Comparator.comparingDouble(e -> calculateAngle(e, entity))).orElse(null);
        return seekEntity(entity, seekRange, seekAngle);
    }

    public static Entity seekEntity(Entity entity, double range, double angle) {
        return new Builder(entity)
                .withinRange(range)
                .withinAngle(angle)
                .baseFilter()
                .smokeFilter()
                .noVehicle()
                .clip()
                .buildWithClosest();
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
                    if (e.distanceTo(entity) <= seekRange
                            && calculateAngle(e, entity) < seekAngle
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
                })
                .min(Comparator.comparingDouble(e -> calculateAngle(e, entity)))
                .orElse(null);
    }

    public static List<Entity> seekLivingEntities(Entity entity, Level level, double seekRange, double seekAngle) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> {
                    if (e.distanceTo(entity) <= seekRange
                            && calculateAngle(e, entity) < seekAngle
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
                    if (e.distanceTo(vehicle) <= seekRange
                            && calculateAngleVehicle(e, vehicle) < seekAngle
                            && e != vehicle
                            && baseFilter(e)
                            && smokeFilter(e)
                            && e.getVehicle() == null
                            && !friendlyToPlayer(vehicle, e)) {
                        return level.clip(new ClipContext(vehicle.getNewEyePos(1), vehicle.getNewEyePos(1),
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle)).getType() != HitResult.Type.BLOCK;
                    }
                    return false;
                })
                .min(Comparator.comparingDouble(e -> calculateAngleVehicle(e, vehicle)))
                .orElse(null);
    }

    public static List<Entity> seekLivingEntitiesThroughWall(Entity entity, Level level, double seekRange, double seekAngle) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> e.distanceTo(entity) <= seekRange
                        && calculateAngle(e, entity) < seekAngle
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
        var box = pEntity.getBoundingBox().inflate(8);

        var entities = pEntity.level().getEntities(EntityTypeTest.forClass(Entity.class), box,
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
        var type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return SeekConfig.SEEK_BLACKLIST.get().contains(type.toString());
    }

    public static class Builder {

        @NotNull
        private final Entity entity;
        private final List<Function<Entity, Boolean>> filters = new ArrayList<>();

        public Builder(@NotNull Entity entity) {
            this(entity, true);
        }

        public Builder(@NotNull Entity entity, boolean excludeSelf) {
            this.entity = entity;
            if (excludeSelf) {
                this.filters.add(e -> e != this.entity);
            }
        }

        public List<Entity> build() {
            return StreamSupport.stream(EntityFindUtil.getEntities(entity.level()).getAll().spliterator(), false)
                    .filter(e -> this.filters.stream().map(f -> f.apply(e)).reduce(true, (a, b) -> a && b))
                    .toList();
        }

        @Nullable
        public Entity buildWithClosest() {
            return StreamSupport.stream(EntityFindUtil.getEntities(entity.level()).getAll().spliterator(), false)
                    .filter(e -> this.filters.stream().map(f -> f.apply(e)).reduce(true, (a, b) -> a && b))
                    .min(Comparator.comparingDouble(e -> calculateAngle(e, entity)))
                    .orElse(null);
        }

        public Builder withinRange(double range) {
            this.filters.add(e -> e.position().distanceTo(this.entity.getEyePosition()) <= range);
            return this;
        }

        public Builder overRange(double range) {
            this.filters.add(e -> e.position().distanceTo(this.entity.getEyePosition()) >= range);
            return this;
        }

        public Builder sameTeam() {
            this.filters.add(e -> teamFilter(entity, e));
            return this;
        }

        public Builder differentTeam() {
            this.filters.add(e -> !teamFilter(entity, e));
            return this;
        }

        public Builder friendly() {
            this.filters.add(e -> friendlyToPlayer(entity, e));
            return this;
        }

        public Builder blackList() {
            this.filters.add(e -> {
                var type = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
                return SeekConfig.SEEK_BLACKLIST.get().contains(type.toString());
            });
            return this;
        }

        public Builder smokeFilter() {
            this.filters.add(SeekTool::smokeFilter);
            return this;
        }

        public Builder onGround(double height) {
            this.filters.add(e -> SeekTool.isOnGround(e, height));
            return this;
        }

        public Builder baseFilter() {
            this.filters.add(SeekTool::baseFilter);
            return this;
        }

        public Builder withinAngle(double angle) {
            this.filters.add(e -> SeekTool.calculateAngle(entity, e) < angle);
            return this;
        }

        public Builder is(Class<? extends Entity> clazz) {
            this.filters.add(clazz::isInstance);
            return this;
        }

        public Builder isNot(Class<? extends Entity> clazz) {
            this.filters.add(e -> !clazz.isInstance(e));
            return this;
        }

        public Builder is(TagKey<EntityType<?>> tagKey) {
            this.filters.add(e -> e.getType().is(tagKey));
            return this;
        }

        public Builder isNot(TagKey<EntityType<?>> tagKey) {
            this.filters.add(e -> !e.getType().is(tagKey));
            return this;
        }

        public Builder clip() {
            this.filters.add(e ->
                    this.entity.level()
                            .clip(new ClipContext(entity.getEyePosition(), e.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity))
                            .getType() != HitResult.Type.BLOCK
            );
            return this;
        }

        public Builder hasVehicle() {
            this.filters.add(e -> e.getVehicle() != null);
            return this;
        }

        public Builder noVehicle() {
            this.filters.add(e -> e.getVehicle() == null);
            return this;
        }

        public Builder sizeLesserThan(double size) {
            this.filters.add(e -> e.getBoundingBox().getSize() <= size);
            return this;
        }

        public Builder sizeGreaterThan(double size) {
            this.filters.add(e -> e.getBoundingBox().getSize() >= size);
            return this;
        }
    }
}
