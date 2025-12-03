package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class Plz05Entity extends ArtilleryEntity {

    public Plz05Entity(EntityType<Plz05Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.3f) * damage);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (getNthEntity(getTurretControllerIndex()) == null && getDeltaMovement().horizontalDistanceSqr() > 0.007) {
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
    public boolean canBind() {
        return true;
    }
}
