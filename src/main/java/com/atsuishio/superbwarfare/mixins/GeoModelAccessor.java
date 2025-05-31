package com.atsuishio.superbwarfare.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import software.bernie.geckolib.model.GeoModel;

@Mixin(GeoModel.class)
public interface GeoModelAccessor {

    @Accessor(value = "animTime", remap = false)
    double getAnimTime();

    @Accessor(value = "animTime", remap = false)
    void setAnimTime(double animTime);

    @Accessor(value = "lastGameTickTime", remap = false)
    double getLastGameTickTime();

    @Accessor(value = "lastGameTickTime", remap = false)
    void setLastGameTickTime(double lastGameTickTime);

    @Accessor(value = "lastRenderedInstance", remap = false)
    long getLastRenderedInstance();

    @Accessor(value = "lastRenderedInstance", remap = false)
    void setLastRenderedInstance(long lastRenderedInstance);
}
