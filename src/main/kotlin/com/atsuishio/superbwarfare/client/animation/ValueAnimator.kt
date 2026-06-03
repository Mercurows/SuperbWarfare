package com.atsuishio.superbwarfare.client.animation

/**
 * 可以存储额外的新旧数值的AnimationTimer
 */
open class ValueAnimator<T>(duration: Long, defaultValue: T) : AnimationTimer(duration) {
    private var oldValue: T = defaultValue
    private var newValue: T = defaultValue

    fun update(value: T) {
        oldValue = newValue
        newValue = value
    }

    /**
     * 比较当前值和新值，如果不同则更新
     *
     * @param value 当前值
     */
    fun compareAndUpdate(value: T) {
        compareAndUpdate(value, null)
    }

    /**
     * 比较当前值和新值，如果不同则更新
     *
     * @param value    当前值
     * @param callback 更新成功后的回调函数
     */
    fun compareAndUpdate(value: T, callback: Runnable?) {
        if (newValue != value) {
            update(value)
            callback?.run()
        }
    }

    fun reset(value: T) {
        oldValue = value
        newValue = value
    }

    fun oldValue(): T = oldValue

    fun newValue(): T = newValue

    companion object {
        @JvmStatic
        fun <T> create(size: Int, duration: Long, defaultValue: T): Array<ValueAnimator<T>> {
            @Suppress("UNCHECKED_CAST")
            return Array(size) { ValueAnimator(duration, defaultValue) }
        }
    }
}
