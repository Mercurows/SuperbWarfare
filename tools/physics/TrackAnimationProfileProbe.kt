import com.atsuishio.superbwarfare.client.model.entity.TrackAnimationProfile
import com.atsuishio.superbwarfare.physics.TrackSuspensionProfile
import kotlin.math.abs

/** Run with the production TrackAnimationProfile source, without Minecraft. */
fun main() {
    var calls = 0
    val profile = TrackAnimationProfile(80,
        { calls++; if (it < 79.99f) -it * 4.5f else 0f },
        { calls++; it * .5f }, { calls++; it * 2f })
    val preparedCalls = calls
    repeat(1000) {
        val phase = it * .071f
        check(abs(profile.height(phase) - phase * .5f) < .0001f)
        check(abs(profile.length(phase) - phase * 2f) < .0001f)
        check(abs(profile.height(phase) - profile.height(phase + 80f)) < .0001f)
        check(abs(profile.length(phase) - profile.length(phase - 80f)) < .0001f)
    }
    check(abs(profile.rotation(79.999f) + 360f) < .1f) { "rotation jumps backwards at the authored -360/0 seam" }
    check(calls == preparedCalls) { "runtime evaluated authored trajectory instead of reading preprocessed values" }
    for (scale in listOf(0f, .25f, .8f, 1f)) for (limit in listOf(.02f, .06f, .25f)) {
        val heights = TrackSuspensionProfile(.18f, .24f, scale, limit)
        repeat(1001) {
            val physical = -.4f + it * .0008f
            val expected = (physical.coerceIn(-.24f, .18f) * scale).coerceIn(-limit, limit)
            check(abs(heights.height(physical) - expected) < .000001f)
        }
    }
    println("TrackAnimationProfile: 80-step cycle, forward/reverse wrap, interpolation, angle seam and no runtime curve evaluation OK")
    println("TrackSuspensionProfile: asymmetric travel, preprocessed clamp breakpoints, zero scale and visual height limits OK")
}
