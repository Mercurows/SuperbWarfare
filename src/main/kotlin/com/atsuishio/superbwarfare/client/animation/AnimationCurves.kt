package com.atsuishio.superbwarfare.client.animation

import java.util.function.Function
import kotlin.math.pow
import kotlin.math.sqrt

// https://easings.net/
object AnimationCurves {
    @JvmField
    val LINEAR = Function { x: Double -> x }

    @JvmField
    val EASE_OUT_CIRC = Function { x: Double -> sqrt(1 - (x - 1).pow(2.0)) }

    @JvmField
    val EASE_IN_EXPO =
        Function { x: Double -> if (x == 0.0) 0.0 else 2.0.pow(10 * x - 10) }

    @JvmField
    val EASE_OUT_EXPO =
        Function { x: Double -> if (x == 1.0) 1.0 else (1 - 2.0.pow(-10 * x)) }

    @JvmField
    val EASE_IN_OUT_QUINT =
        Function { x: Double -> if (x < 0.5) 4 * x * x * x else (1 - (-2 * x + 2).pow(3.0) / 2) }

    @JvmField
    val EASE_IN_QUART = Function { x: Double -> x.pow(4.0) }

    // wtf
    @JvmField
    val PARABOLA = Function { x: Double -> -(2 * x - 1).pow(2.0) + 1 }
}