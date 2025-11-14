package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.mixin.ExplosionAccess;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Explosion.class)
public class ExplosionMixin implements ExplosionAccess {

    @Shadow
    @Final
    private float radius;

    @Override
    public float superbwarfare$getRadius() {
        return this.radius;
    }
}
