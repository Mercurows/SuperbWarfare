package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.*;

public class Bl132Entity extends ArtilleryEntity implements WeaponVehicleEntity {

    public Bl132Entity(EntityType<Bl132Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.5f) * damage);
    }

    private PlayState fire1Predicate(AnimationState<Bl132Entity> event) {
        if (this.entityData.get(BARREL_ANIM).get(3) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.bl_132.fire_1"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bl_132.idle"));
    }

    private PlayState fire2Predicate(AnimationState<Bl132Entity> event) {
        if (this.entityData.get(BARREL_ANIM).get(2) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.bl_132.fire_2"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bl_132.idle"));
    }

    private PlayState fire3Predicate(AnimationState<Bl132Entity> event) {
        if (this.entityData.get(BARREL_ANIM).get(1) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.bl_132.fire_3"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bl_132.idle"));
    }

    private PlayState fire4Predicate(AnimationState<Bl132Entity> event) {
        if (this.entityData.get(BARREL_ANIM).getFirst() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.bl_132.fire_4"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bl_132.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "fire1", 0, this::fire1Predicate));
        data.add(new AnimationController<>(this, "fire2", 0, this::fire2Predicate));
        data.add(new AnimationController<>(this, "fire3", 0, this::fire3Predicate));
        data.add(new AnimationController<>(this, "fire4", 0, this::fire4Predicate));
    }
}
