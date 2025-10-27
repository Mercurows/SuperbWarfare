package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;

public class Ah6Entity extends VehicleEntity implements GeoEntity, WeaponVehicleEntity, OBBEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OBB obb;
    public OBB obb2;
    public OBB obb3;
    public OBB obb4;
    public OBB obb5;
    public OBB obb6;
    public OBB obb7;

    public Ah6Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.AH_6.get(), world);
    }

    public Ah6Entity(EntityType<Ah6Entity> type, Level world) {
        super(type, world);
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(1.0625f, 1.18125f, 1.625f), new Quaternionf(), OBB.Part.BODY);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(0.875f, 0.6875f, 0.59375f), new Quaternionf(), OBB.Part.BODY);
        this.obb3 = new OBB(this.position().toVector3f(), new Vector3f(0.25f, 0.3125f, 2.25f), new Quaternionf(), OBB.Part.BODY);
        this.obb4 = new OBB(this.position().toVector3f(), new Vector3f(0.0625f, 1.15625f, 0.40625f), new Quaternionf(), OBB.Part.BODY);
        this.obb5 = new OBB(this.position().toVector3f(), new Vector3f(1f, 0.25f, 0.21875f), new Quaternionf(), OBB.Part.BODY);
        this.obb6 = new OBB(this.position().toVector3f(), new Vector3f(0.3125f, 0.40625f, 0.84375f), new Quaternionf(), OBB.Part.ENGINE1);
        this.obb7 = new OBB(this.position().toVector3f(), new Vector3f(0.3125f, 0.40625f, 0.40625f), new Quaternionf(), OBB.Part.ENGINE2);
    }

    // TODO 切换弹药这块有问题

    @Override
    public void changeWeapon(int index, int value, boolean isScroll) {
        var gunData = getGunData(index);
        if (gunData == null) return;

        var ammoList = gunData.get(GunProp.AMMO_CONSUMER);
        var targetIndex = isScroll ? (value + gunData.selectedAmmoType.get()) % ammoList.size() : value;
        setWeaponIndex(index, targetIndex);
        var soundInfo = gunData.get(GunProp.SOUND_INFO);

        // TODO 正确播放武器切换音效
        SoundEvent soundEvent = soundInfo.getSoundEvent(soundInfo.change);

        if (soundEvent != null) {
            this.level().playSound(null, this, soundEvent, this.getSoundSource(), 1, 1);
        }

    }

    @Override
    public void setWeaponIndex(int index, int type) {
        modifyGunData(index, gunData -> gunData.changeAmmoConsumer(type, getAmmoSupplier()));
    }

    @Override
    public int getWeaponIndex(int index) {
        var gunData = getGunData(index);
        if (gunData == null) return 0;

        var consumersSize = gunData.get(GunProp.AMMO_CONSUMER).size();
        return Mth.clamp(gunData.selectedAmmoType.get(), 0, consumersSize - 1);
    }

    @Override
    public int getAmmoCount(LivingEntity passenger, int weaponIndex) {
        var gunData = getGunData(getSeatIndex(passenger));
        if (gunData == null || gunData.selectedAmmoType.get() != weaponIndex) return 0;

        return gunData.backupAmmoCount.get();
    }

    @Override
    public VehicleWeapon[][] initWeapons() {
        return null;
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(7, 1, -2.7);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> {
                    var entity = source.getDirectEntity();
                    if (entity != null && entity.getType().is(ModTags.EntityTypes.AERIAL_BOMB)) {
                        damage *= 2;
                    }
                    damage *= getHealth() > 0.1f ? 0.7f : 0.05f;
                    return damage;
                });
    }

    @Override
    public void baseTick() {
        super.baseTick();
        updateOBB();
        lowHealthWarning();
        this.terrainCompact(2.7f, 2.7f);

        this.refreshDimensions();
    }

    @Override
    public SoundEvent getEngineSound() {
        return ModSounds.HELICOPTER_ENGINE.get();
    }

    @Override
    public float getEngineSoundVolume() {
        return entityData.get(PROPELLER_ROT) * 2f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void vehicleShoot(LivingEntity living, int type) {
        var seatIndex = getSeatIndex(living);

        modifyGunData(seatIndex, data -> {
            if (!data.canShoot(getAmmoSupplier())) return;
            data.shoot(new ShootParameters(getAmmoSupplier(), living, (ServerLevel) this.level(), getShootPos(living, 1), getShootVec(living, 1), data, data.get(GunProp.SPREAD), true, null, null));
        });

        sendParticle((ServerLevel) this.level(), ParticleTypes.LARGE_SMOKE, getShootPos(living, 1).x, getShootPos(living, 1).y, getShootPos(living, 1).z, 1, 0.02, 0.02, 0.02, 0, false);
        playShootSound3p(living, 0, 4, 12, 24, getShootPos(living, 1));
        ShakeClientMessage.sendToNearbyPlayers(this, 5, 6, 5, 9);

        var data = getGunData(getSeatIndex(living));
        if (data != null) {
            var list = data.get(GunProp.SHOOT_POS).positions.list;
            this.currentFirePosIndex = ++this.currentFirePosIndex % list.size();
        }
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
        return seatIndex == 0 ? 0 : original;
    }

    @Override
    public double getMouseSensitivity() {
        return 0.25;
    }

    @Override
    public double getMouseSpeedX() {
        return 0.4;
    }

    @Override
    public double getMouseSpeedY() {
        return 0.25;
    }

    @Override
    public float rotateYOffset() {
        return 1.45f;
    }

    @Override
    public @NotNull Vec3 getDismountLocationForIndex(LivingEntity passenger, int index) {
        Matrix4f transform = getVehicleTransform(1);
        Vector4f worldPosition;

        if (index == 0) {
            worldPosition = transformPosition(transform, 2f, 1.2f, 1f);
        } else if (index == 1) {
            worldPosition = transformPosition(transform, -2f, 1.2f, 1f);
        } else if (index == 2) {
            worldPosition = transformPosition(transform, -2f, 1.2f, 0);
        } else {
            worldPosition = transformPosition(transform, 2f, 1.2f, 0);
        }

        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2, this.obb3, this.obb4, this.obb5, this.obb6, this.obb7);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        Vector4f worldPosition = transformPosition(transform, 0, 1.86875f, -0.15625f);
        this.obb.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.obb.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition2 = transformPosition(transform, 0, 1.5f, 1.90625f);
        this.obb2.center().set(new Vector3f(worldPosition2.x, worldPosition2.y, worldPosition2.z));
        this.obb2.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition3 = transformPosition(transform, 0, 2.3125f, -4.1875f);
        this.obb3.center().set(new Vector3f(worldPosition3.x, worldPosition3.y, worldPosition3.z));
        this.obb3.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition4 = transformPosition(transform, -0.125f, 2.34375f, -6.34375f);
        this.obb4.center().set(new Vector3f(worldPosition4.x, worldPosition4.y, worldPosition4.z));
        this.obb4.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition5 = transformPosition(transform, -0.125f, 3.5625f, -6.65625f);
        this.obb5.center().set(new Vector3f(worldPosition5.x, worldPosition5.y, worldPosition5.z));
        this.obb5.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition6 = transformPosition(transform, 0, 3.28125f, -0.53125f);
        this.obb6.center().set(new Vector3f(worldPosition6.x, worldPosition6.y, worldPosition6.z));
        this.obb6.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition7 = transformPosition(transform, 0.1875f, 2.09375f, -6.15625f);
        this.obb7.center().set(new Vector3f(worldPosition7.x, worldPosition7.y, worldPosition7.z));
        this.obb7.setRotation(VectorTool.combineRotations(1, this));
    }

    @Override
    public int getContainerSize() {
        return 102;
    }
}
