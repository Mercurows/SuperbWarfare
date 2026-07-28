package com.atsuishio.superbwarfare.mixins;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Provides direct field-level access to {@link Entity#onGround} (SRG: {@code f_19796_}),
 * bypassing {@link Entity#setOnGround(boolean)} which internally calls the expensive
 */
@Mixin(Entity.class)
public interface EntityOnGroundAccessor {

    /**
     * Writes the {@code onGround} field directly
     *
     * @param value new ground state
     */
    @Accessor("onGround")
    void sbw$setOnGroundRaw(boolean value);
}