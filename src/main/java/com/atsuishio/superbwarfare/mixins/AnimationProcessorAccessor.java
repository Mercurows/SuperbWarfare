package com.atsuishio.superbwarfare.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.model.GeoModel;

@Mixin(AnimationProcessor.class)
public interface AnimationProcessorAccessor<T extends GeoAnimatable> {

    @Accessor(value = "model", remap = false)
    GeoModel<T> getModel();
}
