package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Bmp2Entity extends VehicleEntity implements GeoEntity, WeaponVehicleEntity, OBBEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    public Bmp2Entity(EntityType<Bmp2Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public void baseTick() {
        super.baseTick();
    }

    private PlayState cannonFirePredicate(AnimationState<Bmp2Entity> event) {
        if (getShootAnimationTimer(0, 0) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.lav_150.fire"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.lav_150.idle"));
    }

    private PlayState machineGunFirePredicate(AnimationState<Bmp2Entity> event) {
        if (getShootAnimationTimer(0, 1) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.lav_150.fire2"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.lav_150.idle2"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "cannon", 0, this::cannonFirePredicate));
        data.add(new AnimationController<>(this, "machineGun", 0, this::machineGunFirePredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

}
