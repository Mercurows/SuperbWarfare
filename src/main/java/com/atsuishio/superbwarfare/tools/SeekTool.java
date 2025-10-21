package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.config.server.SeekConfig;
import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity;
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
import net.minecraft.world.entity.TraceableEntity;
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
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.LAST_DRIVER_UUID;

// TODO 0.8.9把下面的废弃方法都删了
public class SeekTool {

    @Deprecated(forRemoval = true)
    public static boolean baseFilter(Entity entity) {
        return BASIC_FILTER.test(entity);
    }

    @Deprecated(forRemoval = true)
    public static boolean smokeFilter(Entity pEntity) {
        return NOT_IN_SMOKE.test(pEntity);
    }

    @Deprecated(forRemoval = true)
    public static boolean friendlyToPlayer(Entity e, Entity entity) {
        return IS_FRIENDLY.test(e, entity);
    }

    @Deprecated(forRemoval = true)
    public static boolean teamFilter(Entity e, Entity entity) {
        return IN_SAME_TEAM.test(e, entity);
    }

    @Deprecated(forRemoval = true)
    public static Entity seekEntity(Entity entity, Level level, double seekRange, double seekAngle) {
        return seekEntity(entity, seekRange, seekAngle);
    }

    public static Entity seekEntity(Entity entity, double range, double angle) {
        return new Builder(entity)
                .withinRange(range)
                .withinAngle(angle)
                .baseFilter()
                .smokeFilter()
                .noVehicle()
                .noClip()
                .buildWithClosest();
    }

    @Deprecated(forRemoval = true)
    public static Entity seekCustomSizeEntity(Entity entity, Level level, double seekRange, double seekAngle, double size, boolean checkOnGround) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> {
                    if (e.distanceTo(entity) <= seekRange && calculateAngle(e, entity) < seekAngle
                            && e != entity
                            && baseFilter(e)
                            && (!checkOnGround || ON_GROUND_HEIGHT.test(e, 10d))
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

    @Deprecated(forRemoval = true)
    public static Entity seekLivingEntity(Entity entity, Level level, double seekRange, double seekAngle) {
        return seekLivingEntity(entity, seekRange, seekAngle);
    }

    @Nullable
    public static Entity seekLivingEntity(@NotNull Entity entity, double range, double angle) {
        return new Builder(entity)
                .withinRange(range)
                .withinAngle(angle)
                .baseFilter()
                .smokeFilter()
                .noVehicle()
                .notFriendly()
                .isNotOwner()
                .noClip()
                .buildWithClosest();
    }

    @Deprecated(forRemoval = true)
    public static List<Entity> seekLivingEntities(Entity entity, Level level, double seekRange, double seekAngle) {
        return seekLivingEntities(entity, seekRange, seekAngle);
    }

    public static List<Entity> seekLivingEntities(Entity entity, double seekRange, double seekAngle) {
        return new Builder(entity)
                .withinRange(seekRange)
                .withinAngle(seekAngle)
                .baseFilter()
                .smokeFilter()
                .noVehicle()
                .notFriendly()
                .noClip()
                .build();
    }

    @Deprecated(forRemoval = true)
    public static List<Entity> seekCustomSizeEntities(Entity entity, Level level, double seekRange, double seekAngle, double size, boolean checkOnGround) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> {
                    if (e.distanceTo(entity) <= seekRange && calculateAngle(e, entity) < seekAngle
                            && e != entity
                            && e.getBoundingBox().getSize() >= size
                            && baseFilter(e)
                            && (!checkOnGround || ON_GROUND_HEIGHT.test(e, 10d))
                            && smokeFilter(e)
                            && e.getVehicle() == null
                            && !friendlyToPlayer(entity, e)) {
                        return level.clip(new ClipContext(entity.getEyePosition(), e.getEyePosition(),
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() != HitResult.Type.BLOCK;
                    }
                    return false;
                }).toList();
    }

    @Deprecated(forRemoval = true)
    public static List<Entity> seekLivingEntitiesThroughWall(Entity entity, Level level, double seekRange, double seekAngle) {
        return seekLivingEntitiesThroughWall(entity, seekRange, seekAngle);
    }

    public static List<Entity> seekLivingEntitiesThroughWall(Entity entity, double range, double angle) {
        return new Builder(entity)
                .withinRange(range)
                .withinAngle(angle)
                .baseFilter()
                .noVehicle()
                .notFriendly()
                .build();
    }

    @Deprecated(forRemoval = true)
    public static Entity seekEntityThroughWall(Entity entity, Level level, double seekRange, double seekAngle) {
        return seekEntityThroughWall(entity, seekRange, seekAngle);
    }

    @Nullable
    public static Entity seekEntityThroughWall(Entity entity, double range, double angle) {
        return new Builder(entity)
                .withinRange(range)
                .withinAngle(angle)
                .baseFilter()
                .noVehicle()
                .notFriendly()
                .buildWithClosest();
    }

    public static List<Entity> getEntitiesWithinRange(BlockPos pos, Level level, double range) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> e.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= range * range
                        && BASIC_FILTER.test(e)
                        && NOT_IN_SMOKE.test(e)
                        && !e.getType().is(ModTags.EntityTypes.DECOY))
                .toList();
    }

