package com.atsuishio.superbwarfare.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimationProcessor;

@Mixin(AnimationProcessor.class)
public interface AnimationProcessorAccessor<T extends GeoAnimatable> {

    @Accessor(value = "model", remap = false)
    CoreGeoModel<T> getModel();
}
