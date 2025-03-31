package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public abstract class MobileVehicleEntity extends EnergyVehicleEntity implements ControllableVehicle {
    public static final EntityDataAccessor<Integer> CANNON_RECOIL_TIME = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Integer> FIRE_ANIM = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> HEAT = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> COAX_HEAT = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.INT);

    public static final EntityDataAccessor<Integer> AMMO = SynchedEntityData.defineId(MobileVehicleEntity.class, EntityDataSerializers.INT);

    public boolean leftInputDown;
    public boolean rightInputDown;
    public boolean forwardInputDown;
    public boolean backInputDown;
    public boolean upInputDown;
    public boolean downInputDown;
    public boolean decoyInputDown;
    public double lastTickSpeed;
    public double lastTickVerticalSpeed;
    public int collisionCoolDown;

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

    public boolean cannotFire;
    public boolean cannotFireCoax;
    public int reloadCoolDown;

    public double velocityO;
    public double velocity;

    public MobileVehicleEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public void processInput(short keys) {
        leftInputDown
                = (keys & 0b0000001) > 0;
        rightInputDown
                = (keys & 0b0000010) > 0;
        forwardInputDown
                = (keys & 0b0000100) > 0;
        backInputDown
                = (keys & 0b0001000) > 0;
        upInputDown
                = (keys & 0b0010000) > 0;
        downInputDown
                = (keys & 0b0100000) > 0;
        decoyInputDown
                = (keys & 0b1000000) > 0;
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

        super.baseTick();

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

        if (this.entityData.get(HEAT) > 0) {
            this.entityData.set(HEAT, this.entityData.get(HEAT) - 1);
        }

        if (this.entityData.get(COAX_HEAT) > 0) {
            this.entityData.set(COAX_HEAT, this.entityData.get(COAX_HEAT) - 1);
        }

        if (this.entityData.get(FIRE_ANIM) > 0) {
            this.entityData.set(FIRE_ANIM, this.entityData.get(FIRE_ANIM) - 1);
        }

        if (this.entityData.get(HEAT) < 40) {
            cannotFire = false;
        }

        if (this.entityData.get(COAX_HEAT) < 40) {
            cannotFireCoax = false;
        }

        if (this.entityData.get(HEAT) > 100 && !cannotFire) {
            cannotFire = true;
            this.level().playSound(null, this.getOnPos(), ModSounds.MINIGUN_OVERHEAT.get(), SoundSource.PLAYERS, 1, 1);
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
        if (!(this instanceof DroneEntity)) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.06, 0.0));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        collideLilyPadBlock();
        this.refreshDimensions();
    }

    // 惯性倾斜

    public void inertiaRotate(float multiple) {
        float angleX = 0;
        float diffX = (float) (getAcceleration() * multiple - angleX);
        setXRot(getXRot() - 0.5f * diffX);
    }

    // 地形适应测试
    public void terrainCompat(float w, float l) {
        Matrix4f transform = this.getWheelsTransform(1);

        // 点位
        // 前
        Vector4f positionF = transformPosition(transform, 0, 0, l / 2);
        // 后
        Vector4f positionB = transformPosition(transform, 0, 0, -l / 2);
        // 左
        Vector4f positionL = transformPosition(transform, -w / 2, 0, 0);
        // 右
        Vector4f positionR = transformPosition(transform, w / 2, 0, 0);
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

        Vec3 p5 = new Vec3(positionF.x, positionF.y, positionF.z);
        Vec3 p6 = new Vec3(positionB.x, positionB.y, positionB.z);
        Vec3 p7 = new Vec3(positionL.x, positionL.y, positionL.z);
        Vec3 p8 = new Vec3(positionR.x, positionR.y, positionR.z);

        // 确定点位是否在墙里来调整点位高度
        float p1y = (float) this.traceBlockY(p1, l);
        float p2y = (float) this.traceBlockY(p2, l);
        float p3y = (float) this.traceBlockY(p3, l);
        float p4y = (float) this.traceBlockY(p4, l);

        float p5y = (float) Mth.clamp(this.traceBlockY(p5, l), -l, l);
        float p6y = (float) Mth.clamp(this.traceBlockY(p6, l), -l, l);
        float p7y = (float) Mth.clamp(this.traceBlockY(p7, l), -l, l);
        float p8y = (float) Mth.clamp(this.traceBlockY(p8, l), -l, l);

        p1 = new Vec3(positionLF.x, p1y, positionLF.z);
        p2 = new Vec3(positionRF.x, p2y, positionRF.z);
        p3 = new Vec3(positionLB.x, p3y, positionLB.z);
        p4 = new Vec3(positionRB.x, p4y, positionRB.z);

        // 测试用粒子效果，用于确定点位位置
//        var passenger = this.getFirstPassenger();
//
//        if (passenger != null) {
//            if (passenger.level() instanceof ServerLevel serverLevel) {
//                sendParticle(serverLevel, ParticleTypes.END_ROD, p1.x, p1.y, p1.z, 1, 0, 0, 0, 0, true);
//                sendParticle(serverLevel, ParticleTypes.END_ROD, p2.x, p2.y, p2.z, 1, 0, 0, 0, 0, true);
//                sendParticle(serverLevel, ParticleTypes.END_ROD, p3.x, p3.y, p3.z, 1, 0, 0, 0, 0, true);
//                sendParticle(serverLevel, ParticleTypes.END_ROD, p4.x, p4.y, p4.z, 1, 0, 0, 0, 0, true);
//            }
//        }

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

        float diffX = Math.clamp(-90f, 90f, Mth.wrapDegrees((float) ((x1 + x2)) - this.getXRot()));
        this.setXRot(Mth.clamp(this.getXRot() + 0.075f * diffX, -90f, 90f));

        float diffZ = Math.clamp(-90f, 90f, Mth.wrapDegrees((float) ((z1 + z2)) - this.getRoll()));
        this.setZRot(Mth.clamp(this.getRoll() + 0.15f * diffZ, -90f, 90f));
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

        double targetY = 0;

        if (res.getType() == HitResult.Type.BLOCK) {
            targetY = res.getLocation().y;
        } else if (!this.level().noCollision(new AABB(pos, pos))) {
            targetY = pos.y + maxLength / 2;
        } else if (res.getType() == HitResult.Type.MISS) {
            targetY = pos.y - maxLength / 2;
        }

        double diffY = targetY - pos.y;
        return pos.y + 0.5f * diffY;
    }

    public void collideLilyPadBlock() {
        if (level() instanceof ServerLevel) {
            AABB aabb = getBoundingBox().inflate(0.05).move(this.getDeltaMovement().scale(0.6));
            BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
                BlockState blockstate = this.level().getBlockState(pos);
                if (blockstate.is(Blocks.LILY_PAD)) {
                    this.level().destroyBlock(pos, true);
                }
            });
        }
    }

    public void collideBlock() {
        if (level() instanceof ServerLevel) {
            if (!VehicleConfig.COLLISION_DESTROY_BLOCKS.get()) return;

            AABB aabb = getBoundingBox().move(this.getDeltaMovement().scale(0.5)).inflate(0.1, -0.05, 0.1);
            BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
                BlockState blockstate = this.level().getBlockState(pos);
                if (blockstate.is(ModTags.Blocks.SOFT_COLLISION)) {
                    this.level().destroyBlock(pos, true);
                }
            });
        }
    }

    public void collideHardBlock() {
        if (level() instanceof ServerLevel) {
            if (!VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.get()) return;

            AABB aabb = getBoundingBox().move(this.getDeltaMovement().scale(0.5)).inflate(0.1, -0.05, 0.1);
            BlockPos.betweenClosedStream(aabb).forEach((pos) -> {
                BlockState blockstate = this.level().getBlockState(pos);
                if (blockstate.is(ModTags.Blocks.HARD_COLLISION)) {
                    this.level().destroyBlock(pos, true);
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.95));
                }
            });
        }
    }

    @Override
    public void move(@NotNull MoverType movementType, @NotNull Vec3 movement) {
        if (!this.level().isClientSide()) {
            MobileVehicleEntity.IGNORE_ENTITY_GROUND_CHECK_STEPPING = true;
        }
        super.move(movementType, movement);
        if (level() instanceof ServerLevel) {
            if (lastTickSpeed < 0.3 || collisionCoolDown > 0 || this instanceof DroneEntity) return;
            Entity driver = EntityFindUtil.findEntity(this.level(), this.entityData.get(LAST_DRIVER_UUID));

            if ((verticalCollision)) {
                if (this instanceof HelicopterEntity) {
                    this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, driver == null ? this : driver), (float) (20 * ((lastTickSpeed - 0.3) * (lastTickSpeed - 0.3))));
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
                this.entityData.set(POWER, 0.4f * entityData.get(POWER));
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
            if (velocity.horizontalDistance() < 0.25) return;
            if (isRemoved()) return;
            var frontBox = getBoundingBox().move(velocity.scale(0.6));
            var velAdd = velocity.add(0, 0, 0).scale(0.9);

            var entities = level().getEntities(EntityTypeTest.forClass(Entity.class), frontBox,
                            entity -> entity != this && entity != getFirstPassenger() && entity.getVehicle() == null)
                    .stream().filter(entity -> {
                                if (entity.isAlive()) {
                                    var type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                                    if (type == null) return false;
                                    return (entity instanceof VehicleEntity || entity instanceof Boat || entity instanceof Minecart
                                            || (entity instanceof LivingEntity living && !(living instanceof Player player && player.isSpectator())))
                                            || VehicleConfig.COLLISION_ENTITY_WHITELIST.get().contains(type.toString());
                                }
                                return false;
                            }
                    )
                    .toList();

            for (var entity : entities) {
                double entitySize = entity.getBbWidth() * entity.getBbHeight();
                double thisSize = this.getBbWidth() * this.getBbHeight();
                double f = Math.min(entitySize / thisSize, 2) * 0.5;
                double f1 = Math.min(thisSize / entitySize, 4) * 2;

                if (velocity.length() > 0.3 && getBoundingBox().distanceToSqr(entity.getBoundingBox().getCenter()) < 1) {
                    if (!this.level().isClientSide) {
                        this.level().playSound(null, this, ModSounds.VEHICLE_STRIKE.get(), this.getSoundSource(), 1, 1);
                    }
                    if (!(entity instanceof TargetEntity)) {
                        this.pushNew(-f * velAdd.x, -f * velAdd.y, -f * velAdd.z);
                    }

                    if (entity instanceof MobileVehicleEntity mobileVehicle) {
                        mobileVehicle.pushNew(f1 * velAdd.x, f1 * velAdd.y, f1 * velAdd.z);
                    } else {
                        entity.push(f1 * velAdd.x, f1 * velAdd.y, f1 * velAdd.z);
                    }

                    entity.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, this.getFirstPassenger() == null ? this : this.getFirstPassenger()), (float) (thisSize * 20 * ((velocity.length() - 0.3) * (velocity.length() - 0.3))));
                    if (entities instanceof VehicleEntity) {
                        this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), entity, entity.getFirstPassenger() == null ? entity : entity.getFirstPassenger()), (float) (entitySize * 10 * ((velocity.length() - 0.3) * (velocity.length() - 0.3))));
                    }
                } else {
                    entity.push(0.3 * f1 * velAdd.x, 0.3 * f1 * velAdd.y, 0.3 * f1 * velAdd.z);
                }
            }
        }
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

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CANNON_RECOIL_TIME, 0);
        this.entityData.define(POWER, 0f);
        this.entityData.define(YAW, 0f);
        this.entityData.define(AMMO, 0);
        this.entityData.define(FIRE_ANIM, 0);
        this.entityData.define(HEAT, 0);
        this.entityData.define(COAX_HEAT, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(POWER, compound.getFloat("Power"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("Power", this.entityData.get(POWER));
    }

    public boolean canCrushEntities() {
        return true;
    }

    public static boolean IGNORE_ENTITY_GROUND_CHECK_STEPPING = false;
}
