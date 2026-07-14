package com.atsuishio.superbwarfare.client.sound

/** Pure mixer math kept separate from Minecraft sound instances for fast regression tests. */
object VehicleEngineSoundMix {
    private const val DRIVE_RISE_PER_TICK = 0.22f
    private const val DRIVE_FALL_PER_TICK = 0.14f
    private const val GROUND_DISTANT_SCALE = 0.5f
    private const val ROTOR_DISTANT_SCALE = 0.25f
    private const val TURBINE_DISTANT_SCALE = 0.23f
    private const val AIRCRAFT_DISTANT_SCALE = 0.23f
    private const val MAX_DISTANT_VOLUME = 0.6f

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

    @JvmStatic
    fun groundDistantGain(load: Float): Float =
        (0.9f + 0.6f * load.coerceIn(0f, 1f)) * GROUND_DISTANT_SCALE

    @JvmStatic
    fun rotorDistantGain(): Float = ROTOR_DISTANT_SCALE

    @JvmStatic
    fun turbineDistantGain(): Float = TURBINE_DISTANT_SCALE

    @JvmStatic
    fun aircraftDistantGain(load: Float): Float =
        (0.8f + 0.9f * load.coerceIn(0f, 1f)) * AIRCRAFT_DISTANT_SCALE

    /** Leaves headroom below Minecraft's 1.0 per-source limiter for future data-driven profiles. */
    @JvmStatic
    fun distantVolume(baseVolume: Float, gain: Float): Float =
        (baseVolume.coerceAtLeast(0f) * gain.coerceAtLeast(0f)).coerceAtMost(MAX_DISTANT_VOLUME)

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