    private static double calculateAngle(Entity entityA, Entity entityB) {
        Vec3 start = new Vec3(entityA.getX() - entityB.getX(), entityA.getY() - entityB.getY(), entityA.getZ() - entityB.getZ());
        Vec3 end = entityB.getLookAngle();
        return VectorTool.calculateAngle(start, end);
    }

    /**
     * 判断实体是否存活
     */
    public static final Predicate<Entity> IS_ALIVE = Entity::isAlive;

    /**
     * 判断实体不是旁观者
     */
    public static final Predicate<Entity> NOT_SPECTATOR = e -> !(e instanceof Player player && player.isSpectator());

    /**
     * 判定实体是否位于黑名单中
     */
    public static final Predicate<Entity> IN_BLACKLIST = e -> {
        var type = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return SeekConfig.SEEK_BLACKLIST.get().contains(type.toString());
    };

    /**
     * 判断实体的类型是否属于被排除的默认类型
     */
    public static final Predicate<Entity> BASIC_TYPE_FILTER =
            e -> !(e instanceof HangingEntity || (e instanceof Projectile && !e.getType().is(ModTags.EntityTypes.DESTROYABLE_PROJECTILE)));

    /**
     * 基础实体过滤判断
     */
    public static final Predicate<Entity> BASIC_FILTER = e ->
            IS_ALIVE.test(e) && NOT_SPECTATOR.test(e) && BASIC_TYPE_FILTER.test(e) && !IN_BLACKLIST.test(e);

    /**
     * 判断实体是否位于离地面n米的范围内
     */
    public static final BiPredicate<Entity, Double> ON_GROUND_HEIGHT =
            (entity, height) -> {
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
            };

    /**
     * 判定实体是否在地面上
     */
    public static final Predicate<Entity> ON_GROUND = e -> ON_GROUND_HEIGHT.test(e, 0D);

    /**
     * 判断两个实体是否在同一队伍
     */
    public static final BiPredicate<Entity, Entity> IN_SAME_TEAM = (self, target) -> {
        if (self == null || target == null) return false;
        return self == target || (target.getTeam() != null && !TDMSavedData.enabledTDM(target) && target.getTeam() == self.getTeam());
    };

    /**
     * 判断实体是否和无人机是友方
     */
    public static final BiPredicate<Entity, Entity> IS_FRIENDLY_DRONE = (self, target) -> {
        if (!(self instanceof Player player)) return false;

        ItemStack stack = player.getMainHandItem();
        DroneEntity myDrone = null;
        var tag = NBTTool.getTag(stack);
        if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) {
            myDrone = EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone"));
        }

