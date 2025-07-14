package com.atsuishio.superbwarfare.tools.animation;


import java.util.function.Function;

// https://easings.net/
public class AnimationCurves {

    public static final Function<Double, Double> LINEAR = x -> x;
    public static final Function<Double, Double> EASE_OUT_CIRC = x -> Math.sqrt(1 - Math.pow(x - 1, 2));
    public static final Function<Double, Double> EASE_IN_EXPO = x -> x == 0 ? 0 : Math.pow(2, 10 * x - 10);
    public static final Function<Double, Double> EASE_OUT_EXPO = x -> x == 1 ? 1 : (1 - Math.pow(2, -10 * x));
    public static final Function<Double, Double> EASE_IN_OUT_QUINT = x -> x < 0.5 ? 4 * x * x * x : (1 - Math.pow(-2 * x + 2, 3) / 2);
    public static final Function<Double, Double> EASE_IN_QUART = x -> Math.pow(x, 4);

    // wtf
    public static final Function<Double, Double> PARABOLA = x -> -Math.pow(2 * x - 1, 2) + 1;
}
