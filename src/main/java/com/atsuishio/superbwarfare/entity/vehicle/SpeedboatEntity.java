package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.data.gun.Ammo;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.ProjectileWeapon;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public class SpeedboatEntity extends VehicleEntity implements GeoEntity, ArmedVehicleEntity, WeaponVehicleEntity, OBBEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OBB obb;
    public OBB obb2;

    public SpeedboatEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.SPEEDBOAT.get(), world);
    }

    public SpeedboatEntity(EntityType<SpeedboatEntity> type, Level world) {
        super(type, world);
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(1.5625f, 0.75f, 3.1875f), new Quaternionf(), OBB.Part.BODY);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(1.0625f, 0.5f, 1.90625f), new Quaternionf(), OBB.Part.BODY);
    }

    @Override
    public VehicleWeapon[][] initWeapons() {
        return new VehicleWeapon[][]{
                new VehicleWeapon[]{
                        new ProjectileWeapon()
                                .damage(VehicleConfig.HEAVY_MACHINE_GUN_DAMAGE.get())
                                .headShot(2)
                                .zoom(false)
                                .icon(Mod.loc("textures/screens/vehicle_weapon/gun_12_7mm.png"))
                                .sound1p(ModSounds.M_2_HB_FIRE_1P.get())
                                .sound3p(ModSounds.M_2_HB_FIRE_3P.get())
                                .sound3pFar(ModSounds.M_2_HB_FAR.get())
                                .sound3pVeryFar(ModSounds.M_2_HB_VERYFAR.get())
                }
        };
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(3 + ClientMouseHandler.custom3pDistanceLerp, 1, 0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    public void baseTick() {
        super.baseTick();
        this.updateOBB();

        double fluidFloat = 0.12 * getSubmergedHeight(this);
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, fluidFloat, 0.0));

        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 0.99, 0.2));
        } else if (isInWater()) {
            float f = (float) (0.75f - (0.04f * java.lang.Math.min(getSubmergedHeight(this), this.getBbHeight())) + 0.09f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90);
            this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).normalize().scale(0.04 * getDeltaMovement().dot(getViewVector(1)))));
            this.setDeltaMovement(this.getDeltaMovement().multiply(f, 0.85, f));
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.99, 0.99, 0.99));
        }

        if (this.level() instanceof ServerLevel serverLevel && this.isInWater() && this.getDeltaMovement().length() > 0.1) {
            sendParticle(serverLevel, ParticleTypes.CLOUD, this.getX() + 0.5 * this.getDeltaMovement().x, this.getY() + getSubmergedHeight(this) - 0.2, this.getZ() + 0.5 * this.getDeltaMovement().z, (int) (2 + 4 * this.getDeltaMovement().length()), 0.65, 0, 0.65, 0, true);
            sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, this.getX() + 0.5 * this.getDeltaMovement().x, this.getY() + getSubmergedHeight(this) - 0.2, this.getZ() + 0.5 * this.getDeltaMovement().z, (int) (2 + 10 * this.getDeltaMovement().length()), 0.65, 0, 0.65, 0, true);
            sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP, this.getX() - 4.5 * this.getLookAngle().x, this.getY() - 0.25, this.getZ() - 4.5 * this.getLookAngle().z, (int) (40 * Mth.abs(this.entityData.get(POWER))), 0.15, 0.15, 0.15, 0.02, true);
        }

        if (this.level() instanceof ServerLevel) {
            this.handleAmmo();
        }

        this.lowHealthWarning();
        this.inertiaRotate(2);
        this.terrainCompact(2f, 3f);

        this.refreshDimensions();
    }

    // 炮塔最大水平旋转速度
    @Override
    public float turretYSpeed() {
        return 25;
    }

    // 炮塔最大俯仰旋转速度
    @Override
    public float turretXSpeed() {
        return 25F;
    }

    // 炮塔最小俯角
    @Override
    public float turretMinPitch() {
        return -25f;
    }

    // 炮塔最大仰角
    @Override
    public float turretMaxPitch() {
        return 50f;
    }

    // 炮弹发射位置
    @Override
    public Vec3 getTurretShootPos(int seatIndex, float ticks) {
        Matrix4f transform = getBarrelTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0, 0.20106875f, 0);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    // 炮弹发射速度
    @Override
    public float projectileVelocity(Entity entity) {
        return 20;
    }

    // 炮弹重力
    @Override
    public float projectileGravity(Entity entity) {
        return 0.05f;
    }

    @Override
    public boolean canCollideHardBlock() {
        return getDeltaMovement().horizontalDistance() > 0.15;
    }

    private void handleAmmo() {
        if (!(this.getFirstPassenger() instanceof Player)) return;

        int ammoCount = this.getItemStacks().stream().filter(stack -> {
            if (stack.is(ModItems.AMMO_BOX.get())) {
                return Ammo.HEAVY.get(stack) > 0;
            }
            return false;
        }).mapToInt(Ammo.HEAVY::get).sum() + countItem(ModItems.HEAVY_AMMO.get());

        this.entityData.set(AMMO, ammoCount);
    }

    /**
     * 机枪塔开火
     */
    @Override
    public void vehicleShoot(LivingEntity living, int type) {
        if (this.cannotFire) return;
        var projectile = ((ProjectileWeapon) getWeapon(0)).create(living).setGunItemId(this.getType().getDescriptionId());

        projectile.bypassArmorRate(0.4f);
        projectile.setPos(getTurretShootPos(living, 1).x, getTurretShootPos(living, 1).y, getTurretShootPos(living, 1).z);
        projectile.shoot(living, getBarrelVector(1).x, getBarrelVector(1).y, getBarrelVector(1).z, projectileVelocity(living),
                (float) 0.4);
        this.level().addFreshEntity(projectile);

        playShootSound3p(living, 0, 4, 12, 24, new Vec3(getTurretShootPos(living, 1).x, getTurretShootPos(living, 1).y, getTurretShootPos(living, 1).z));

        ShakeClientMessage.sendToNearbyPlayers(this, 5, 6, 5, 5);

        this.entityData.set(CANNON_RECOIL_TIME, 30);
        this.entityData.set(YAW, getTurretYRot());

        this.entityData.set(HEAT, this.entityData.get(HEAT) + 4);
        this.entityData.set(FIRE_ANIM, 3);

        boolean hasCreativeAmmo = false;
        for (int i = 0; i < getMaxPassengers() - 1; i++) {
            if (InventoryTool.hasCreativeAmmoBox(getNthEntity(i))) {
                hasCreativeAmmo = true;
            }
        }

        if (!hasCreativeAmmo) {
            ItemStack ammoBox = this.getItemStacks().stream().filter(stack -> {
                if (stack.is(ModItems.AMMO_BOX.get())) {
                    return Ammo.HEAVY.get(stack) > 0;
                }
                return false;
            }).findFirst().orElse(ItemStack.EMPTY);

            if (!ammoBox.isEmpty()) {
                Ammo.HEAVY.add(ammoBox, -1);
            } else {
                this.getItemStacks().stream().filter(stack -> stack.is(ModItems.HEAVY_AMMO.get())).findFirst().ifPresent(stack -> stack.shrink(1));
            }
        }
    }

    @Override
    public void travel() {
        Entity passenger0 = this.getFirstPassenger();

        if (this.getEnergy() > 0) {
            if (passenger0 == null) {
                setLeftInputDown(false);
                setRightInputDown(false);
                setForwardInputDown(false);
                setBackInputDown(false);
            }

            if (forwardInputDown()) {
                this.entityData.set(POWER, this.entityData.get(POWER) + 0.005f);
            }

            if (backInputDown()) {
                this.entityData.set(POWER, this.entityData.get(POWER) - 0.005f);
                if (rightInputDown()) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 0.1f);
                } else if (leftInputDown()) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 0.1f);
                }
            } else {
                if (rightInputDown()) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) - 0.1f);
                } else if (this.leftInputDown()) {
                    this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) + 0.1f);
                }
            }

            this.consumeEnergy((int) (Mth.abs(this.entityData.get(POWER)) * VehicleConfig.SPEEDBOAT_ENERGY_COST.get()));

            this.entityData.set(POWER, this.entityData.get(POWER) * 0.96f);
            this.entityData.set(DELTA_ROT, this.entityData.get(DELTA_ROT) * 0.8f);

            this.setRotorRot(this.getRotorRot() + 30 * this.entityData.get(POWER));
            this.setRudderRot(Mth.clamp(this.getRudderRot() - this.entityData.get(DELTA_ROT), -1.25f, 1.25f) * 0.7f * (this.entityData.get(POWER) > 0 ? 1 : -1));

            if (this.isInWater() || this.isUnderWater()) {
                this.setXRot(this.getXRot() * 0.85f);
                float direct = (90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90;

                this.setXRot((float) (this.getXRot() - direct * (this.onGround() ? 0 : 1) * 1.1f * this.getDeltaMovement().horizontalDistance()));
                this.setYRot((float) (this.getYRot() - Math.max(12 * this.getDeltaMovement().length(), 0.8) * this.entityData.get(DELTA_ROT)));
                this.setZRot((float) (this.getRoll() - direct * this.entityData.get(DELTA_ROT) * (this.onGround() ? 0 : 1) * 10 * this.getDeltaMovement().horizontalDistance()));

                this.setDeltaMovement(this.getDeltaMovement().add(getViewVector(1).scale(this.entityData.get(POWER) * 1.75f)));
            } else {
                this.setXRot(this.getXRot() * 0.99f);
            }
        }

        this.setZRot(this.roll * 0.85f);
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.BOAT_ENGINE.get();
    }

    @Override
    public float getEngineSoundVolume() {
        return (Mth.abs(entityData.get(POWER)) - 0.01f) * 2f;
    }

    @Override
    public Vec3 driverZoomPos(float ticks) {
        Matrix4f transform = getBarrelTransform(ticks);

        float x = 0f;
        float y = 0.5f;
        float z = -0.25f;

        Vector4f worldPosition = transformPosition(transform, x, y, z);

        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public Vec3 getBarrelPosition() {
        return new Vec3(0, 0.5088375, 0.04173125);
    }

    @Override
    public Vec3 getTurretPosition() {
        return new Vec3(0, 2.5616625, -0.565625);
    }

    @Override
    public float rotateYOffset() {
        return 1f;
    }

    private PlayState firePredicate(AnimationState<SpeedboatEntity> event) {
        if (this.entityData.get(FIRE_ANIM) > 1) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.speedboat.fire"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.speedboat.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement", 0, this::firePredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public int mainGunRpm(LivingEntity living) {
        return 500;
    }

    @Override
    public boolean canShoot(LivingEntity living) {
        return (this.entityData.get(AMMO) > 0 || InventoryTool.hasCreativeAmmoBox(living))
                && !cannotFire;
    }

    @Override
    public int getAmmoCount(LivingEntity living) {
        return this.entityData.get(AMMO);
    }

    @Override
    public int zoomFov() {
        return 1;
    }

    @Override
    public int getWeaponHeat(LivingEntity living) {
        return entityData.get(HEAT);
    }

    @Override
    public Vec3 getGunVec(float ticks) {
        return getBarrelVector(ticks);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (this.getSeatIndex(player) == 0 && zoom) {
            return new Vec2((float) -getYRotFromVector(this.getBarrelVector(partialTicks)), (float) -getXRotFromVector(this.getBarrelVector(partialTicks)));
        }
        return super.getCameraRotation(partialTicks, player, zoom, isFirstPerson);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vec3 getCameraPosition(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (this.getSeatIndex(player) == 0 && zoom) {
            return new Vec3(this.driverZoomPos(partialTicks).x, this.driverZoomPos(partialTicks).y, this.driverZoomPos(partialTicks).z);
        }
        return super.getCameraPosition(partialTicks, player, zoom, isFirstPerson);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean useFixedCameraPos(Entity entity) {
        return this.getSeatIndex(entity) == 0;
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        Vector4f worldPosition = transformPosition(transform, 0, 0.875f, 0.375f);
        this.obb.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.obb.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition2 = transformPosition(transform, 0, 2.0625f, -0.71875f);
        this.obb2.center().set(new Vector3f(worldPosition2.x, worldPosition2.y, worldPosition2.z));
        this.obb2.setRotation(VectorTool.combineRotations(1, this));
    }

    @Override
    public boolean hasTurret() {
        return true;
    }
}
