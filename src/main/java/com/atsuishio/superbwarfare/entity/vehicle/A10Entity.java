package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.joml.Math;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.zoomVehicle;

public class A10Entity extends VehicleEntity implements GeoEntity, WeaponVehicleEntity, OBBEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public String lockingTargetO = "none";
    public String lockingTarget = "none";
    public float destroyRot;
    public int lockTime;
    public boolean locked;

    public OBB obb;
    public OBB obb2;
    public OBB obb3;
    public OBB obb4;
    public OBB obb5;
    public OBB obb6;
    public OBB obb7;
    public OBB obb8;
    public OBB obb9;
    public OBB obb10;
    public OBB obb11;

    public A10Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.A_10A.get(), world);
    }

    public A10Entity(EntityType<A10Entity> type, Level world) {
        super(type, world);
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(0.6875f, 1.09375f, 3.65625f), new Quaternionf(), OBB.Part.BODY);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(8.8125f, 0.3125f, 1.40625f), new Quaternionf(), OBB.Part.BODY);
        this.obb3 = new OBB(this.position().toVector3f(), new Vector3f(3.1875f, 0.125f, 0.96875f), new Quaternionf(), OBB.Part.BODY);
        this.obb4 = new OBB(this.position().toVector3f(), new Vector3f(0.0625f, 1.09375f, 0.84375f), new Quaternionf(), OBB.Part.BODY);
        this.obb5 = new OBB(this.position().toVector3f(), new Vector3f(0.0625f, 1.09375f, 0.84375f), new Quaternionf(), OBB.Part.BODY);
        this.obb6 = new OBB(this.position().toVector3f(), new Vector3f(0.625f, 0.78125f, 1.09375f), new Quaternionf(), OBB.Part.BODY);
        this.obb7 = new OBB(this.position().toVector3f(), new Vector3f(0.6875f, 0.75f, 2.9375f), new Quaternionf(), OBB.Part.BODY);
        this.obb8 = new OBB(this.position().toVector3f(), new Vector3f(0.75f, 0.75f, 1.5625f), new Quaternionf(), OBB.Part.MAIN_ENGINE);
        this.obb9 = new OBB(this.position().toVector3f(), new Vector3f(0.75f, 0.75f, 1.5625f), new Quaternionf(), OBB.Part.SUB_ENGINE);
        this.obb10 = new OBB(this.position().toVector3f(), new Vector3f(0.34375f, 0.359375f, 1.78125f), new Quaternionf(), OBB.Part.BODY);
        this.obb11 = new OBB(this.position().toVector3f(), new Vector3f(0.34375f, 0.359375f, 1.78125f), new Quaternionf(), OBB.Part.BODY);
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(17, 3, 0);
    }

    @Override
    public void baseTick() {
        this.lockingTargetO = getTargetUuid();

        super.baseTick();
        this.updateOBB();
        float f = (float) Mth.clamp(Math.max((onGround() ? 0.819f : 0.82f) - 0.005 * getDeltaMovement().length(), 0.5) + 0.001f * Mth.abs(90 - (float) VehicleVecUtils.calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90, 0.01, 0.99);

        boolean forward = getDeltaMovement().dot(getViewVector(1)) > 0;
        this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).scale((forward ? 0.227 : 0.1) * getDeltaMovement().dot(getViewVector(1)))));
        this.setDeltaMovement(this.getDeltaMovement().multiply(f, f, f));

        if (this.isInWater() && this.tickCount % 4 == 0) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.6, 0.6));
            if (lastTickSpeed > 0.4) {
                this.hurt(ModDamageTypes.causeVehicleStrikeDamage(this.level().registryAccess(), this, this.getFirstPassenger() == null ? this : this.getFirstPassenger()), (float) (20 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))));
            }
        }

        if (onGround()) {
            terrainCompactA10();
        }

        if (this.getWeaponIndex(0) == 3) {
            seekTarget();
        }

        lowHealthWarning();
        this.refreshDimensions();
    }

    @Override
    public void onEngine1Damaged(Vec3 pos) {
        if (level().isClientSide) {
            float random = 2 * (this.random.nextFloat() - 0.5f);
            addRandomParticle(ModParticleTypes.FIRE_STAR.get(), pos, 0, level(), 0.25f, 5);
            addRandomParticle(ParticleTypes.LARGE_SMOKE, pos, 0.5f, level(), 0.001f, 1);
            addRandomParticle(new CustomCloudOption(1f, 0.25f, 0, (int) (240 + 40 * random), 2.5f + 0.5f * random, -0.07f, true, true), pos, 0.5f, level(), 1.5f, 1);
        }
    }

    @Override
    public void onEngine2Damaged(Vec3 pos) {
        if (level().isClientSide) {
            float random = 2 * (this.random.nextFloat() - 0.5f);
            addRandomParticle(ModParticleTypes.FIRE_STAR.get(), pos, 0, level(), 0.25f, 5);
            addRandomParticle(ParticleTypes.LARGE_SMOKE, pos, 0.5f, level(), 0.001f, 1);
            addRandomParticle(new CustomCloudOption(1f, 0.25f, 0, (int) (240 + 40 * random), 2.5f + 0.5f * random, -0.07f, true, true), pos, 0.5f, level(), 1.5f, 1);
        }
    }

    public void terrainCompactA10() {
        if (onGround()) {
            Matrix4f transform = this.getWheelsTransform(1);

            // 前
            Vector4f positionF = transformPosition(transform, 0.141675f, 0, 4.6315125f);
            // 左后
            Vector4f positionLB = transformPosition(transform, 2.5752f, 0, -0.7516125f);
            // 右后
            Vector4f positionRB = transformPosition(transform, -2.5752f, 0, -0.7516125f);

            Vec3 p1 = new Vec3(positionF.x, positionF.y, positionF.z);
            Vec3 p2 = new Vec3(positionLB.x, positionLB.y, positionLB.z);
            Vec3 p3 = new Vec3(positionRB.x, positionRB.y, positionRB.z);

            // 确定点位是否在墙里来调整点位高度
            float p1y = (float) this.traceBlockY(p1, 3);
            float p2y = (float) this.traceBlockY(p2, 3);
            float p3y = (float) this.traceBlockY(p3, 3);

            p1 = new Vec3(positionF.x, p1y, positionF.z);
            p2 = new Vec3(positionLB.x, p2y, positionLB.z);
            p3 = new Vec3(positionRB.x, p3y, positionRB.z);
            Vec3 p4 = p2.add(p3).scale(0.5);

            // 通过点位位置获取角度

            // 左后-右后
            Vec3 v1 = p2.vectorTo(p3);
            // 后-前
            Vec3 v2 = p4.vectorTo(p1);

            double x = VehicleVecUtils.getXRotFromVector(v2);
            double z = VehicleVecUtils.getXRotFromVector(v1);

            float diffX = Math.clamp(-5f, 5f, Mth.wrapDegrees((float) (-2 * x) - getXRot()));
            setXRot(Mth.clamp(getXRot() + 0.05f * diffX, -45f, 45f));

            float diffZ = Math.clamp(-5f, 5f, Mth.wrapDegrees((float) (-2 * z) - getRoll()));
            setZRot(Mth.clamp(getRoll() + 0.05f * diffZ, -45f, 45f));
        } else if (isInWater()) {
            setXRot(getXRot() * 0.9f);
            setZRot(getRoll() * 0.9f);
        }
    }

    public void seekTarget() {
        if (!(this.getFirstPassenger() instanceof Player player)) return;

        if (getTargetUuid().equals(lockingTargetO) && !getTargetUuid().equals("none")) {
            lockTime++;
        } else {
            resetSeek(player);
        }

        Entity entity = new SeekTool.Builder(this)
                .withinRange(384)
                .withinAngle(18)
                .baseFilter()
                .onGround(10)
                .sizeBiggerThan(0.9)
                .smokeFilter()
                .noVehicle()
                .noClip()
                .buildWithClosest();

        if (entity != null) {
            if (lockTime == 0) {
                setTargetUuid(String.valueOf(entity.getUUID()));
            }
            if (!String.valueOf(entity.getUUID()).equals(getTargetUuid())) {
                resetSeek(player);
                setTargetUuid(String.valueOf(entity.getUUID()));
            }
        } else {
            setTargetUuid("none");
        }

        if (lockTime == 1) {
            if (player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, ModSounds.MISSILE_LOCKING.get(), 2, 1);
            }
        }

        if (lockTime > 10) {
            if (player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, ModSounds.MISSILE_LOCKED.get(), 2, 1);
            }
            locked = true;
        }
    }

    public void resetSeek(Player player) {
        lockTime = 0;
        locked = false;
        if (player instanceof ServerPlayer serverPlayer) {
            var clientboundstopsoundpacket = new ClientboundStopSoundPacket(new ResourceLocation(Mod.MODID, "jet_lock"), SoundSource.PLAYERS);
            serverPlayer.connection.send(clientboundstopsoundpacket);
        }
    }

    public void setTargetUuid(String uuid) {
        this.lockingTarget = uuid;
    }

    public String getTargetUuid() {
        return this.lockingTarget;
    }

    @Override
    public void travel() {
        Entity passenger = this.getFirstPassenger();

        if (getHealth() > 0.1f * getMaxHealth()) {
            if (passenger == null || isInWater()) {
                setLeftInputDown(false);
                setRightInputDown(false);
                setForwardInputDown(false);
                setBackInputDown(false);
                this.entityData.set(POWER, this.entityData.get(POWER) * 0.95f);
                if (onGround()) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.94, 1, 0.94));
                } else {
                    this.setXRot(Mth.clamp(this.getXRot() + 0.1f, -89, 89));
                }
            } else if (passenger instanceof Player) {
                if (getEnergy() > 0) {
                    if (forwardInputDown()) {
                        this.entityData.set(POWER, Math.min(this.entityData.get(POWER) + 0.004f, sprintInputDown() ? 1f : 0.0575f));
                    }

                    if (backInputDown()) {
                        this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - 0.002f, onGround() ? -0.05f : 0.01f));
                    }
                }

                if (!onGround()) {
                    if (rightInputDown()) {
                        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 1.2f);
                    } else if (this.leftInputDown()) {
                        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 1.2f);
                    }
                }

                // 刹车
                if (downInputDown()) {
                    if (onGround()) {
                        this.entityData.set(POWER, this.entityData.get(POWER) * 0.8f);
                        this.setDeltaMovement(this.getDeltaMovement().multiply(0.97, 1, 0.97));
                    } else {
                        this.entityData.set(POWER, this.entityData.get(POWER) * 0.97f);
                        this.setDeltaMovement(this.getDeltaMovement().multiply(0.994, 1, 0.994));
                    }
                    this.entityData.set(PLANE_BREAK, Math.min(this.entityData.get(PLANE_BREAK) + 10, 60f));
                }
            }

            if (getEnergy() > 0) {
                this.consumeEnergy((int) (Mth.abs(this.entityData.get(POWER)) * 5 * VehicleConfig.A_10_MAX_ENERGY_COST.get()));
            }

            float rotSpeed = 1.5f + 2 * Mth.abs(VectorTool.calculateY(getRoll()));

            float addY = Mth.clamp(Math.max((this.onGround() ? 0.1f : 0.2f) * (float) getDeltaMovement().length(), 0f) * getMouseMoveSpeedX(), -rotSpeed, rotSpeed);
            float addX = Mth.clamp(Math.min((float) Math.max(getDeltaMovement().dot(getViewVector(1)) - 0.24, 0.15), 0.4f) * getMouseMoveSpeedY(), -3.5f, 3.5f);
            float addZ = this.entityData.get(DELTA_ROT) - (this.onGround() ? 0 : 0.004f) * getMouseMoveSpeedX() * (float) getDeltaMovement().dot(getViewVector(1));

            this.setYRot(this.getYRot() + addY);
            if (!onGround()) {
                this.setXRot(this.getXRot() + addX);
                this.setZRot(this.getRoll() - addZ);
            }

            // 自动回正
            if (!onGround()) {
                float xSpeed = 1 + 20 * Mth.abs(getXRot() / 180);
                float speed = Mth.clamp(Mth.abs(roll) / (90 / xSpeed), 0, 1);

                if (this.roll > 0) {
                    setZRot(roll - Math.min(speed, roll));
                } else if (this.roll < 0) {
                    setZRot(roll + Math.min(speed, -roll));
                }
            }

            this.setPropellerRot(this.getPropellerRot() + 30 * this.entityData.get(POWER));

            // 起落架
            if (upInputDown()) {
                setUpInputDown(false);
                if (entityData.get(GEAR_ROT) == 0 && !onGround()) {
                    entityData.set(GEAR_UP, true);
                } else if (entityData.get(GEAR_ROT) == 85) {
                    entityData.set(GEAR_UP, false);
                }
            }

            if (onGround()) {
                entityData.set(GEAR_UP, false);
            }

            if (entityData.get(GEAR_UP)) {
                entityData.set(GEAR_ROT, Math.min(entityData.get(GEAR_ROT) + 5, 85));
            } else {
                entityData.set(GEAR_ROT, Math.max(entityData.get(GEAR_ROT) - 5, 0));
            }

            float flapX = (1 - (Mth.abs(getRoll())) / 90) * Mth.clamp(getMouseMoveSpeedY(), -22.5f, 22.5f) - VectorTool.calculateY(getRoll()) * Mth.clamp(getMouseMoveSpeedX(), -22.5f, 22.5f);

            setFlap1LRot(Mth.clamp(-flapX - 4 * addZ - this.entityData.get(PLANE_BREAK), -22.5f, 22.5f));
            setFlap1RRot(Mth.clamp(-flapX + 4 * addZ - this.entityData.get(PLANE_BREAK), -22.5f, 22.5f));
            setFlap1L2Rot(Mth.clamp(-flapX - 4 * addZ + this.entityData.get(PLANE_BREAK), -22.5f, 22.5f));
            setFlap1R2Rot(Mth.clamp(-flapX + 4 * addZ + this.entityData.get(PLANE_BREAK), -22.5f, 22.5f));

            setFlap2LRot(Mth.clamp(flapX - 4 * addZ, -22.5f, 22.5f));
            setFlap2RRot(Mth.clamp(flapX + 4 * addZ, -22.5f, 22.5f));

            float flapY = (1 - (Mth.abs(getRoll())) / 90) * Mth.clamp(getMouseMoveSpeedX(), -22.5f, 22.5f) + VectorTool.calculateY(getRoll()) * Mth.clamp(getMouseMoveSpeedY(), -22.5f, 22.5f);

            setFlap3Rot(flapY * 5);
        } else if (!onGround()) {
            float diffX;
            this.entityData.set(POWER, Math.max(this.entityData.get(POWER) - 0.0003f, 0.02f));
            destroyRot += 0.1f;
            diffX = 90 - this.getXRot();
            this.setXRot(this.getXRot() + diffX * 0.001f * destroyRot);
            this.setZRot(this.getRoll() - destroyRot);
            setDeltaMovement(getDeltaMovement().add(0, -0.03, 0));
            setDeltaMovement(getDeltaMovement().add(0, -destroyRot * 0.005, 0));
        }

        this.entityData.set(POWER, this.entityData.get(POWER) * 0.99f);
        this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * 0.85f);
        this.entityData.set(PLANE_BREAK, this.entityData.get(PLANE_BREAK) * 0.8f);

        if (entityData.get(MAIN_ENGINE_DAMAGED)) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.96f);
        }

        if (entityData.get(SUB_ENGINE_DAMAGED)) {
            this.entityData.set(POWER, this.entityData.get(POWER) * 0.96f);
        }

        Matrix4f transform = getVehicleTransform(1);
        double flapAngle = (getFlap1LRot() + getFlap1RRot() + getFlap1L2Rot() + getFlap1R2Rot()) / 4;

        Vector4f force0 = transformPosition(transform, 0, 0, 0);
        Vector4f force1 = transformPosition(transform, 0, 1, 0);

        Vec3 force = new Vec3(force0.x, force0.y, force0.z).vectorTo(new Vec3(force1.x, force1.y, force1.z));

        setDeltaMovement(getDeltaMovement().add(force.scale(getDeltaMovement().dot(getViewVector(1)) * 0.022 * (1 + Math.sin((onGround() ? 25 : flapAngle + 25) * Mth.DEG_TO_RAD)))));

        this.setDeltaMovement(this.getDeltaMovement().add(getViewVector(1).scale(0.4 * this.entityData.get(POWER))));
    }

    @Override
    public @NotNull Vec3 getDismountLocationForIndex(LivingEntity passenger, int index) {
        Matrix4f transform = getVehicleTransform(1);
        if ((!onGround() || getDeltaMovement().length() >= 0.1)) {
            Vector4f worldPosition = transformPosition(transform, 0, 4.025f, 3.7f);
            return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
        } else {
            return super.getDismountLocationForIndex(passenger, index);
        }
    }

    @Override
    public @NotNull Vec3 getDismountMovement(LivingEntity passenger, int index) {
        return getDeltaMovement().add(new Vec3(0, 4, 0));
    }

    @Override
    public boolean allowEjection() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public double getMouseSensitivity() {
        return zoomVehicle ? 0.03 : 0.07;
    }

    @Override
    public double getMouseSpeedX() {
        return 0.3;
    }

    @Override
    public double getMouseSpeedY() {
        return 0.3;
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2, this.obb3, this.obb4, this.obb5, this.obb6, this.obb7, this.obb8, this.obb9, this.obb10, this.obb11);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        Vector4f worldPosition = transformPosition(transform, 0, 2.65625f, 1.71875f);
        this.obb.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.obb.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition2 = transformPosition(transform, 0, 2.35f, -0.46875f);
        this.obb2.center().set(new Vector3f(worldPosition2.x, worldPosition2.y, worldPosition2.z));
        this.obb2.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition3 = transformPosition(transform, 0, 2.4375f, -6.71875f);
        this.obb3.center().set(new Vector3f(worldPosition3.x, worldPosition3.y, worldPosition3.z));
        this.obb3.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition4 = transformPosition(transform, -3.125f, 3.65625f, -6.71875f);
        this.obb4.center().set(new Vector3f(worldPosition4.x, worldPosition4.y, worldPosition4.z));
        this.obb4.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition5 = transformPosition(transform, 3.125f, 3.65625f, -6.71875f);
        this.obb5.center().set(new Vector3f(worldPosition5.x, worldPosition5.y, worldPosition5.z));
        this.obb5.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition6 = transformPosition(transform, 0f, 2.34375f, 6.46875f);
        this.obb6.center().set(new Vector3f(worldPosition6.x, worldPosition6.y, worldPosition6.z));
        this.obb6.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition7 = transformPosition(transform, 0f, 2.5625f, -4.875f);
        this.obb7.center().set(new Vector3f(worldPosition7.x, worldPosition7.y, worldPosition7.z));
        this.obb7.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition8 = transformPosition(transform, -1.625f, 3.375f, -3.5f);
        this.obb8.center().set(new Vector3f(worldPosition8.x, worldPosition8.y, worldPosition8.z));
        this.obb8.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition9 = transformPosition(transform, 1.625f, 3.375f, -3.5f);
        this.obb9.center().set(new Vector3f(worldPosition9.x, worldPosition9.y, worldPosition9.z));
        this.obb9.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition10 = transformPosition(transform, -2.703125f, 1.921875f, 0.03125f);
        this.obb10.center().set(new Vector3f(worldPosition10.x, worldPosition10.y, worldPosition10.z));
        this.obb10.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition11 = transformPosition(transform, 2.703125f, 1.921875f, 0.03125f);
        this.obb11.center().set(new Vector3f(worldPosition11.x, worldPosition11.y, worldPosition11.z));
        this.obb11.setRotation(VectorTool.combineRotations(1, this));
    }
}
