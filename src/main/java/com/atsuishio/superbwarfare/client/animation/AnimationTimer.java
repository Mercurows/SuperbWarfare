package com.atsuishio.superbwarfare.client.animation;

import net.minecraft.util.Mth;

import java.util.function.Function;

/**
 * 可以更改计时方向的动画计时器
 */
public class AnimationTimer {

    private final long forwardDuration;
    private final long backwardDuration;
    private long startTime;
    private boolean isForward = true;
    private boolean initialized;

    // 未初始化状态下，动画进度是否从0开始
    private boolean playFromStart;

    private Function<Double, Double> forwardAnimationCurve = AnimationCurves.LINEAR;
    private Function<Double, Double> backwardAnimationCurve = AnimationCurves.LINEAR;

    /**
     * 创建一个动画计时器
     *
     * @param duration 动画持续时间，单位为毫秒
     */
    public AnimationTimer(long duration) {
        this.forwardDuration = duration;
        this.backwardDuration = duration;
    }

    /**
     * 创建一个动画计时器
     *
     * @param forwardDuration  正向动画持续时间，单位为毫秒
     * @param backwardDuration 反向动画持续时间，单位为毫秒
     */
    public AnimationTimer(long forwardDuration, long backwardDuration) {
        this.forwardDuration = forwardDuration;
        this.backwardDuration = backwardDuration;
    }

    /**
     * 设置正向和反向计时时采用的动画曲线
     */
    public AnimationTimer animation(Function<Double, Double> animationCurve) {
        return this.forwardAnimation(animationCurve).backwardAnimation(animationCurve);
    }

    /**
     * 设置反向计时时采用的动画曲线
     */
    public AnimationTimer forwardAnimation(Function<Double, Double> animationCurve) {
        this.forwardAnimationCurve = animationCurve;
        return this;
    }

    /**
     * 设置反向计时时采用的动画曲线
     */
    public AnimationTimer backwardAnimation(Function<Double, Double> animationCurve) {
        this.backwardAnimationCurve = animationCurve;
        return this;
    }

    /**
     * 创建多个线性动画计时器
     *
     * @param size     计时器数量
     * @param duration 动画持续时间，单位为毫秒
     */
    public static AnimationTimer[] createTimers(int size, long duration) {
        return createTimers(size, duration, AnimationCurves.LINEAR);
    }

    /**
     * 创建多个动画计时器
     *
     * @param size           计时器数量
     * @param duration       动画持续时间，单位为毫秒
     * @param animationCurve 动画曲线函数
     */
    public static AnimationTimer[] createTimers(int size, long duration, Function<Double, Double> animationCurve) {
        return createTimers(size, duration, animationCurve, animationCurve);
    }

    /**
     * 创建多个动画计时器
     *
     * @param size                   计时器数量
     * @param duration               动画持续时间，单位为毫秒
     * @param forwardAnimationCurve  正向动画曲线函数
     * @param backwardAnimationCurve 反向动画曲线函数
     */
    public static AnimationTimer[] createTimers(int size, long duration, Function<Double, Double> forwardAnimationCurve, Function<Double, Double> backwardAnimationCurve) {
        return createTimers(size, duration, duration, forwardAnimationCurve, backwardAnimationCurve);
    }

    /**
     * 创建多个动画计时器
     *
     * @param size                   计时器数量
     * @param forwardDuration        正向动画持续时间，单位为毫秒
     * @param backwardDuration       反向动画持续时间，单位为毫秒
     * @param forwardAnimationCurve  正向动画曲线函数
     * @param backwardAnimationCurve 反向动画曲线函数
     */
    public static AnimationTimer[] createTimers(int size, long forwardDuration, long backwardDuration, Function<Double, Double> forwardAnimationCurve, Function<Double, Double> backwardAnimationCurve) {
        var timers = new AnimationTimer[size];
        var currentTime = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            timers[i] = new AnimationTimer(forwardDuration, backwardDuration).forwardAnimation(forwardAnimationCurve).backwardAnimation(backwardAnimationCurve);
            timers[i].endBackward(currentTime);
        }
        return timers;
    }

    /**
     * 当前计时方向是否为正向
     */
    public boolean isForward() {
        return isForward;
    }

    /**
     * 获取当前进度
     *
     * @return 进度值，范围在0到1之间
     */
    public float getProgress(long currentTime) {
        if (isForward) {
            return forwardAnimationCurve.apply(Mth.clamp(getElapsedTime(currentTime) / (double) forwardDuration, 0, 1)).floatValue();
        } else {
            return 1 - backwardAnimationCurve.apply(Mth.clamp(1 - getElapsedTime(currentTime) / (double) backwardDuration, 0, 1)).floatValue();
        }
    }

    private long getElapsedTime(long currentTime) {
        if (!initialized) return playFromStart ? 0 : (isForward() ? forwardDuration : backwardDuration);

        if (isForward) {
            return Math.min(forwardDuration, currentTime - startTime);
        } else {
            return Math.min(backwardDuration, Math.max(0, startTime - currentTime));
        }
    }

    /**
     * 当前动画是否已经结束
     */
    public boolean finished(long currentTime) {
        return getElapsedTime(currentTime) >= (isForward ? forwardDuration : backwardDuration);
    }

    /**
     * 将计时器设置为开始状态
     */
    public void begin() {
        initialized = false;
        playFromStart = true;
    }

    /**
     * 将计时器设置为结束状态
     */
    public void end() {
        initialized = false;
        playFromStart = false;
    }

    /**
     * 将计时方向更改为正向
     */
    public void forward(long currentTime) {
        if (!initialized) {
            initialized = true;
            startTime = currentTime + (playFromStart ? 0 : forwardDuration);
        } else if (!isForward) {
            startTime = (long) (currentTime - ((double) getElapsedTime(currentTime) / backwardDuration * forwardDuration));
        }
        isForward = true;
    }

    /**
     * 开始正向计时
     */
    public void beginForward(long currentTime) {
        begin();
        forward(currentTime);
    }

    /**
     * 结束正向计时
     */
    public void endForward(long currentTime) {
        end();
        forward(currentTime);
    }

    /**
     * 将计时方向更改为反向
     */
    public void backward(long currentTime) {
        if (!initialized) {
            initialized = true;
            startTime = currentTime + (playFromStart ? backwardDuration : 0);
        } else if (isForward) {
            startTime = (long) (currentTime + ((double) getElapsedTime(currentTime) / forwardDuration * backwardDuration));
        }
        isForward = false;
    }

    /**
     * 开始反向计时
     */
    public void beginBackward(long currentTime) {
        begin();
        backward(currentTime);
    }

    /**
     * 结束反向计时
     */
    public void endBackward(long currentTime) {
        end();
        backward(currentTime);
    }

    public float lerp(float start, float end, long currentTime) {
        return Mth.lerp(getProgress(currentTime), start, end);
    }
}
