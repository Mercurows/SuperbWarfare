package com.atsuishio.superbwarfare.entity.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.AnimatableManager;

public class Sc250Entity extends Mk82Entity {
    public Sc250Entity(EntityType<? extends Sc250Entity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.explosionRadius = 20;
        this.explosionDamage = 500;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }
}
