package com.atsuishio.superbwarfare.client.animation;

import javax.annotation.Nullable;

/**
 * 可以存储额外的新旧数值的AnimationTimer
 */
public class ValueAnimator<T> extends AnimationTimer {
    private T oldValue;
    private T newValue;

    public ValueAnimator(long duration, T defaultValue) {
        super(duration);
        reset(defaultValue);
    }

    public static <T> ValueAnimator<T>[] create(int size, long duration, T defaultValue) {
        // 傻逼Java
        @SuppressWarnings("unchecked")
        ValueAnimator<T>[] animators = (ValueAnimator<T>[]) new ValueAnimator[size];

        for (int i = 0; i < size; i++) {
            animators[i] = new ValueAnimator<>(duration, defaultValue);
        }

        return animators;
    }

    public void update(T value) {
        this.oldValue = this.newValue;
        this.newValue = value;
    }

    /**
     * 比较当前值和新值，如果不同则更新
     *
     * @param value 当前值
     */
    public void compareAndUpdate(T value) {
        compareAndUpdate(value, null);
    }


    /**
     * 比较当前值和新值，如果不同则更新
     *
     * @param value    当前值
     * @param callback 更新成功后的回调函数
     */
    public void compareAndUpdate(T value, @Nullable Runnable callback) {
        if (!this.newValue.equals(value)) {
            update(value);

            if (callback != null) {
                callback.run();
            }
        }
    }

    public void reset(T value) {
        this.oldValue = value;
        this.newValue = value;
    }

    public T oldValue() {
        return this.oldValue;
    }

    public T newValue() {
        return this.newValue;
    }
}
