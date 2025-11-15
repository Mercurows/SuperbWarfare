package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
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

public class Bl132Entity extends ArtilleryEntity implements GeoEntity, WeaponVehicleEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Bl132Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.BL_132.get(), world);
    }

    public Bl132Entity(EntityType<Bl132Entity> type, Level world) {
        super(type, world);
    }


    private PlayState fire1Predicate(AnimationState<Bl132Entity> event) {
        if (this.entityData.get(BARREL_ANIM).getInt(3) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.bl_132.fire_1"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bl_132.idle"));
    }

    private PlayState fire2Predicate(AnimationState<Bl132Entity> event) {
        if (this.entityData.get(BARREL_ANIM).getInt(2) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.bl_132.fire_2"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bl_132.idle"));
    }

    private PlayState fire3Predicate(AnimationState<Bl132Entity> event) {
        if (this.entityData.get(BARREL_ANIM).getInt(1) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.bl_132.fire_3"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bl_132.idle"));
    }

    private PlayState fire4Predicate(AnimationState<Bl132Entity> event) {
        if (this.entityData.get(BARREL_ANIM).getInt(0) > 0) {
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

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
