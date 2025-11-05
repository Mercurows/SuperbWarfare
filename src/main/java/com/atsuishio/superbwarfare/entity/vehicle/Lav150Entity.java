package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class Lav150Entity extends VehicleEntity implements GeoEntity, WeaponVehicleEntity, OBBEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OBB obb;
    public OBB obb2;
    public OBB obb3;
    public OBB obb4;
    public OBB obb5;
    public OBB obb6;
    public OBB obb7;
    public OBB obb8;
    public OBB obbTurret;

    public Lav150Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.LAV_150.get(), world);
    }

    public Lav150Entity(EntityType<Lav150Entity> type, Level world) {
        super(type, world);
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(0.3f, 0.75f, 0.75f), new Quaternionf(), OBB.Part.WHEEL_RIGHT);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(0.3f, 0.75f, 0.75f), new Quaternionf(), OBB.Part.WHEEL_LEFT);
        this.obb3 = new OBB(this.position().toVector3f(), new Vector3f(0.3f, 0.75f, 0.75f), new Quaternionf(), OBB.Part.WHEEL_LEFT);
        this.obb4 = new OBB(this.position().toVector3f(), new Vector3f(0.3f, 0.75f, 0.75f), new Quaternionf(), OBB.Part.WHEEL_RIGHT);
        this.obb5 = new OBB(this.position().toVector3f(), new Vector3f(1.3125f, 0.90625f, 2.4375f), new Quaternionf(), OBB.Part.BODY);
        this.obb6 = new OBB(this.position().toVector3f(), new Vector3f(1.3125f, 0.53125f, 0.34375f), new Quaternionf(), OBB.Part.BODY);
        this.obb7 = new OBB(this.position().toVector3f(), new Vector3f(1.3125f, 0.625f, 0.53125f), new Quaternionf(), OBB.Part.BODY);
        this.obb8 = new OBB(this.position().toVector3f(), new Vector3f(0.71875f, 0.46875f, 0.875f), new Quaternionf(), OBB.Part.ENGINE1);
        this.obbTurret = new OBB(this.position().toVector3f(), new Vector3f(0.875f, 0.3625f, 1.25f), new Quaternionf(), OBB.Part.TURRET);
    }

    @Override
    public int getAmmoCount(LivingEntity passenger, int weaponIndex) {
        var gunData = getGunData(getSeatIndex(passenger));
        if (gunData == null || gunData.selectedAmmoType.get() != weaponIndex) return 0;

        return gunData.backupAmmoCount.get();
    }

    // TODO 移除这个
    @Override
    public VehicleWeapon[][] initWeapons() {
        return null;
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(2.75 + ClientMouseHandler.custom3pDistanceLerp, 1, 0);
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        this.playSound(ModSounds.WHEEL_STEP.get(), (float) (getDeltaMovement().length() * 0.1), random.nextFloat() * 0.15f + 1.05f);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.25f) * damage);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        updateOBB();
        lowHealthWarning();
        this.terrainCompact(2.7f, 3.61f);
        inertiaRotate(1.25f);
        this.refreshDimensions();
    }

    // 炮塔最大水平旋转速度
    @Override
    public float turretYSpeed() {
        return 10;
    }

    // 炮塔最大俯仰旋转速度
    @Override
    public float turretXSpeed() {
        return 12.5F;
    }

    // 炮塔最小俯角
    @Override
    public float turretMinPitch() {
        return -15f;
    }

    // 炮塔最大仰角
    @Override
    public float turretMaxPitch() {
        return 32.5f;
    }


    @Override
    public boolean canCollideHardBlock() {
        return getDeltaMovement().horizontalDistance() > 0.09 || Mth.abs(this.entityData.get(POWER)) > 0.15;
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.LAV_ENGINE.get();
    }

    @Override
    public float getEngineSoundVolume() {
        return Mth.abs(entityData.get(POWER)) * 0.4f;
    }

    @Override
    public Vec3 getBarrelPosition() {
        return new Vec3(0.0234375, 0.33795, 0.825);
    }

    @Override
    public Vec3 getTurretPosition() {
        return new Vec3(0, 2.4003, 0);
    }

    // TODO 正确播放动画
    private PlayState firePredicate(AnimationState<Lav150Entity> event) {
        if (this.entityData.get(FIRE_ANIM) > 1 && getWeaponIndex(0) == 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.lav_150.fire"));
        }

        if (this.entityData.get(FIRE_ANIM) > 0 && getWeaponIndex(0) == 1) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.lav_150.fire2"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.lav_150.idle"));
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
        var data = getGunData(getSeatIndex(living));
        if (data == null) return 0;
        return data.get(GunProp.RPM);
    }

    // client side
    @Override
    public boolean canShoot(LivingEntity living) {
        var gunData = getGunData(getSeatIndex(living));
        return gunData != null && gunData.canShoot(getAmmoSupplier());
    }

    // TODO 正确计算AmmoCount
    @Override
    public int getAmmoCount(LivingEntity living) {
        var data = getGunData(getSeatIndex(living));
        if (data == null) return 0;
        return data.useBackpackAmmo() ? data.backupAmmoCount.get() : data.ammo.get();
    }

    @Override
    public int zoomFov() {
        return 3;
    }

    @Override
    public int getWeaponHeat(LivingEntity living) {
        var gunData = getGunData(getSeatIndex(living));
        if (gunData == null) return 0;
        return Math.toIntExact(Math.round(gunData.heat.get()));
    }

    @Override
    public double getSensitivity(double original, boolean zoom, int seatIndex, boolean isOnGround) {
        return zoom ? 0.23 : Minecraft.getInstance().options.getCameraType().isFirstPerson() ? 0.3 : 0.4;
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2, this.obb3, this.obb4, this.obb5, this.obb6, this.obb7, this.obb8, this.obbTurret);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        Vector4f worldPosition = transformPosition(transform, -1.140625f, 0.75f, 1.584375f);
        this.obb.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.obb.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition2 = transformPosition(transform, 1.140625f, 0.75f, 1.584375f);
        this.obb2.center().set(new Vector3f(worldPosition2.x, worldPosition2.y, worldPosition2.z));
        this.obb2.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition3 = transformPosition(transform, 1.140625f, 0.75f, -1.571875f);
        this.obb3.center().set(new Vector3f(worldPosition3.x, worldPosition3.y, worldPosition3.z));
        this.obb3.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition4 = transformPosition(transform, -1.140625f, 0.75f, -1.571875f);
        this.obb4.center().set(new Vector3f(worldPosition4.x, worldPosition4.y, worldPosition4.z));
        this.obb4.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition5 = transformPosition(transform, 0, 1.53125f, -0.4375f);
        this.obb5.center().set(new Vector3f(worldPosition5.x, worldPosition5.y, worldPosition5.z));
        this.obb5.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition6 = transformPosition(transform, 0, 1.90625f, -3.21875f);
        this.obb6.center().set(new Vector3f(worldPosition6.x, worldPosition6.y, worldPosition6.z));
        this.obb6.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition7 = transformPosition(transform, 0, 1.4375f, 2.53125f);
        this.obb7.center().set(new Vector3f(worldPosition7.x, worldPosition7.y, worldPosition7.z));
        this.obb7.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition8 = transformPosition(transform, 0.65625f, 2.03125f, -2.0625f);
        this.obb8.center().set(new Vector3f(worldPosition8.x, worldPosition8.y, worldPosition8.z));
        this.obb8.setRotation(VectorTool.combineRotations(1, this));

        Matrix4f transformT = getTurretTransform(1);
        Vector4f worldPositionT = transformPosition(transformT, 0, 0.3625f, 0f);
        this.obbTurret.center().set(new Vector3f(worldPositionT.x, worldPositionT.y, worldPositionT.z));
        this.obbTurret.setRotation(VectorTool.combineRotationsTurret(1, this));
    }

    @Override
    public boolean hasTurret() {
        return true;
    }
}
