package com.atsuishio.superbwarfare.entity;

import net.minecraft.sounds.SoundEvent;

public interface LoudlyEntity {

    SoundEvent getCloseSound ();
    SoundEvent getSound ();

    float getVolume();
}
