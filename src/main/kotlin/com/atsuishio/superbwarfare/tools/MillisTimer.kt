package com.atsuishio.superbwarfare.tools

open class MillisTimer {
    var startTime: Long = 0
    private var started = false

    fun start() {
        if (!started) {
            started = true
            startTime = System.currentTimeMillis()
        }
    }

    fun started(): Boolean {
        return started
    }

    fun stop() {
        started = false
    }

    var progress: Long
        get() {
            if (!started) {
                return 0
            }
            return System.currentTimeMillis() - startTime
        }
        set(progress) {
            startTime = System.currentTimeMillis() - progress
        }
}
