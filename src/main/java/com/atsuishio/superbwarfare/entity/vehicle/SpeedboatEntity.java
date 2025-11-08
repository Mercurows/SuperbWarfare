package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.VehicleWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
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

import java.util.List;

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
        return null;
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(3 + ClientMouseHandler.custom3pDistanceLerp, 1, 0);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        this.updateOBB();
        this.lowHealthWarning();
        this.inertiaRotate(2);
        this.terrainCompact(2f, 3f);
        this.refreshDimensions();
    }

    @Override
    public float getEngineSoundVolume() {
        return (Mth.abs(entityData.get(POWER)) - 0.01f) * 0.4f;
    }

    private PlayState machineGunFirePredicate(AnimationState<SpeedboatEntity> event) {
        if (getShootAnimationTimer(0, 0) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.speedboat.fire"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.speedboat.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "machineGun", 0, this::machineGunFirePredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public int zoomFov() {
        return 1;
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
}
