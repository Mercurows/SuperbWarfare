package com.atsuishio.superbwarfare.tools

/**
 * 把服务端 tick 的真实墙钟耗时折算成本 tick 应推进的伪 tick 数
 */
object ActionTickClock {
    private const val MS_PER_TICK = 50L
    private const val MAX_DELTA_MS = 1000L

    private var lastTickMs = -1L
    private var carryMs = 0L

    var budget: Int = 1
        private set

    /** 每个服务端 tick 在实体 tick 之前推进一次 */
    fun advance() {
        val now = System.currentTimeMillis()

        if (lastTickMs < 0) {
            lastTickMs = now
            budget = 1
            return
        }

        val delta = (now - lastTickMs).coerceIn(0L, MAX_DELTA_MS)
        lastTickMs = now
        carryMs += delta

        val ticks = (carryMs / MS_PER_TICK).toInt()
        if (ticks < 1) {
            budget = 1
            carryMs = 0
        } else {
            carryMs -= ticks * MS_PER_TICK
            budget = ticks.coerceAtMost((MAX_DELTA_MS / MS_PER_TICK).toInt())
        }
    }
}
