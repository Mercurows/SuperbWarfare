package com.atsuishio.superbwarfare.mixins;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor<T extends Entity> {

    @Accessor(value = "shadowRadius")
    float getShadowRadius();

    @Accessor(value = "shadowStrength")
    float getShadowStrength();
}
