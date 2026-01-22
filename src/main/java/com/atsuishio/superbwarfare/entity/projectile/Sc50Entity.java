package com.atsuishio.superbwarfare.entity.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;

public class Sc50Entity extends Mk82Entity {
    public Sc50Entity(EntityType<? extends Sc50Entity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.explosionRadius = 11;
        this.explosionDamage = 120;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public float getVolume() {
        return 0.4f;
    }

    @Override
    public float getMaxHealth() {
        return 25;
    }
}
