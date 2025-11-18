package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Plz05Entity extends ArtilleryEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Plz05Entity(EntityType<Plz05Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (getLeftTrack() < 0) {
            setLeftTrack(100);
        }

        if (getLeftTrack() > 100) {
            setLeftTrack(0);
        }

        if (getRightTrack() < 0) {
            setRightTrack(100);
        }

        if (getRightTrack() > 100) {
            setRightTrack(0);
        }
        if (getNthEntity(getTurretControllerIndex()) == null && getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            entityData.set(SHOOT_VEC, getForwardDirection());
        }
    }

    private PlayState shootPredicate(AnimationState<Plz05Entity> event) {
        if (getShootAnimationTimer(1, 0) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.plz_05.shoot"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.plz_05.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "shoot", 0, this::shootPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
