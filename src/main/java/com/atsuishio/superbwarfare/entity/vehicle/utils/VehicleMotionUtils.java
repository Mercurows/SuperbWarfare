package com.atsuishio.superbwarfare.entity.vehicle.utils;

import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.List;

/**
 * 处理载具运动相关方法的工具类
 */
public final class VehicleMotionUtils {
    /**
     * 防止载具堆叠
     *
     * @param vehicle 载具
     */
    public static void preventStacking(VehicleEntity vehicle) {
        var entities = vehicle.level().getEntities(
                EntityTypeTest.forClass(VehicleEntity.class),
                vehicle.getBoundingBox().inflate(6),
                entity -> entity != vehicle && entity != vehicle.getFirstPassenger() && entity.getVehicle() == null
        );

        for (var entity : entities) {
            if (entity.getBoundingBox().intersects(vehicle.getBoundingBox())) {
                Vec3 toVec = vehicle.position().add(new Vec3(1, 1, 1).scale(vehicle.getRandom().nextFloat() * 0.01f + 1f)).vectorTo(entity.position());
                Vec3 velAdd = toVec.normalize().scale(Math.max((vehicle.getBbWidth() + 2) - vehicle.position().distanceTo(entity.position()), 0) * 0.1);
                double entitySize = entity.getBbWidth() * entity.getBbHeight();
                double thisSize = vehicle.getBbWidth() * vehicle.getBbHeight();
                double f = Math.min(entitySize / thisSize, 2);
                double f1 = Math.min(thisSize / entitySize, 2);

                vehicle.pushNew(-f * velAdd.x, -f * velAdd.y, -f * velAdd.z);
                entity.push(f1 * velAdd.x, f1 * velAdd.y, f1 * velAdd.z);
            }
        }
    }

    /**
     * 支撑自身范围内的实体
     *
     * @param vehicle 载具
     */
    public static void supportEntities(VehicleEntity vehicle) {
        if (vehicle.isRemoved()) return;
        if (!(vehicle instanceof OBBEntity obbEntity) || obbEntity.getOBBs().isEmpty()) {
            return;
        }

        var frontBox = calculateCombinedAABBOptimized(vehicle).inflate(1);
        List<Entity> entities = vehicle.level().getEntities(EntityTypeTest.forClass(Entity.class), frontBox,
                        entity -> entity != vehicle && entity != vehicle.getFirstPassenger() && entity.getVehicle() == null)
                .stream().filter(entity -> {
                            if (entity.isAlive() && obbEntity.isInObb(entity, vehicle.getDeltaMovement())) {
                                var type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                                return (entity instanceof VehicleEntity || entity instanceof Boat || entity instanceof Minecart || (entity instanceof LivingEntity living && !(living instanceof Player player && player.isSpectator()))) || VehicleConfig.COLLISION_ENTITY_WHITELIST.get().contains(type.toString());
                            }
                            return false;
                        }
                )
                .toList();

        entities.forEach(e -> {
            if (e instanceof Player player && vehicle.level().isClientSide) {
                vehicle.support(player);
            } else if (!vehicle.level().isClientSide) {
                vehicle.support(e);
            }
        });
    }