        return target instanceof DroneEntity drone
                && drone != myDrone
                && drone.getController() != null
                && IN_SAME_TEAM.test(target, drone.getController());
    };

    /**
     * 判断两个实体是否是友方关系
     */
    public static final BiPredicate<Entity, Entity> IS_FRIENDLY = (self, target) -> {
        if (IN_SAME_TEAM.test(self, target)) return true;
        if (target instanceof OwnableEntity ownableEntity && ownableEntity.getOwner() != null && IN_SAME_TEAM.test(self, ownableEntity.getOwner())) {
            return true;
        }
        if (IS_FRIENDLY_DRONE.test(self, target)) return true;

        List<Entity> entities = target.getPassengers();
        for (var passenger : entities) {
            if (IN_SAME_TEAM.test(self, passenger)) {
                return true;
            }
        }

        if (target instanceof VehicleEntity vehicle) {
            Entity lastDriver = EntityFindUtil.findEntity(vehicle.level(), vehicle.getEntityData().get(LAST_DRIVER_UUID));
            return lastDriver != null && IN_SAME_TEAM.test(self, lastDriver);
        }
        return false;
    };

    /**
     * 判断实体周围是否没有烟雾
     */
    public static final Predicate<Entity> NOT_IN_SMOKE = e -> {
        var box = e.getBoundingBox().inflate(8);
        var entities = e.level().getEntities(EntityTypeTest.forClass(Entity.class), box, entity -> entity instanceof SmokeDecoyEntity).stream().toList();
        return entities.isEmpty();
    };

    /**
     * 判断某实体是否是自己的
     */
    public static final BiPredicate<Entity, Entity> IS_OWNER = (self, target) -> {
        if (target instanceof TraceableEntity traceableEntity) {
            return traceableEntity.getOwner() == self;
        } else if (target instanceof OwnableEntity ownableEntity) {
            return ownableEntity.getOwner() == self;
        } else {
            return false;
        }
    };

    /**
     * 判断某实体是否不是自己的
     */
    public static final BiPredicate<Entity, Entity> IS_NOT_OWNER = (self, target) -> {
        if (target instanceof TraceableEntity traceableEntity) {
            return traceableEntity.getOwner() != self;
        } else if (target instanceof OwnableEntity ownableEntity) {
            return ownableEntity.getOwner() != self;
        } else {
            return true;
        }
    };

    public static class Builder {

        @NotNull
        private final Entity entity;
        private final List<Predicate<Entity>> filters = new ArrayList<>();

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
                    .filter(e -> {
                        for (var f : this.filters) {
                            if (!f.test(e)) return false;
                        }
                        return true;
                    })
                    .toList();
        }

        @Nullable
        public Entity buildWithClosest() {
            return StreamSupport.stream(EntityFindUtil.getEntities(entity.level()).getAll().spliterator(), false)
                    .filter(e -> {
                        for (var f : this.filters) {
                            if (!f.test(e)) return false;
                        }
                        return true;
                    })
                    .min(Comparator.comparingDouble(e -> calculateAngle(e, entity)))
                    .orElse(null);
        }

        public Builder notItsVehicle() {
            this.filters.add(e -> e.getVehicle() != this.entity);
            return this;
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
            this.filters.add(e -> IN_SAME_TEAM.test(entity, e));
            return this;
        }

        public Builder differentTeam() {
            this.filters.add(e -> !IN_SAME_TEAM.test(entity, e));
            return this;
        }

        public Builder friendly() {
            this.filters.add(e -> IS_FRIENDLY.test(entity, e));
            return this;
        }

        public Builder notFriendly() {
            this.filters.add(e -> !IS_FRIENDLY.test(entity, e));
            return this;
        }

        public Builder blackList() {
            this.filters.add(IN_BLACKLIST);
            return this;
        }

        public Builder smokeFilter() {
            this.filters.add(NOT_IN_SMOKE);
            return this;
        }

        public Builder onGround(double height) {
            this.filters.add(e -> ON_GROUND_HEIGHT.test(e, height));
            return this;
        }

        public Builder baseFilter() {
            this.filters.add(BASIC_FILTER);
            return this;
        }

        public Builder withinAngle(double angle) {
            this.filters.add(e -> SeekTool.calculateAngle(e, entity) < angle);
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

        public Builder isOwner() {
            this.filters.add(e -> IS_OWNER.test(entity, e));
            return this;
        }

        public Builder isNotOwner() {
            this.filters.add(e -> IS_NOT_OWNER.test(entity, e));
            return this;
        }

        public Builder noClip() {
            this.filters.add(e ->
                    this.entity.level()
                            .clip(new ClipContext(entity.getEyePosition(), e.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity))
                            .getType() != HitResult.Type.BLOCK
            );
            return this;
        }

        public Builder vehicleNoClip(Entity entity) {
            this.filters.add(e -> {
                        if (this.entity instanceof VehicleEntity vehicle) {
                            return this.entity.level()
                                    .clip(new ClipContext(vehicle.driverZoomPos(entity, 1), vehicle.driverZoomPos(entity, 1), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle))
                                    .getType() != HitResult.Type.BLOCK;
                        }
                        return false;
                    }
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

        public Builder sizeBiggerThan(double size) {
            this.filters.add(e -> e.getBoundingBox().getSize() >= size);
            return this;
        }

        public Builder sizeGreaterThan(double size) {
            this.filters.add(e -> e.getBoundingBox().getSize() >= size);
            return this;
        }

        public Builder custom(Predicate<Entity> predicate) {
            this.filters.add(predicate);
            return this;
        }

        public Builder custom(BiPredicate<Entity, Entity> predicate) {
            this.filters.add(e -> predicate.test(entity, e));
            return this;
        }
    }
}
