package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class Lav150Entity extends GeoVehicleEntity {

    public Lav150Entity(EntityType<Lav150Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.25f) * damage);
    }

    private PlayState cannonFirePredicate(AnimationState<Lav150Entity> event) {
        if (getShootAnimationTimer(0, 0) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.lav_150.fire"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.lav_150.idle"));
    }

    private PlayState machineGunFirePredicate(AnimationState<Lav150Entity> event) {
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
}
