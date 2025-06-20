package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.projectile.FlareDecoyEntity;
import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.OBB;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

public abstract class MobileVehicleEntity extends EnergyVehicleEntity implements ControllableVehicle {
    public static Consumer<MobileVehicleEntity> trackSound = vehicle -> {
    };
    public static Consumer<MobileVehicleEntity> engineSound = vehicle -> {
    };
    public static Consumer<MobileVehicleEntity> swimSound = vehicle -> {
    };

    public static final EntityDataAccessor<Integer> CANNON_RECOIL_TIME = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Integer> FIRE_ANIM = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> COAX_HEAT = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Integer> AMMO = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DECOY_COUNT = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> GEAR_ROT = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> GEAR_UP = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> PLANE_BREAK = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.FLOAT);

    private Vec3 previousVelocity = Vec3.ZERO;

    public double acceleration;
    public int decoyReloadCoolDown;
    public static boolean IGNORE_ENTITY_GROUND_CHECK_STEPPING = false;
    public boolean leftInputDown;
    public boolean rightInputDown;
    public boolean forwardInputDown;
    public boolean backInputDown;
    public boolean upInputDown;
    public boolean downInputDown;
    public boolean decoyInputDown;
    public boolean fireInputDown;
    public boolean sprintInputDown;
    public double lastTickSpeed;
    public double lastTickVerticalSpeed;
    public int collisionCoolDown;

    private boolean wasEngineRunning = false;

    public float rudderRot;
    public float rudderRotO;

    public float leftWheelRot;
    public float rightWheelRot;
    public float leftWheelRotO;
    public float rightWheelRotO;

    public float leftTrackO;
    public float rightTrackO;
    public float leftTrack;
    public float rightTrack;

    public float rotorRot;
    public float rotorRotO;

    public float propellerRot;
    public float propellerRotO;

    public double recoilShake;
    public double recoilShakeO;

    public boolean cannotFireCoax;
    public int reloadCoolDown;

    public double velocityO;
    public double velocity;

    public float flap1LRot;
    public float flap1LRotO;
    public float flap1RRot;
    public float flap1RRotO;
    public float flap1L2Rot;
    public float flap1L2RotO;
    public float flap1R2Rot;
    public float flap1R2RotO;
    public float flap2LRot;
    public float flap2LRotO;
    public float flap2RRot;
    public float flap2RRotO;
    public float flap3Rot;
    public float flap3RotO;
    public float gearRotO;

    public MobileVehicleEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void processInput(short keys) {
        leftInputDown
                = (keys & 0b000000001) > 0;
        rightInputDown
                = (keys & 0b000000010) > 0;
        forwardInputDown
                = (keys & 0b000000100) > 0;
        backInputDown
                = (keys & 0b000001000) > 0;
        upInputDown
                = (keys & 0b000010000) > 0;
        downInputDown
                = (keys & 0b000100000) > 0;
        decoyInputDown
                = (keys & 0b001000000) > 0;
        fireInputDown
                = (keys & 0b010000000) > 0;
        sprintInputDown
                = (keys & 0b100000000) > 0;
    }

    @Override
    public void playerTouch(Player pPlayer) {
        if (pPlayer.isCrouching() && !this.level().isClientSide) {
            double entitySize = pPlayer.getBbWidth() * pPlayer.getBbHeight();
            double thisSize = this.getBbWidth() * this.getBbHeight();
            double f = Math.min(entitySize / thisSize, 2);
            double f1 = Math.min(thisSize / entitySize, 4);
            this.setDeltaMovement(this.getDeltaMovement().add(new Vec3(pPlayer.position().vectorTo(this.position()).toVector3f()).scale(0.15 * f * pPlayer.getDeltaMovement().length())));
            pPlayer.setDeltaMovement(pPlayer.getDeltaMovement().add(new Vec3(this.position().vectorTo(pPlayer.position()).toVector3f()).scale(0.1 * f1 * pPlayer.getDeltaMovement().length())));
        }
    }

    @Override
    public void baseTick() {
        if (!this.wasEngineRunning && this.engineRunning() && this.level().isClientSide()) {
            engineSound.accept(this);
            swimSound.accept(this);
            if (this.hasTracks()) {
                trackSound.accept(this);
            }
        }
        this.wasEngineRunning = this.engineRunning();

        turretYRotO = this.getTurretYRot();
        turretXRotO = this.getTurretXRot();

        gunYRotO = this.getGunYRot();
        gunXRotO = this.getGunXRot();

        leftWheelRotO = this.getLeftWheelRot();
        rightWheelRotO = this.getRightWheelRot();

        leftTrackO = this.getLeftTrack();
        rightTrackO = this.getRightTrack();

        rotorRotO = this.getRotorRot();

        rudderRotO = this.getRudderRot();

        propellerRotO = this.getPropellerRot();

        recoilShakeO = this.getRecoilShake();

        velocityO = this.getVelocity();

        lastTickSpeed = new Vec3(this.getDeltaMovement().x, this.getDeltaMovement().y + 0.06, this.getDeltaMovement().z).length();
        lastTickVerticalSpeed = this.getDeltaMovement().y + 0.06;
        if (collisionCoolDown > 0) {
            collisionCoolDown--;
        }

        flap1LRotO = this.getFlap1LRot();
        flap1RRotO = this.getFlap1RRot();
        flap1L2RotO = this.getFlap1L2Rot();
        flap1R2RotO = this.getFlap1R2Rot();
        flap2LRotO = this.getFlap2LRot();
        flap2RRotO = this.getFlap2RRot();
        flap3RotO = this.getFlap3Rot();
        gearRotO = entityData.get(GEAR_ROT);

        super.baseTick();

        // 获取当前速度（deltaMovement 是当前速度向量）
        Vec3 currentVelocity = this.getDeltaMovement();

        // 计算加速度向量（时间间隔 Δt = 0.05秒）
        Vec3 accelerationVec = currentVelocity.subtract(previousVelocity).scale(20); // scale(1/0.05) = scale(20)

        // 计算加速度的绝对值
        acceleration = accelerationVec.length() * 20;

        // 更新前一时刻的速度
        previousVelocity = currentVelocity;

        double direct = (90 - calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90;
        setVelocity(Mth.lerp(0.4, getVelocity(), getDeltaMovement().horizontalDistance() * direct * 20));

        float deltaT = java.lang.Math.abs(getTurretYRot() - turretYRotO);
        while (getTurretYRot() > 180F) {
            setTurretYRot(getTurretYRot() - 360F);
            turretYRotO = getTurretYRot() - deltaT;
        }
        while (getTurretYRot() <= -180F) {
            setTurretYRot(getTurretYRot() + 360F);
            turretYRotO = deltaT + getTurretYRot();
        }

        if (this.entityData.get(COAX_HEAT) > 0) {
            this.entityData.set(COAX_HEAT, this.entityData.get(COAX_HEAT) - 1);
        }

        if (this.entityData.get(FIRE_ANIM) > 0) {
            this.entityData.set(FIRE_ANIM, this.entityData.get(FIRE_ANIM) - 1);
        }

        if (this.entityData.get(COAX_HEAT) < 40) {
            cannotFireCoax = false;
        }

        if (decoyReloadCoolDown > 0) {
            decoyReloadCoolDown--;
        }

        if (this.entityData.get(COAX_HEAT) > 100) {
            cannotFireCoax = true;
            this.level().playSound(null, this.getOnPos(), ModSounds.MINIGUN_OVERHEAT.get(), SoundSource.PLAYERS, 1, 1);
        }

        if (this.entityData.get(CANNON_RECOIL_TIME) > 0) {
            this.entityData.set(CANNON_RECOIL_TIME, this.entityData.get(CANNON_RECOIL_TIME) - 1);
        }

        this.setRecoilShake(java.lang.Math.pow(entityData.get(CANNON_RECOIL_TIME), 4) * 0.0000007 * java.lang.Math.sin(0.2 * java.lang.Math.PI * (entityData.get(CANNON_RECOIL_TIME) - 2.5)));

        preventStacking();
        crushEntities(this.getDeltaMovement());

        this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.06, 0.0));

        this.move(MoverType.SELF, this.getDeltaMovement());
        baseCollideBlock();

        this.refreshDimensions();
    }

    //烟雾诱饵
    public void releaseSmokeDecoy(Vec3 vec3) {
        if (decoyInputDown) {
            if (this.entityData.get(DECOY_COUNT) > 0 && this.level() instanceof ServerLevel) {
                for (int i = 0; i < 8; i++) {
                    SmokeDecoyEntity smokeDecoyEntity = new SmokeDecoyEntity(this.level());
                    smokeDecoyEntity.setPos(this.getX(), this.getY() + getBbHeight(), this.getZ());
                    smokeDecoyEntity.decoyShoot(this, vec3.yRot((-78.75f + 22.5F * i) * Mth.DEG_TO_RAD), 4f, 8);
                    this.level().addFreshEntity(smokeDecoyEntity);
                }
                this.level().playSound(null, this, ModSounds.DECOY_FIRE.get(), this.getSoundSource(), 1, 1);
                decoyReloadCoolDown = 500;
                this.getEntityData().set(DECOY_COUNT, this.getEntityData().get(DECOY_COUNT) - 1);
            }
            decoyInputDown = false;
        }
        if (this.entityData.get(DECOY_COUNT) < 1 && decoyReloadCoolDown == 0 && this.level() instanceof ServerLevel) {
            this.entityData.set(DECOY_COUNT, this.entityData.get(DECOY_COUNT) + 1);
            this.level().playSound(null, this, ModSounds.DECOY_RELOAD.get(), this.getSoundSource(), 1, 1);
            decoyReloadCoolDown = 500;
        }
    }

    //热诱弹诱饵
    public void releaseDecoy() {
        if (decoyInputDown) {
            if (this.entityData.get(DECOY_COUNT) > 0 && this.level() instanceof ServerLevel) {
                for (int i = 0; i < 4; i++) {
                    FlareDecoyEntity flareDecoyEntity = new FlareDecoyEntity(this.level());
                    flareDecoyEntity.setPos(this.getX() + this.getDeltaMovement().x, this.getY() + 0.5 + this.getDeltaMovement().y, this.getZ() + this.getDeltaMovement().z);
                    flareDecoyEntity.decoyShoot(this, this.getViewVector(1).yRot((45 + 90 * i) * Mth.DEG_TO_RAD), 0.8f, 8);
                    this.level().addFreshEntity(flareDecoyEntity);
                }
                this.level().playSound(null, this, ModSounds.DECOY_FIRE.get(), this.getSoundSource(), 2, 1);
                if (this.getEntityData().get(DECOY_COUNT) == 4) {
                    decoyReloadCoolDown = 300;
                }
                this.getEntityData().set(DECOY_COUNT, this.getEntityData().get(DECOY_COUNT) - 1);
            }
            decoyInputDown = false;
        }
        if (this.entityData.get(DECOY_COUNT) < 4 && decoyReloadCoolDown == 0 && this.level() instanceof ServerLevel) {
            this.entityData.set(DECOY_COUNT, this.entityData.get(DECOY_COUNT) + 1);
            this.level().playSound(null, this, ModSounds.DECOY_RELOAD.get(), this.getSoundSource(), 1, 1);
            decoyReloadCoolDown = 300;
        }
    }

    // 惯性倾斜

    public void inertiaRotate(float multiple) {
        float angleX = 0;
        float diffX = (float) (getAcceleration() * multiple - angleX);
        setXRot(getXRot() - 0.5f * diffX);
    }

    public static List<Entity> getPlayer(Level level) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> e instanceof Player)
                .toList();
    }

    // 地形适应测试
    public void terrainCompact(float w, float l) {
        if (onGround()) {
            Matrix4f transform = this.getWheelsTransform(1);

            // 左前
            Vector4f positionLF = transformPosition(transform, w / 2, 0, l / 2);
            // 右前
            Vector4f positionRF = transformPosition(transform, -w / 2, 0, l / 2);
            // 左后
            Vector4f positionLB = transformPosition(transform, w / 2, 0, -l / 2);
            // 右后
            Vector4f positionRB = transformPosition(transform, -w / 2, 0, -l / 2);

            Vec3 p1 = new Vec3(positionLF.x, positionLF.y, positionLF.z);
            Vec3 p2 = new Vec3(positionRF.x, positionRF.y, positionRF.z);
            Vec3 p3 = new Vec3(positionLB.x, positionLB.y, positionLB.z);
            Vec3 p4 = new Vec3(positionRB.x, positionRB.y, positionRB.z);

//            if (mainSupportingBlockPos.isPresent()) {
//                BlockPos blockpos = this.mainSupportingBlockPos.get();
//            }

            // 确定点位是否在墙里来调整点位高度
            float p1y = (float) this.traceBlockY(p1, 3);
            float p2y = (float) this.traceBlockY(p2, 3);
            float p3y = (float) this.traceBlockY(p3, 3);
            float p4y = (float) this.traceBlockY(p4, 3);

            p1 = new Vec3(positionLF.x, p1y, positionLF.z);
            p2 = new Vec3(positionRF.x, p2y, positionRF.z);
            p3 = new Vec3(positionLB.x, p3y, positionLB.z);
            p4 = new Vec3(positionRB.x, p4y, positionRB.z);

            // 测试用粒子效果，用于确定点位位置

//            List<Entity> entities = getPlayer(level());
//            for (var e : entities) {
//                if (e instanceof ServerPlayer player) {
//                    if (player.level() instanceof ServerLevel serverLevel) {
//                        sendParticle(serverLevel, ParticleTypes.END_ROD, p1.x, p1.y, p1.z, 1, 0, 0, 0, 0, true);
//                        sendParticle(serverLevel, ParticleTypes.END_ROD, p2.x, p2.y, p2.z, 1, 0, 0, 0, 0, true);
//                        sendParticle(serverLevel, ParticleTypes.END_ROD, p3.x, p3.y, p3.z, 1, 0, 0, 0, 0, true);
//                        sendParticle(serverLevel, ParticleTypes.END_ROD, p4.x, p4.y, p4.z, 1, 0, 0, 0, 0, true);
//                    }
//                }
//            }

            // 通过点位位置获取角度

            // 左后-左前
            Vec3 v0 = p3.vectorTo(p1);
            // 右后-右前
            Vec3 v1 = p4.vectorTo(p2);
            // 左前-右前
            Vec3 v2 = p1.vectorTo(p2);
            // 左后-右后
            Vec3 v3 = p3.vectorTo(p4);

            double x1 = getXRotFromVector(v0);
            double x2 = getXRotFromVector(v1);
            double z1 = getXRotFromVector(v2);
            double z2 = getXRotFromVector(v3);

            float diffX = Math.clamp(-15f, 15f, Mth.wrapDegrees((float) (-(x1 + x2)) - getXRot()));
            setXRot(Mth.clamp(getXRot() + 0.15f * diffX, -45f, 45f));

            float diffZ = Math.clamp(-15f, 15f, Mth.wrapDegrees((float) (-(z1 + z2)) - getRoll()));
            setZRot(Mth.clamp(getRoll() + 0.15f * diffZ, -45f, 45f));
        } else if (isInWater()) {
            setXRot(getXRot() * 0.9f);
            setZRot(getRoll() * 0.9f);
        }
    }

    //用于履带的地形适应

    public float[] terrainCompactTrackValue(float w, float l) {
        Matrix4f transform = this.getWheelsTransform(1);

        // 左前
        Vector4f positionLF = transformPosition(transform, w / 2, 0, l / 2);
        // 右前
        Vector4f positionRF = transformPosition(transform, -w / 2, 0, l / 2);
        // 左后
        Vector4f positionLB = transformPosition(transform, w / 2, 0, -l / 2);
        // 右后
        Vector4f positionRB = transformPosition(transform, -w / 2, 0, -l / 2);

        Vec3 p1 = new Vec3(positionLF.x, positionLF.y, positionLF.z);
        Vec3 p2 = new Vec3(positionRF.x, positionRF.y, positionRF.z);
        Vec3 p3 = new Vec3(positionLB.x, positionLB.y, positionLB.z);
        Vec3 p4 = new Vec3(positionRB.x, positionRB.y, positionRB.z);

        // 确定点位是否在墙里来调整点位高度
        float p1y = (float) this.traceBlockY(p1, 3);
        float p2y = (float) this.traceBlockY(p2, 3);
        float p3y = (float) this.traceBlockY(p3, 3);
        float p4y = (float) this.traceBlockY(p4, 3);

        p1 = new Vec3(positionLF.x, p1y, positionLF.z);
        p2 = new Vec3(positionRF.x, p2y, positionRF.z);
        p3 = new Vec3(positionLB.x, p3y, positionLB.z);
        p4 = new Vec3(positionRB.x, p4y, positionRB.z);

        Vec3 v0 = p3.vectorTo(p1);
        Vec3 v1 = p4.vectorTo(p2);
        Vec3 v2 = p1.vectorTo(p2);
        Vec3 v3 = p3.vectorTo(p4);

        double x1 = getXRotFromVector(v0);
        double x2 = getXRotFromVector(v1);

        double z1 = getXRotFromVector(v2);
        double z2 = getXRotFromVector(v3);

        float x = Math.clamp(-15f, 15f, Mth.wrapDegrees((float) (-(x1 + x2)) - getXRot()));
        float z = Math.clamp(-15f, 15f, Mth.wrapDegrees((float) (-(z1 + z2)) - getRoll()));

        return new float[]{x, z};
    }

    public Matrix4f getWheelsTransform(float ticks) {
        Matrix4f transform = new Matrix4f();
        transform.translate((float) Mth.lerp(ticks, xo, getX()), (float) Mth.lerp(ticks, yo, getY()), (float) Mth.lerp(ticks, zo, getZ()));
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(ticks, yRotO, getYRot())));
        return transform;
    }

    public double traceBlockY(Vec3 pos, double maxLength) {
        var res = this.level().clip(new ClipContext(pos, pos.add(0, -maxLength, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        double targetY;

        BlockState state = level().getBlockState(BlockPos.containing(pos));
        VoxelShape shape = state.getCollisionShape(level(), BlockPos.containing(pos));
        if (!shape.isEmpty()) {
            targetY = pos.y + shape.max(Direction.Axis.Y);
        } else if (res.getType() == HitResult.Type.BLOCK && this.level().noCollision(new AABB(pos, pos))) {
            targetY = res.getLocation().y;
        } else {
            targetY = pos.y - maxLength;
        }

        double diffY = targetY - pos.y;
        return pos.y + 0.5f * diffY;
    }

    public void baseCollideBlock() {
        if (level() instanceof ServerLevel) {
            AABB aabb = getBoundingBox().inflate(0.25, 1, 0.25).expandTowards(0, 0.5, 1).move(this.getDeltaMovement().scale(1.2));
            BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
                BlockState blockstate = this.level().getBlockState(pos);
                if (blockstate.is(Blocks.LILY_PAD) ||
                        (blockstate.is(BlockTags.LEAVES) && blockstate.hasProperty(LeavesBlock.PERSISTENT) && !blockstate.getValue(LeavesBlock.PERSISTENT)) ||
                        blockstate.is(Blocks.COBWEB) || blockstate.is(Blocks.CACTUS)) {
                    this.level().destroyBlock(pos, true);
                }
            });
        }
    }

    public void collideBlock() {
        if (!VehicleConfig.COLLISION_DESTROY_BLOCKS.get()) return;
        AABB aabb = getBoundingBox().inflate(0.25, 1, 0.25).expandTowards(0, 0.5, 1).move(this.getDeltaMovement().scale(1.2));
        BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
            BlockState blockstate = this.level().getBlockState(pos);
            if (blockstate.is(ModTags.Blocks.SOFT_COLLISION)) {
                this.level().destroyBlock(pos, true);
            }
        });
    }

    public void collideHardBlock() {
        if (!VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.get()) return;
        AABB aabb = getBoundingBox().inflate(0.25, 1, 0.25).expandTowards(0, 0.5, 1).move(this.getDeltaMovement().scale(1.2));
        BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
            BlockState blockstate = this.level().getBlockState(pos);
            if (blockstate.is(ModTags.Blocks.HARD_COLLISION)) {
                this.level().destroyBlock(pos, true);
                this.setDeltaMovement(this.getDeltaMovement().scale(0.95));
            }
        });
    }

    public void collideBlockBeastly() {
        if (!VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.get()) return;
        AABB aabb = getBoundingBox().inflate(0.25, 1, 0.25).expandTowards(0, 0.52, 1).move(this.getDeltaMovement().scale(1.2));
        BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
            BlockState blockstate = this.level().getBlockState(pos);
            float hardness = blockstate.getBlock().defaultDestroyTime();
            if (hardness > 0 && hardness <= 4) {
                this.level().destroyBlock(pos, true);
            }
        });
    }

    public boolean canCollideHardBlock() {
        return false;
    }

    public boolean canCollideBlockBeastly() {
        return false;
    }

    @Override
    public void move(@NotNull MoverType movementType, @NotNull Vec3 movement) {
        if (!this.level().isClientSide()) {
            MobileVehicleEntity.IGNORE_ENTITY_GROUND_CHECK_STEPPING = true;
        }
        if (level() instanceof ServerLevel && canCollideBlockBeastly()) {
            collideBlockBeastly();
        }

        super.move(movementType, movement);
        if (level() instanceof ServerLevel) {
            if (this.horizontalCollision) {
                collideBlock();
                if (canCollideHardBlock()) {
                    collideHardBlock();
                }
            }

            if (lastTickSpeed < 0.3 || collisionCoolDown > 0 || this instanceof DroneEntity) return;
            Entity driver = EntityFindUtil.findEntity(this.level(), this.entityData.get(LAST_DRIVER_UUID));

            if ((verticalCollision)) {
                if (this instanceof HelicopterEntity) {
                    this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, driver == null ? this : driver), (float) (60 * ((lastTickSpeed - 0.3) * (lastTickSpeed - 0.3))));
                    this.bounceVertical(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
                } else if (Mth.abs((float) lastTickVerticalSpeed) > 0.4) {
                    this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, driver == null ? this : driver), (float) (96 * ((Mth.abs((float) lastTickVerticalSpeed) - 0.4) * (lastTickSpeed - 0.3) * (lastTickSpeed - 0.3))));
                    if (!this.level().isClientSide) {
                        this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.getSoundSource(), 1, 1);
                    }
                    this.bounceVertical(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
                }
            }

            if (this.horizontalCollision) {
                this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, driver == null ? this : driver), (float) (126 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))));
                this.bounceHorizontal(Direction.getNearest(this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z()).getOpposite());
                if (!this.level().isClientSide) {
                    this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.getSoundSource(), 1, 1);
                }
                collisionCoolDown = 4;
                crash = true;
                this.entityData.set(POWER, 0.8f * entityData.get(POWER));
            }
        }
    }

    public void bounceHorizontal(Direction direction) {
        collideBlock();
        collideHardBlock();
        switch (direction.getAxis()) {
            case X:
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 0.99, 0.99));
                break;
            case Z:
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.99, 0.99, 0.8));
                break;
        }
    }

    public void bounceVertical(Direction direction) {
        if (!this.level().isClientSide) {
            this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.getSoundSource(), 1, 1);
        }
        collisionCoolDown = 4;
        crash = true;
        if (direction.getAxis() == Direction.Axis.Y) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.9, -0.8, 0.9));
        }
    }

    /**
     * 防止载具堆叠
     */
    public void preventStacking() {
        var Box = getBoundingBox();

        var entities = level().getEntities(EntityTypeTest.forClass(Entity.class), Box, entity -> entity != this && entity != getFirstPassenger() && entity.getVehicle() == null)
                .stream().filter(entity -> entity instanceof VehicleEntity)
                .toList();

        for (var entity : entities) {
            Vec3 toVec = this.position().add(new Vec3(1, 1, 1).scale(random.nextFloat() * 0.01f + 1f)).vectorTo(entity.position());
            Vec3 velAdd = toVec.normalize().scale(Math.max((this.getBbWidth() + 2) - position().distanceTo(entity.position()), 0) * 0.002);
            double entitySize = entity.getBbWidth() * entity.getBbHeight();
            double thisSize = this.getBbWidth() * this.getBbHeight();
            double f = Math.min(entitySize / thisSize, 2);
            double f1 = Math.min(thisSize / entitySize, 2);

            this.pushNew(-f * velAdd.x, -f * velAdd.y, -f * velAdd.z);
            entity.push(f1 * velAdd.x, f1 * velAdd.y, f1 * velAdd.z);
        }
    }

    public void pushNew(double pX, double pY, double pZ) {
        this.setDeltaMovement(this.getDeltaMovement().add(pX, pY, pZ));
    }

    /**
     * 撞击实体并造成伤害
     *
     * @param velocity 动量
     */
    public void crushEntities(Vec3 velocity) {
        if (level() instanceof ServerLevel) {
            if (!this.canCrushEntities()) return;
//            if (velocity.horizontalDistance() < 0.25) return;
            if (isRemoved()) return;

            List<Entity> entities;

            if (this instanceof OBBEntity obbEntity) {
                var frontBox = getBoundingBox().move(velocity).inflate(4);
                entities = level().getEntities(EntityTypeTest.forClass(Entity.class), frontBox,
                                entity -> entity != this && entity != getFirstPassenger() && entity.getVehicle() == null)
                        .stream().filter(entity -> {
                                    if (entity.isAlive() && isInObb(obbEntity, entity, velocity)) {
                                        var type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                                        return (entity instanceof VehicleEntity || entity instanceof Boat || entity instanceof Minecart || (entity instanceof LivingEntity living && !(living instanceof Player player && player.isSpectator()))) || VehicleConfig.COLLISION_ENTITY_WHITELIST.get().contains(type.toString());
                                    }
                                    return false;
                                }
                        )
                        .toList();

            } else {
                var frontBox = getBoundingBox().move(velocity);
                entities = level().getEntities(EntityTypeTest.forClass(Entity.class), frontBox,
                                entity -> entity != this && entity != getFirstPassenger() && entity.getVehicle() == null)
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

            for (var entity : entities) {
                double entitySize = entity.getBoundingBox().getSize();
                double thisSize = this.getBoundingBox().getSize();
                double f;
                double f1;

                // TODO 给非载具实体也设置质量

                if (entity instanceof VehicleEntity vehicle) {
                    f = Mth.clamp(vehicle.getMass() / getMass(), 0.25, 4);
                    f1 = Mth.clamp(getMass() / vehicle.getMass(), 0.25, 4);
                } else {
                    f = Mth.clamp(entitySize / thisSize, 0.25, 4);
                    f1 = Mth.clamp(thisSize / entitySize, 0.25, 4);

                }

                float v = (float) velocity.dot(position().vectorTo(entity.position()));
                var velAdd = position().vectorTo(entity.position()).normalize().scale(0.1 * v);

                if (Mth.abs(v) > 0.3) {
                    if (!this.level().isClientSide) {
                        this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.getSoundSource(), 1, 1);
                    }

                    if (entity instanceof LivingEntity) {
                        entity.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, this.getFirstPassenger() == null ? this : this.getFirstPassenger()), (float) (f1 * 5 * (Mth.abs(v) - 0.3) * (Mth.abs(v) - 0.3)));
                    } else {
                        entity.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, this.getFirstPassenger() == null ? this : this.getFirstPassenger()), (float) (f1 * 2 * (Mth.abs(v) - 0.3) * (Mth.abs(v) - 0.3)));
                    }

                    if (entity instanceof VehicleEntity) {
                        this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), entity, entity.getFirstPassenger() == null ? entity : entity.getFirstPassenger()), (float) (f * (Mth.abs(v) - 0.3) * (Mth.abs(v) - 0.3)));
                    }

                    if (!(entity instanceof TargetEntity)) {
                        this.pushNew(-0.3f * f * velAdd.x, -0.3f * f * velAdd.y, -0.3f * f * velAdd.z);
                    }

                    if (entity instanceof MobileVehicleEntity mobileVehicle) {
                        mobileVehicle.pushNew(f1 * velAdd.x, f1 * velAdd.y, f1 * velAdd.z);
                    } else {
                        entity.push(f1 * velAdd.x, f1 * velAdd.y, f1 * velAdd.z);
                    }
                } else {
                    entity.push(0.3 * f1 * velAdd.x, 0.3 * f1 * velAdd.y, 0.3 * f1 * velAdd.z);
                }
            }
        }
    }

    public boolean isInObb(OBBEntity obbEntity, Entity entity, Vec3 velocity) {
        var obbList = obbEntity.getOBBs();
        for (var obb : obbList) {
            obb = obb.move(velocity);
            if (entity instanceof OBBEntity obbEntity2) {
                var obbList2 = obbEntity2.getOBBs();
                for (var obb2 : obbList2) {
                    return OBB.isColliding(obb, obb2);
                }
            } else {
                return OBB.isColliding(obb, entity.getBoundingBox());
            }
        }
        return false;
    }

    public Vector3f getForwardDirection() {
        return new Vector3f(
                Mth.sin(-getYRot() * ((float) Math.PI / 180)),
                0.0f,
                Mth.cos(getYRot() * ((float) Math.PI / 180))
        ).normalize();
    }

    public Vector3f getRightDirection() {
        return new Vector3f(
                Mth.cos(-getYRot() * ((float) Math.PI / 180)),
                0.0f,
                Mth.sin(getYRot() * ((float) Math.PI / 180))
        ).normalize();
    }

    public SoundEvent getEngineSound() {
        return SoundEvents.EMPTY;
    }

    public float getEngineSoundVolume() {
        return (float) Mth.lerp(Mth.clamp(getDeltaMovement().length(), 0F, 0.5F), 0.0F, 0.7F);
    }

    public double getVelocity() {
        return this.velocity;
    }

    public void setVelocity(double pV) {
        this.velocity = pV;
    }

    public double getAcceleration() {
        return getVelocity() - velocityO;
    }

    public float getRudderRot() {
        return this.rudderRot;
    }

    public void setRudderRot(float pRudderRot) {
        this.rudderRot = pRudderRot;
    }

    public float getLeftWheelRot() {
        return this.leftWheelRot;
    }

    public void setLeftWheelRot(float pLeftWheelRot) {
        this.leftWheelRot = pLeftWheelRot;
    }

    public float getRightWheelRot() {
        return this.rightWheelRot;
    }

    public void setRightWheelRot(float pRightWheelRot) {
        this.rightWheelRot = pRightWheelRot;
    }


    public float getLeftTrack() {
        return this.leftTrack;
    }

    public void setLeftTrack(float pLeftTrack) {
        this.leftTrack = pLeftTrack;
    }

    public float getRightTrack() {
        return this.rightTrack;
    }

    public void setRightTrack(float pRightTrack) {
        this.rightTrack = pRightTrack;
    }

    public float getRotorRot() {
        return this.rotorRot;
    }

    public void setRotorRot(float pRotorRot) {
        this.rotorRot = pRotorRot;
    }

    public float getPropellerRot() {
        return this.propellerRot;
    }

    public void setPropellerRot(float pPropellerRot) {
        this.propellerRot = pPropellerRot;
    }

    public double getRecoilShake() {
        return this.recoilShake;
    }

    public void setRecoilShake(double pRecoilShake) {
        this.recoilShake = pRecoilShake;
    }

    public float getFlap1LRot() {
        return this.flap1LRot;
    }

    public void setFlap1L2Rot(float pFlap1L2Rot) {
        this.flap1L2Rot = pFlap1L2Rot;
    }

    public float getFlap1R2Rot() {
        return this.flap1R2Rot;
    }

    public void setFlap1R2Rot(float pFlap1R2Rot) {
        this.flap1R2Rot = pFlap1R2Rot;
    }

    public float getFlap1L2Rot() {
        return this.flap1L2Rot;
    }

    public void setFlap1LRot(float pFlap1LRot) {
        this.flap1LRot = pFlap1LRot;
    }

    public float getFlap1RRot() {
        return this.flap1RRot;
    }

    public void setFlap1RRot(float pFlap1RRot) {
        this.flap1RRot = pFlap1RRot;
    }

    public float getFlap2LRot() {
        return this.flap2LRot;
    }

    public void setFlap2LRot(float pFlap2LRot) {
        this.flap2LRot = pFlap2LRot;
    }

    public float getFlap2RRot() {
        return this.flap2RRot;
    }

    public void setFlap2RRot(float pFlap2RRot) {
        this.flap2RRot = pFlap2RRot;
    }

    public float getFlap3Rot() {
        return this.flap3Rot;
    }

    public void setFlap3Rot(float pFlap3Rot) {
        this.flap3Rot = pFlap3Rot;
    }

    public boolean hasDecoy() {
        return false;
    }

    public boolean engineRunning() {
        return Math.abs(this.entityData.get(POWER)) > 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CANNON_RECOIL_TIME, 0)
                .define(POWER, 0f)
                .define(YAW, 0f)
                .define(AMMO, 0)
                .define(FIRE_ANIM, 0)
                .define(COAX_HEAT, 0)
                .define(DECOY_COUNT, 0)
                .define(GEAR_ROT, 0)
                .define(GEAR_UP, false)
                .define(PLANE_BREAK, 0f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(POWER, compound.getFloat("Power"));
        this.entityData.set(DECOY_COUNT, compound.getInt("DecoyCount"));
        this.entityData.set(GEAR_ROT, compound.getInt("GearRot"));
        this.entityData.set(GEAR_UP, compound.getBoolean("GearUp"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("Power", this.entityData.get(POWER));
        compound.putInt("DecoyCount", this.entityData.get(DECOY_COUNT));
        compound.putInt("GearRot", this.entityData.get(GEAR_ROT));
        compound.putBoolean("GearUp", this.entityData.get(GEAR_UP));
    }

    public boolean hasTracks() {
        return false;
    }

    public boolean canCrushEntities() {
        return true;
    }
}
