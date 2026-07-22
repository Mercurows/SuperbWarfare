package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.client.lighting.ProjectileLightHelper;
import com.atsuishio.superbwarfare.entity.mixin.ExplosionAccess;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixes into the vanilla Explosion class to trigger real-time client-side
 * dynamic light flashes whenever an explosion is finalized, and implements
 * {@link ExplosionAccess} to provide radius access.
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin implements ExplosionAccess {

    @Shadow @Final private Level level;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;
    @Shadow @Final private float radius;

    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void sbw$onExplosionClient(boolean spawnParticles, CallbackInfo ci) {
        if (this.level.isClientSide()) {
            // Emits highly performant, propagated light from the epicenter of the explosion
            ProjectileLightHelper.emitExplosionFlashDirect(
                this.level, 
                new Vec3(this.x, this.y, this.z), 
                this.radius
            );
        }
    }

    /**
     * Implements the getter from {@link ExplosionAccess}.
     * Returns the shadowed radius field.
     *
     * @return the explosion radius
     */
    @Override
    public float superbwarfare$getRadius() {
        return this.radius;
    }
}