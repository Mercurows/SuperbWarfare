package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
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

public class Mle1934Entity extends ArtilleryEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Mle1934Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.MLE_1934.get(), world);
    }

    public Mle1934Entity(EntityType<Mle1934Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.25f) * damage);
    }

    private PlayState fireLeftPredicate(AnimationState<Mle1934Entity> event) {
        if (this.entityData.get(BARREL_ANIM).getInt(1) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mle_1934.fire_left"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mle_1934.idle"));
    }

    private PlayState fireRightPredicate(AnimationState<Mle1934Entity> event) {
        if (this.entityData.get(BARREL_ANIM).getInt(0) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mle_1934.fire_right"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mle_1934.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "fireLeft", 0, this::fireLeftPredicate));
        data.add(new AnimationController<>(this, "fireRight", 0, this::fireRightPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
