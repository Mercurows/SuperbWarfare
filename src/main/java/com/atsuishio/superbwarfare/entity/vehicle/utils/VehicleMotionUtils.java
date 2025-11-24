package com.atsuishio.superbwarfare.entity.vehicle.utils;

import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.VectorTool;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Optional;

import static com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEngineUtils.lerpAngle;
import static com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.transformPosition;

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
        if (vehicle.enableAABB() || vehicle instanceof Type63Entity) {
            return;
        }

        var frontBox = calculateCombinedAABBOptimized(vehicle).inflate(1);
        List<Entity> entities = vehicle.level().getEntities(EntityTypeTest.forClass(Entity.class), frontBox,
                        entity -> entity != vehicle && entity != vehicle.getFirstPassenger() && entity.getVehicle() == null)
                .stream().filter(entity -> {
                    if (entity.isAlive() && vehicle.isInObb(entity, vehicle.getDeltaMovement())) {
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
        if (vehicle.enableAABB()) return;
        if (entity.noPhysics || vehicle.noPhysics) {
            return;
        }

        Vec3 feetPos = entity.position().subtract(new Vec3(0, 0.1f, 0));
        Vec3 midPos = feetPos.add(0, entity.getEyeHeight() / 2, 0);
        Vec3 eyePos = feetPos.add(0, entity.getEyeHeight(), 0);

        for (var obb : vehicle.getOBBs()) {
            if (entity instanceof Player player && player.onGround() && player.isCrouching() && player.level() instanceof ServerLevel) {
                // 推车
                vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(player.getForward()).normalize().scale(player.getDeltaMovement().length() * 3));
            }

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
                    double d = 1 / dMax;
                    if (d > 1) {
                        d = 1;
                    }
                    dx *= d;
                    dz *= d;
                    dx *= 0.05F;
                    dz *= 0.05F;
                    if (entity.isPushable()) {
                        entity.push(dx, 0, dz);
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
                    entity.setPos(entity.position().add(vec));
                    entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.2, 0.2, 0.2));
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
        if (!vehicle.enableAABB()) {
            var frontBox = vehicle.getBoundingBox().move(vec3).inflate(6);
            entities = vehicle.level().getEntities(EntityTypeTest.forClass(Entity.class), frontBox,
                            entity -> entity != vehicle && entity != vehicle.getFirstPassenger() && entity.getVehicle() == null)
                    .stream().filter(entity -> {
                        if (entity.isAlive() && vehicle.isInObb(entity, vec3)) {
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

                if (!vehicle.enableAABB()) {
                    if (vehicle.isInObb(entity, Vec3.ZERO)) {
                        Vec3 thisPos = vehicle.position();
                        Vec3 otherPos = entity.position();

                        for (OBB obb : vehicle.getOBBs()) {
                            if (!mobileVehicle.enableAABB()) {
                                var obbList2 = mobileVehicle.getOBBs();
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
        if (vehicle.enableAABB()) {
            return vehicle.getBoundingBox();
        }

        var obbList = vehicle.getOBBs();

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
        var collisionLevel = vehicle.computed().collisionLevel;
        var limits = collisionLevel.powerLimits;

        float power = vehicle.getEntityData().get(VehicleEntity.POWER);
        var motion = vehicle.getDeltaMovement().horizontalDistance();

        boolean[] flags = new boolean[]{
                VehicleConfig.COLLISION_DESTROY_SOFT_BLOCKS.get() && collisionLevel.level >= 1,
                VehicleConfig.COLLISION_DESTROY_NORMAL_BLOCKS.get() && collisionLevel.level >= 2,
                VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.get() && collisionLevel.level >= 3,
                VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.get() && collisionLevel.level >= 4
        };

        for (int i = 0; i < flags.length && i < limits.size(); i++) {
            var limit = limits.get(i);
            flags[i] &= limit.equals() ? power >= limit.power() || motion >= limit.motion() : power > limit.power() || motion > limit.motion();
        }

        if (!vehicle.enableAABB()) {
            AABB aabb = vehicle.getBoundingBox().move(vehicle.getDeltaMovement()).inflate(5);
            BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
                BlockState blockstate = vehicle.level().getBlockState(pos);
                if (vehicle.isInObb(pos, vehicle.getDeltaMovement())) {
                    if ((flags[0] && blockstate.is(ModTags.Blocks.SOFT_COLLISION)) ||
                            (flags[1] && blockstate.is(ModTags.Blocks.NORMAL_COLLISION)) ||
                            (flags[2] && blockstate.is(ModTags.Blocks.HARD_COLLISION)) ||
                            (flags[3] && (blockstate.getBlock().defaultDestroyTime() > 0 || blockstate.getBlock().defaultDestroyTime() <= 4))) {
                        vehicle.level().destroyBlock(pos, true);
                    }
                }
            });
        }

        AABB aabb = vehicle.getBoundingBox().inflate(0.25, 0, 0.25).move(vehicle.getDeltaMovement()).move(0, 0.5, 0);
        BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
            BlockState blockstate = vehicle.level().getBlockState(pos);
            if ((flags[0] && blockstate.is(ModTags.Blocks.SOFT_COLLISION)) ||
                    (flags[1] && blockstate.is(ModTags.Blocks.NORMAL_COLLISION)) ||
                    (flags[2] && blockstate.is(ModTags.Blocks.HARD_COLLISION)) ||
                    (flags[3] && (blockstate.getBlock().defaultDestroyTime() > 0 || blockstate.getBlock().defaultDestroyTime() <= 4))) {
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

    public static void bounceHorizontal(VehicleEntity vehicle, Direction direction) {
        switch (direction.getAxis()) {
            case X:
                vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(0.8, 0.99, 0.99));
                break;
            case Z:
                vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(0.99, 0.99, 0.8));
                break;
        }
    }

    public static void bounceVertical(VehicleEntity vehicle, Direction direction) {
        if (!vehicle.level().isClientSide) {
            vehicle.level().playSound(null, vehicle, ModSounds.VEHICLE_STRIKE.get(), vehicle.getSoundSource(), 1, 1);
        }
        vehicle.collisionCoolDown = 4;
        vehicle.setCrash(true);
        if (direction.getAxis() == Direction.Axis.Y) {
            vehicle.setDeltaMovement(vehicle.getDeltaMovement().multiply(0.9, -0.8, 0.9));
        }
    }

    public static void terrainCompact(VehicleEntity vehicle, List<Vec3> positions) {
        if (vehicle.onGround()) {
            Matrix4f transform = vehicle.getWheelsTransform(1);
            for (Vec3 vec3 : positions) {
                Vector4f vector4f = transformPosition(transform, (float) vec3.x, (float) vec3.y - 0.02f, (float) vec3.z);
                Vec3 p = new Vec3(vector4f.x, vector4f.y, vector4f.z);
                var level = vehicle.level();
                var res = level.clip(new ClipContext(p, p.add(0, -512, 0),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle));

                double heightY;

                BlockPos blockPos = BlockPos.containing(p);
                BlockPos blockPosUp = BlockPos.containing(p.add(0, 1, 0));
                if (level.getBlockState(blockPosUp).canOcclude()) {
                    blockPos = blockPosUp;
                }
                BlockState state = level.getBlockState(blockPos);
                VoxelShape shape = state.getCollisionShape(level, blockPos);

                if (!shape.isEmpty()) {
                    heightY = p.y - (shape.max(Direction.Axis.Y) + blockPos.getY());
                } else if (res.getType() == HitResult.Type.BLOCK && level.noCollision(new AABB(p, p))) {
                    heightY = Mth.clamp(p.y - res.getLocation().y, 0, 20);
                } else {
                    heightY = 0;
                }

                updateTerrainCompact(vehicle, p, heightY);
            }
        } else if (vehicle.isInFluidType()) {
            vehicle.setXRot(vehicle.getXRot() * 0.9f);
            vehicle.setZRot(vehicle.getRoll() * 0.9f);
        }
    }

    public static void updateTerrainCompact(VehicleEntity entity, Vec3 landingTarget, double heightY) {
        Vec3 currentPos = entity.position();
        AABB aabb = entity.getBoundingBox();
        AABB aabb1 = new AABB(aabb.minX, aabb.minY - 1.0E-6D, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ);
        Optional<BlockPos> optional = entity.level().findSupportingBlock(entity, aabb1);
        if (optional.isPresent()) {
            currentPos = currentPos.add(currentPos.vectorTo(optional.get().getCenter()).scale(0.4));
        }
        Vec3 horizontalOffset = new Vec3(
                landingTarget.x - currentPos.x,
                0,
                landingTarget.z - currentPos.z
        );

        double horizontalDistance = horizontalOffset.length();
        Vec3 horizontalDirection = horizontalDistance > 0 ?
                horizontalOffset.normalize() : Vec3.ZERO;


        float tiltSmoothingFactor = 0.03f;

        float targetTilt = (float) Math.min(heightY * 7 * entity.data().compute().terrainCompatRotateRate * horizontalDistance, 45);

        float yawRad = Math.toRadians(-entity.getYRot());
        Vec3 localDirection = new Vec3(
                horizontalDirection.x * Math.cos(yawRad) - horizontalDirection.z * Math.sin(yawRad),
                0,
                horizontalDirection.x * Math.sin(yawRad) + horizontalDirection.z * Math.cos(yawRad)
        );

        float targetXRot = (float) (-localDirection.z * targetTilt);
        float targetZRot = (float) (localDirection.x * targetTilt);

        entity.setXRot(lerpAngle(entity.getXRot(), -targetXRot, tiltSmoothingFactor));
        entity.setZRot(lerpAngle(entity.getRoll(), -targetZRot, tiltSmoothingFactor));
    }

    public static Matrix4f getWheelsTransform(VehicleEntity vehicle, float partialTicks) {
        Matrix4f transform = new Matrix4f();
        transform.translate(
                (float) Mth.lerp(partialTicks, vehicle.xo, vehicle.getX()),
                (float) Mth.lerp(partialTicks, vehicle.yo, vehicle.getY()),
                (float) Mth.lerp(partialTicks, vehicle.zo, vehicle.getZ())
        );
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, vehicle.yRotO, vehicle.getYRot())));
        return transform;
    }
}
