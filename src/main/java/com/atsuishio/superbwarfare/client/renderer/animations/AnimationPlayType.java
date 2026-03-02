package com.atsuishio.superbwarfare.client.renderer.animations;

import com.google.gson.annotations.SerializedName;
import com.maydaymemory.mae.control.runner.*;

import java.util.function.Supplier;

public enum AnimationPlayType {

    @SerializedName("play_once_stop")
    PLAY_ONCE_STOP(() -> new PlayingState(System::nanoTime, StopState::new)),

    @SerializedName("play_once_hold")
    PLAY_ONCE_HOLD(() -> new PlayingState(System::nanoTime, PauseState::new)),

    @SerializedName("loop")
    LOOP(() -> new LoopingState(System::nanoTime));

    final Supplier<IAnimationState> supplier;

    AnimationPlayType(Supplier<IAnimationState> supplier) {
        this.supplier = supplier;
    }

    public IAnimationState state() {
        return supplier.get();
    }

    /**
     * Parse PlayType from string (for JSON deserialization).
     */
    public static AnimationPlayType fromString(String str) {
        if (str == null) return LOOP;
        return switch (str.toLowerCase()) {
            case "play_once_stop" -> PLAY_ONCE_STOP;
            case "play_once_hold" -> PLAY_ONCE_HOLD;
            default -> LOOP;
        };
    }

}