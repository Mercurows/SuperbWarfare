package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WaveforceTowerEntity extends AutoAimableEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    public WaveforceTowerEntity(EntityType<WaveforceTowerEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    @Override
    public void baseTick() {
        super.baseTick();
    }

    private PlayState barrelLightPredicate(AnimationState<WaveforceTowerEntity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.waveforce_tower.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "barrelLight", 0, this::barrelLightPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
