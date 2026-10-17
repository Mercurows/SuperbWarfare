package com.atsuishio.superbwarfare.client.model.entity

import kotlin.math.ceil

/** Preprocessed authored belt trajectory; no wheel wrapping/chain solver.
 * Prepared lazily per renderer/model and cycle length, after construction, so
 * vehicle-specific getBone* overrides remain the source of truth.
 */
class TrackAnimationProfile(
    val cycle: Int,
    rotation: (Float) -> Float,
    height: (Float) -> Float,
    length: (Float) -> Float,
) {
    private val count = ceil(cycle * 128.0).toInt().coerceAtLeast(2)
    private val rotations = FloatArray(count + 1) { rotation(it.toFloat() * cycle / count) }
    private val heights = FloatArray(count + 1) { height(it.toFloat() * cycle / count) }
    private val lengths = FloatArray(count + 1) { length(it.toFloat() * cycle / count) }

    init {
        require(cycle > 0)
        // Authored curves often return 0 after -360 at the loop boundary.
        // Unwrap before interpolation: do not rotate an entire turn backwards
        // within a single lookup-table cell.
        for (i in 1..count) {
            val delta = ((rotations[i] - rotations[i - 1] + 180f) % 360f + 360f) % 360f - 180f
            rotations[i] = rotations[i - 1] + delta
        }
    }

    private fun sample(values: FloatArray, phase: Float): Float {
        val wrapped = ((phase % cycle) + cycle) % cycle
        val position = wrapped / cycle * count
        val index = position.toInt().coerceIn(0, count - 1)
        val t = position - index
        return values[index] + (values[index + 1] - values[index]) * t
    }

    fun rotation(phase: Float) = sample(rotations, phase)
    fun height(phase: Float) = sample(heights, phase)
    fun length(phase: Float) = sample(lengths, phase)
}
