package com.atsuishio.superbwarfare.client.animation

import com.google.gson.annotations.SerializedName
import com.maydaymemory.mae.control.runner.*
import java.util.*
import java.util.function.Supplier

enum class AnimationPlayType(val supplier: Supplier<IAnimationState>) {
    @SerializedName("play_once_stop")
    PLAY_ONCE_STOP(Supplier { PlayingState({ System.nanoTime() }, { StopState() }) }),

    @SerializedName("play_once_hold")
    PLAY_ONCE_HOLD(Supplier { PlayingState({ System.nanoTime() }, { PauseState() }) }),

    @SerializedName("loop")
    LOOP(Supplier { LoopingState { System.nanoTime() } });

    fun state(): IAnimationState {
        return supplier.get()
    }

    companion object {
        /**
         * Parse PlayType from string (for JSON deserialization).
         */
        fun fromString(str: String?): AnimationPlayType {
            if (str == null) return LOOP
            return when (str.lowercase(Locale.ROOT)) {
                "play_once_stop" -> PLAY_ONCE_STOP
                "play_once_hold" -> PLAY_ONCE_HOLD
                else -> LOOP
            }
        }
    }
}