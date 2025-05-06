package com.atsuishio.superbwarfare.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

public interface LoudlyEntity {

    @NotNull
    default SoundEvent getCloseSound() {
        return SoundEvents.EMPTY;
    }

    @NotNull
    SoundEvent getSound();

    float getVolume();
}
