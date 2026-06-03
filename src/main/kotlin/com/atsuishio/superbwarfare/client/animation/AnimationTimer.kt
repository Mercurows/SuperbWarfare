package com.atsuishio.superbwarfare.client.animation

import net.minecraft.util.Mth
import java.util.function.Function

/**
 * 可以更改计时方向的动画计时器
 */
open class AnimationTimer(
    private val forwardDuration: Long,
    private val backwardDuration: Long = forwardDuration
) {
    private var startTime: Long = 0
    private var forwardDirection: Boolean = true
    private var initialized: Boolean = false

    // 未初始化状态下，动画进度是否从0开始
    private var playFromStart: Boolean = false

    var forwardAnimationCurve: Function<Double, Double> = AnimationCurves.LINEAR
        private set
    var backwardAnimationCurve: Function<Double, Double> = AnimationCurves.LINEAR
        private set

    /**
     * 设置正向和反向计时时采用的动画曲线
     */
    fun animation(animationCurve: Function<Double, Double>) = apply {
        forwardAnimationCurve = animationCurve
        backwardAnimationCurve = animationCurve
    }

    /**
     * 设置正向计时时采用的动画曲线
     */
    fun forwardAnimation(animationCurve: Function<Double, Double>) = apply {
        forwardAnimationCurve = animationCurve
    }

    /**
     * 设置反向计时时采用的动画曲线
     */
    fun backwardAnimation(animationCurve: Function<Double, Double>) = apply {
        backwardAnimationCurve = animationCurve
    }

    /**
     * 当前计时方向是否为正向
     */
    val isForward: Boolean get() = forwardDirection

    /**
     * 获取当前进度
     *
     * @return 进度值，范围在0到1之间
     */
    fun getProgress(currentTime: Long): Float {
        return if (forwardDirection) {
            forwardAnimationCurve.apply(
                Mth.clamp(getElapsedTime(currentTime) / forwardDuration.toDouble(), 0.0, 1.0)
            ).toFloat()
        } else {
            1 - backwardAnimationCurve.apply(
                Mth.clamp(1 - getElapsedTime(currentTime) / backwardDuration.toDouble(), 0.0, 1.0)
            ).toFloat()
        }
    }

    private fun getElapsedTime(currentTime: Long): Long {
        if (!initialized) return if (playFromStart) 0 else if (forwardDirection) forwardDuration else backwardDuration

        return if (forwardDirection) {
            minOf(forwardDuration, currentTime - startTime)
        } else {
            minOf(backwardDuration, maxOf(0, startTime - currentTime))
        }
    }

    /**
     * 当前动画是否已经结束
     */
    fun finished(currentTime: Long): Boolean {
        return getElapsedTime(currentTime) >= if (forwardDirection) forwardDuration else backwardDuration
    }

    /**
     * 将计时器设置为开始状态
     */
    fun begin() {
        initialized = false
        playFromStart = true
    }

    /**
     * 将计时器设置为结束状态
     */
    fun end() {
        initialized = false
        playFromStart = false
    }

    /**
     * 将计时方向更改为正向
     */
    fun forward(currentTime: Long) {
        if (!initialized) {
            initialized = true
            startTime = currentTime + if (playFromStart) 0 else forwardDuration
        } else if (!forwardDirection) {
            startTime =
                (currentTime - getElapsedTime(currentTime).toDouble() / backwardDuration * forwardDuration).toLong()
        }
        forwardDirection = true
    }

    /**
     * 开始正向计时
     */
    fun beginForward(currentTime: Long) {
        begin()
        forward(currentTime)
    }

    /**
     * 结束正向计时
     */
    fun endForward(currentTime: Long) {
        end()
        forward(currentTime)
    }

    /**
     * 将计时方向更改为反向
     */
    fun backward(currentTime: Long) {
        if (!initialized) {
            initialized = true
            startTime = currentTime + if (playFromStart) backwardDuration else 0
        } else if (forwardDirection) {
            startTime =
                (currentTime + getElapsedTime(currentTime).toDouble() / forwardDuration * backwardDuration).toLong()
        }
        forwardDirection = false
    }

    /**
     * 开始反向计时
     */
    fun beginBackward(currentTime: Long) {
        begin()
        backward(currentTime)
    }

    /**
     * 结束反向计时
     */
    fun endBackward(currentTime: Long) {
        end()
        backward(currentTime)
    }

    fun lerp(start: Float, end: Float, currentTime: Long): Float {
        return Mth.lerp(getProgress(currentTime), start, end)
    }

    companion object {
        /**
         * 创建多个线性动画计时器
         *
         * @param size     计时器数量
         * @param duration 动画持续时间，单位为毫秒
         */
        @JvmStatic
        fun createTimers(size: Int, duration: Long): Array<AnimationTimer> {
            return createTimers(size, duration, AnimationCurves.LINEAR)
        }

        /**
         * 创建多个动画计时器
         *
         * @param size           计时器数量
         * @param duration       动画持续时间，单位为毫秒
         * @param animationCurve 动画曲线函数
         */
        @JvmStatic
        fun createTimers(size: Int, duration: Long, animationCurve: Function<Double, Double>): Array<AnimationTimer> {
            return createTimers(size, duration, animationCurve, animationCurve)
        }

        /**
         * 创建多个动画计时器
         *
         * @param size                   计时器数量
         * @param duration               动画持续时间，单位为毫秒
         * @param forwardAnimationCurve  正向动画曲线函数
         * @param backwardAnimationCurve 反向动画曲线函数
         */
        @JvmStatic
        fun createTimers(
            size: Int,
            duration: Long,
            forwardAnimationCurve: Function<Double, Double>,
            backwardAnimationCurve: Function<Double, Double>
        ): Array<AnimationTimer> {
            return createTimers(size, duration, duration, forwardAnimationCurve, backwardAnimationCurve)
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
        @JvmStatic
        fun createTimers(
            size: Int,
            forwardDuration: Long,
            backwardDuration: Long,
            forwardAnimationCurve: Function<Double, Double>,
            backwardAnimationCurve: Function<Double, Double>
        ): Array<AnimationTimer> {
            val timers = arrayOfNulls<AnimationTimer>(size)
            val currentTime = System.currentTimeMillis()
            for (i in 0 until size) {
                timers[i] = AnimationTimer(forwardDuration, backwardDuration)
                    .forwardAnimation(forwardAnimationCurve)
                    .backwardAnimation(backwardAnimationCurve)
                timers[i]!!.endBackward(currentTime)
            }
            @Suppress("UNCHECKED_CAST")
            return timers as Array<AnimationTimer>
        }
    }
}
