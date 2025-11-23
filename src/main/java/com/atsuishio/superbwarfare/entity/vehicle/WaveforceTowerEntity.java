package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity;
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

public class WaveforceTowerEntity extends AutoAimableEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    public WaveforceTowerEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.WAVEFORCE_TOWER.get(), world);
    }

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
