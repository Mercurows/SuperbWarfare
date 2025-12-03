package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.*;

public class Mle1934Entity extends ArtilleryEntity {

    public Mle1934Entity(EntityType<Mle1934Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.25f) * damage);
    }

    private PlayState fireLeftPredicate(AnimationState<Mle1934Entity> event) {
        if (this.entityData.get(BARREL_ANIM).get(1) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.mle_1934.fire_left"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.mle_1934.idle"));
    }

    private PlayState fireRightPredicate(AnimationState<Mle1934Entity> event) {
        if (this.entityData.get(BARREL_ANIM).getFirst() > 0) {
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
    public boolean canBind() {
        return true;
    }
}
