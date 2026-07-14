package com.atsuishio.superbwarfare.client.sound

/** Pure mixer math kept separate from Minecraft sound instances for fast regression tests. */
object VehicleEngineSoundMix {
    private const val DRIVE_RISE_PER_TICK = 0.22f
    private const val DRIVE_FALL_PER_TICK = 0.14f

    data class GroundMix(val idle: Float, val drive: Float, val release: Float)

    /**
     * Engine power decays over several seconds after releasing a movement key. The audible
     * idle/drive transition must not inherit that delay, so it follows a short independent ramp.
     */
    @JvmStatic
    fun nextDriveMix(current: Float, accelerating: Boolean): Float {
        val target = if (accelerating) 1f else 0f
        val step = if (accelerating) DRIVE_RISE_PER_TICK else DRIVE_FALL_PER_TICK
        return moveTowards(current.coerceIn(0f, 1f), target, step)
    }

    @JvmStatic
    fun groundMix(driveMix: Float, accelerating: Boolean): GroundMix {
        val mix = driveMix.coerceIn(0f, 1f)
        return if (accelerating) {
            GroundMix(idle = 1f - mix, drive = mix, release = 0f)
        } else {
            GroundMix(idle = 1f - mix, drive = 0f, release = mix)
        }
    }

    /** Crossfade close and distant recordings between 20 and 56 blocks. */
    @JvmStatic
    fun distantBlend(distance: Float): Float = smoothstep(20f, 56f, distance)

    /** Keeps an engine audible when the constrained streaming pool rejects its close layer. */
    @JvmStatic
    fun missingCloseFallback(internal: Float, externalClose: Float, uncoveredMix: Float): Float =
        (0.55f * internal + 0.8f * externalClose) * uncoveredMix.coerceIn(0f, 1f)

    private fun moveTowards(current: Float, target: Float, step: Float): Float = when {
        current < target -> (current + step).coerceAtMost(target)
        current > target -> (current - step).coerceAtLeast(target)
        else -> target
    }

    private fun smoothstep(edge0: Float, edge1: Float, value: Float): Float {
        val x = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }
}
