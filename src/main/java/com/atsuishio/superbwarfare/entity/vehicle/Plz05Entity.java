package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PlayMessages;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Plz05Entity extends ArtilleryEntity implements GeoEntity, OBBEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Plz05Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.PLZ_05.get(), world);
    }

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