    /**
     * 支撑某一个实体
     *
     * @param vehicle 载具
     * @author YWZJ Ranpoes
     */
    public static void support(VehicleEntity vehicle, Entity entity) {
        if (!(vehicle instanceof OBBEntity obbEntity)) return;
        if (entity.noPhysics || vehicle.noPhysics) {
            return;
        }

        Vec3 feetPos = entity.position().subtract(new Vec3(0, 0.1f, 0));
        Vec3 midPos = feetPos.add(0, entity.getEyeHeight() / 2, 0);
        Vec3 eyePos = feetPos.add(0, entity.getEyeHeight(), 0);

        for (var obb : obbEntity.getOBBs()) {
            if (obb.contains(feetPos)) {
                if (!entity.noPhysics && !vehicle.noPhysics) {
                    double gravity = Math.max(entity.getDeltaMovement().y, 0);
                    if (gravity == 0) {
                        entity.setOnGround(true);
                    }
                    double depth = obb.getEmbeddingDepth(feetPos);
                    entity.setDeltaMovement(vehicle.getDeltaMovement().add(0, gravity + depth <= 0.2f ? 0 : depth * 1.1, 0));
                    entity.fallDistance = 0;

                    continue;
                }
            }
            if (obb.contains(eyePos)) {
                double dx = entity.getX() - obb.center().x;
                double dz = entity.getZ() - obb.center().z;
                double dMax = Mth.absMax(dx, dz);
                if (dMax >= (double) 0.01F) {
                    dMax = Math.sqrt(dMax);
                    dx /= dMax;
                    dz /= dMax;
                    double d = 1.0D / dMax;
                    if (d > 1.0D) {
                        d = 1.0D;
                    }
                    dx *= d;
                    dz *= d;
                    dx *= 0.05F;
                    dz *= 0.05F;
                    if (entity.isPushable()) {
                        entity.push(dx, 0.0D, dz);
                    }
                    continue;
                }
            }

            var aabb = entity.getBoundingBox();
            if (OBB.isColliding(obb, aabb)) {
                int face = obb.getEmbeddingFace(midPos);
                var axes = obb.getAxes();
                var support = axes[Math.abs(face) - 1];
                if (face < 0) {
                    support.negate();
                }
                if (entity.isPushable()) {
                    float force = 0.1f;
                    if (vehicle.getDeltaMovement().length() > 0.01 && Math.abs(face) != 2) {
                        force = 0.2f;
                    }
                    var vec = new Vec3(support).scale(force);
                    vec = new Vec3(vec.x, Math.max(0, vec.y), vec.z);
                    if (entity instanceof Player player && player.onGround() && player.isCrouching()) {
                        // 推车
                        vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(vec.scale(-1).normalize().scale(player.getDeltaMovement().horizontalDistance() * 3)));
                        player.setDeltaMovement(player.getDeltaMovement().add(vec.scale(1).normalize().scale(player.getDeltaMovement().horizontalDistance() * 0.5)));
                    } else {
                        entity.setPos(entity.position().add(vec));
                        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.2, 0.2, 0.2));
                    }

                    vehicle.hasImpulse = true;
                }
            }
        }
    }

    /**
     * 撞击实体并造成伤害
     *
     * @param vehicle 载具
     */
    public static void crushEntities(VehicleEntity vehicle) {
        if (!vehicle.canCrushEntities()) return;
        if (vehicle.isRemoved()) return;

        var vec3 = vehicle.getDeltaMovement();

        List<Entity> entities;
        if (vehicle instanceof OBBEntity obbEntity) {
            var frontBox = vehicle.getBoundingBox().move(vec3).inflate(6);
            entities = vehicle.level().getEntities(EntityTypeTest.forClass(Entity.class), frontBox,
                            entity -> entity != vehicle && entity != vehicle.getFirstPassenger() && entity.getVehicle() == null)
                    .stream().filter(entity -> {
                                if (entity.isAlive() && obbEntity.isInObb(entity, vec3)) {
                                    var type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                                    return (entity instanceof VehicleEntity || entity instanceof Boat || entity instanceof Minecart || (entity instanceof LivingEntity living && !(living instanceof Player player && player.isSpectator()))) || VehicleConfig.COLLISION_ENTITY_WHITELIST.get().contains(type.toString());
                                }
                                return false;
                            }
                    )
                    .toList();
        } else {
            var frontBox = vehicle.getBoundingBox().move(vec3);
            entities = vehicle.level().getEntities(EntityTypeTest.forClass(Entity.class), frontBox,
                            entity -> entity != vehicle && entity != vehicle.getFirstPassenger() && entity.getVehicle() == null)
                    .stream().filter(entity -> {
                                if (entity.isAlive()) {
                                    var type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                                    return (entity instanceof VehicleEntity || entity instanceof Boat || entity instanceof Minecart
                                            || (entity instanceof LivingEntity living && !(living instanceof Player player && player.isSpectator())))
                                            || VehicleConfig.COLLISION_ENTITY_WHITELIST.get().contains(type.toString());
                                }
                                return false;
                            }
                    )
                    .toList();
        }

        // TODO 继续优化这个逆天碰撞
        for (var entity : entities) {
            double entitySize = entity.getBoundingBox().getSize();
            double thisSize = vehicle.getBoundingBox().getSize();
            double f;
            double f1;

            Vec3 v0 = vec3.subtract(entity.getDeltaMovement());
            if (VectorTool.calculateAngle(v0, vehicle.position().vectorTo(entity.position())) > 90) return;

            if (vehicle.getDeltaMovement().lengthSqr() < 0.09) return;

            // TODO 给非载具实体也设置质量
            if (entity instanceof LivingEntity living && living.hasEffect(ModMobEffects.STRIKE_PROTECTION)) {
                continue;
            }

            if (entity instanceof VehicleEntity v) {
                f = Mth.clamp(v.getMass() / vehicle.getMass(), 0.25, 4);
                f1 = Mth.clamp(vehicle.getMass() / vehicle.getMass(), 0.25, 4);
            } else {
                f = Mth.clamp(entitySize / thisSize, 0.25, 4);
                f1 = Mth.clamp(thisSize / entitySize, 0.25, 4);
            }

            float length = (float) v0.length();
            var velAdd = v0.normalize().scale(0.8 * length);

            if (length <= 0.3) {
                continue;
            }

            vehicle.level().playSound(null, vehicle, ModSounds.VEHICLE_STRIKE.get(), vehicle.getSoundSource(), 1, 1);

            if (entity instanceof LivingEntity) {
                DamageHandler.doDamage(entity, ModDamageTypes.causeVehicleStrikeDamage(vehicle.level().registryAccess(), vehicle, vehicle.getFirstPassenger() == null ? vehicle : vehicle.getFirstPassenger()), (float) (f1 * 80 * (Mth.abs(length) - 0.3) * (Mth.abs(length) - 0.3)));
            } else {
                entity.hurt(ModDamageTypes.causeVehicleStrikeDamage(vehicle.level().registryAccess(), vehicle, vehicle.getFirstPassenger() == null ? vehicle : vehicle.getFirstPassenger()), (float) (f1 * 60 * (Mth.abs(length) - 0.3) * (Mth.abs(length) - 0.3)));
            }

            if (!(entity instanceof TargetEntity)) {
                vehicle.pushNew(-0.3f * f * velAdd.x, -0.3f * f * velAdd.y, -0.3f * f * velAdd.z);
            }

            if (entity instanceof VehicleEntity mobileVehicle) {
                vehicle.hurt(ModDamageTypes.causeVehicleStrikeDamage(vehicle.level().registryAccess(), entity, entity.getFirstPassenger() == null ? entity : entity.getFirstPassenger()), (float) (f * 40 * (Mth.abs(length) - 0.3) * (Mth.abs(length) - 0.3)));

                if (vehicle instanceof OBBEntity obbEntity) {
                    if (obbEntity.isInObb(entity, Vec3.ZERO)) {
                        Vec3 thisPos = vehicle.position();
                        Vec3 otherPos = entity.position();

                        for (OBB obb : obbEntity.getOBBs()) {
                            if (entity instanceof OBBEntity obbEntity2) {
                                var obbList2 = obbEntity2.getOBBs();
                                for (var obb2 : obbList2) {
                                    if (OBB.isColliding(obb, obb2)) {
                                        thisPos = new Vec3(obb.center());
                                        otherPos = new Vec3(obb2.center());
                                    }
                                }
                            } else {
                                if (OBB.isColliding(obb, entity.getBoundingBox())) {
                                    thisPos = new Vec3(obb.center());
                                }
                            }
                        }

                        Vec3 toVec = thisPos.add(new Vec3(1, 1, 1).scale(vehicle.getRandom().nextFloat() * 0.01f + 1f)).vectorTo(otherPos);
                        velAdd = toVec.normalize().scale(Math.max(thisPos.distanceTo(otherPos), 0) * 0.01);
                        vehicle.pushNew(-f * velAdd.x, -f * velAdd.y, -f * velAdd.z);
                    }
                }

                Vec3 vec31 = vehicle.getDeltaMovement().normalize().scale(velAdd.length());
                mobileVehicle.pushNew(f1 * vec31.x, f1 * vec31.y, f1 * vec31.z);
            } else {
                Vec3 vec31 = vehicle.getDeltaMovement().normalize().scale(velAdd.length());
                entity.push(f1 * vec31.x, f1 * vec31.y, f1 * vec31.z);
            }
        }
    }

    // TODO 实现正确的AABB包围箱
    public static AABB calculateCombinedAABBOptimized(VehicleEntity vehicle) {
        if (!(vehicle instanceof OBBEntity obbEntity) || obbEntity.getOBBs().isEmpty()) {
            return vehicle.getBoundingBox();
        }

        var obbList = obbEntity.getOBBs();

        Vector3f min = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        Vector3f max = new Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);

        for (OBB obb : obbList) {
            Vector3f[] vertices = obb.getVertices();

            for (Vector3f vertex : vertices) {
                min.x = Math.min(min.x, vertex.x);
                min.y = Math.min(min.y, vertex.y);
                min.z = Math.min(min.z, vertex.z);

                max.x = Math.max(max.x, vertex.x);
                max.y = Math.max(max.y, vertex.y);
                max.z = Math.max(max.z, vertex.z);
            }
        }

        return new AABB(new Vec3(min), new Vec3(max));
    }

    /**
     * 根据条件来碰撞方块
     *
     * @param vehicle 载具
     */
    public static void collideBlocks(VehicleEntity vehicle) {
        boolean soft = VehicleConfig.COLLISION_DESTROY_SOFT_BLOCKS.get();
        boolean normal = VehicleConfig.COLLISION_DESTROY_NORMAL_BLOCKS.get();
        boolean hard = VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.get() && vehicle.canCollideHardBlock();
        boolean beast = VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.get() && vehicle.canCollideBlockBeastly();

        if (vehicle instanceof OBBEntity obbEntity) {
            AABB aabb = vehicle.getBoundingBox().move(vehicle.getDeltaMovement()).inflate(5);
            BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
                BlockState blockstate = vehicle.level().getBlockState(pos);
                if (obbEntity.isInObb(pos, vehicle.getDeltaMovement())) {
                    if ((soft && blockstate.is(ModTags.Blocks.SOFT_COLLISION)) ||
                            (normal && blockstate.is(ModTags.Blocks.NORMAL_COLLISION)) ||
                            (hard && blockstate.is(ModTags.Blocks.HARD_COLLISION)) ||
                            (beast && (blockstate.getBlock().defaultDestroyTime() > 0 || blockstate.getBlock().defaultDestroyTime() <= 4))) {
                        vehicle.level().destroyBlock(pos, true);
                    }
                }
            });
        }

        AABB aabb = vehicle.getBoundingBox().inflate(0.25, 0, 0.25).move(vehicle.getDeltaMovement()).move(0, 0.5, 0);
        BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
            BlockState blockstate = vehicle.level().getBlockState(pos);
            if ((soft && blockstate.is(ModTags.Blocks.SOFT_COLLISION)) ||
                    (normal && blockstate.is(ModTags.Blocks.NORMAL_COLLISION)) ||
                    (hard && blockstate.is(ModTags.Blocks.HARD_COLLISION)) ||
                    (beast && (blockstate.getBlock().defaultDestroyTime() > 0 || blockstate.getBlock().defaultDestroyTime() <= 4))) {
                vehicle.level().destroyBlock(pos, true);
            }
        });
    }

    /**
     * 载具在龙牙上行驶时，减速
     *
     * @param vehicle 载具
     */
    public static void handleVehicleMoveOnDragonTeeth(VehicleEntity vehicle) {
        AABB aabb = vehicle.getBoundingBox();
        AABB aabb1 = new AABB(aabb.minX, aabb.minY - 1.0E-6D, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ);
        var pos = vehicle.level().findSupportingBlock(vehicle, aabb1).orElse(null);
        if (pos == null) return;

        BlockState state = vehicle.level().getBlockState(pos);
        if (state.is(ModBlocks.DRAGON_TEETH.get())) {
            vehicle.getEntityData().set(VehicleEntity.POWER, vehicle.getEntityData().get(VehicleEntity.POWER) * 0.8f);
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(-0.1, 0, -0.1));
        }
    }
}
