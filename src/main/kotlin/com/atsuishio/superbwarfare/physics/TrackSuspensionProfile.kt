package com.atsuishio.superbwarfare.physics

/** Captured once per vehicle definition, never derived from individual wheel hits.
 * The sampled interval maps the whole side's physical travel to its authored
 * visual height range. Both renderers consume this same server projection.
 */
internal class TrackSuspensionProfile(travelUp: Float, travelDown: Float, visualScale: Float, visualMaxOffset: Float) {
    val minimum = -travelDown.coerceAtLeast(.001f)
    val maximum = travelUp.coerceAtLeast(.001f)
    private val scale = visualScale.coerceIn(0f, 1f)
    private val limit = visualMaxOffset.coerceIn(.005f, 1f)
    // Include the clamp breakpoints so interpolation is exact, not an
    // approximation which rounds away a narrow authored suspension interval.
    private val travel = (listOf(minimum, 0f, maximum) + if (scale > 0f)
        listOf((-limit / scale).coerceIn(minimum, maximum), (limit / scale).coerceIn(minimum, maximum))
        else emptyList()).distinct().sorted()
    private val height = travel.map { (it * scale).coerceIn(-limit, limit) }

    fun height(travelM: Float): Float {
        val value = travelM.coerceIn(minimum, maximum)
        val end = travel.indexOfFirst { it >= value }.coerceAtLeast(0)
        if (end == 0) return height[0]
        val t = (value - travel[end - 1]) / (travel[end] - travel[end - 1])
        return height[end - 1] + (height[end] - height[end - 1]) * t
    }
}
