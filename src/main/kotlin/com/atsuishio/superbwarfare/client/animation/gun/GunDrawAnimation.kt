package com.atsuishio.superbwarfare.client.animation.gun

import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.ln

object GunDrawAnimation {
    const val LENGTH = 0.7f

    fun timeAt(drawTime: Double): Float {
        if (drawTime <= 0.0) return LENGTH
        val progress = (-ln(drawTime) / LN_100).coerceIn(0.0, 1.0)
        return (progress * LENGTH).toFloat()
    }
    fun positionAt(t: Float, dest: Vector3f): Vector3f = sample(POSITION_KEYS, t, dest)
    fun rotationAt(t: Float, dest: Vector3f): Vector3f {
        sample(ROTATION_KEYS, t, dest)
        return dest.set(-dest.x * DEG_TO_RAD, -dest.y * DEG_TO_RAD, dest.z * DEG_TO_RAD)
    }

    private val ROTATION_KEYS = floatArrayOf(
        0.0f, 0.81f, -32.64f, 33.95f,
        0.0333f, 0.41f, -32.66f, 32.03f,
        0.0667f, -0.2f, -32.9f, 29.28f,
        0.1f, -0.85f, -32.88f, 26.12f,
        0.1333f, -1.39f, -32.15f, 22.92f,
        0.1667f, -1.65f, -30.23f, 20.1f,
        0.2f, -1.54f, -26.5f, 17.61f,
        0.2333f, -1.17f, -21.25f, 15.2f,
        0.2667f, -0.68f, -15.46f, 12.92f,
        0.3f, -0.21f, -10.08f, 10.79f,
        0.3333f, 0.1f, -6.07f, 8.88f,
        0.3667f, 0.23f, -3.2f, 6.78f,
        0.4f, 0.22f, -1.67f, 4.97f,
        0.4333f, 0.16f, -0.89f, 3.43f,
        0.4667f, 0.11f, -0.29f, 2.12f,
        0.5f, 0.05f, -0.02f, 0.15f,
        0.5333f, -0.01f, -0.43f, -0.76f,
        0.5667f, -0.02f, -0.32f, -0.02f,
        0.6f, -0.01f, -0.17f, 0.96f,
        0.6333f, -0.01f, -0.1f, 0.8f,
        0.6667f, 0f, -0.04f, 0.35f,
        0.7f, 0f, 0f, 0f,
    )

    private val POSITION_KEYS = floatArrayOf(
        0.0f, -6.18f, -5.58f, 13.53f,
        0.0333f, -5.08f, -4.98f, 11.4f,
        0.0667f, -4.08f, -4.11f, 8.31f,
        0.1f, -2.91f, -3.19f, 5.12f,
        0.1333f, -1.9f, -2.45f, 2.7f,
        0.1667f, -1.11f, -1.94f, 1.29f,
        0.2f, -0.7f, -1.53f, 0.43f,
        0.2333f, -0.44f, -1.21f, -0.1f,
        0.2667f, -0.25f, -0.94f, -0.49f,
        0.3f, -0.14f, -0.69f, -0.75f,
        0.3333f, -0.07f, -0.55f, -0.66f,
        0.3667f, -0.03f, -0.45f, -0.5f,
        0.4f, -0.01f, -0.38f, -0.3f,
        0.4333f, -0.02f, -0.34f, -0.02f,
        0.4667f, -0.03f, -0.3f, 0.16f,
        0.5f, -0.02f, -0.21f, 0.14f,
        0.5333f, -0.02f, -0.12f, 0.02f,
        0.5667f, -0.01f, -0.05f, -0.06f,
        0.6f, -0.01f, -0.02f, -0.06f,
        0.6333f, 0f, -0.01f, -0.04f,
        0.6667f, 0f, 0f, -0.02f,
        0.7f, 0f, 0f, 0f,
    )

    private const val STRIDE = 4
    private val DEG_TO_RAD = (PI / 180.0).toFloat()
    private val LN_100 = ln(100.0)

    private fun sample(keys: FloatArray, t: Float, dest: Vector3f): Vector3f {
        val last = keys.size - STRIDE
        if (t <= keys[0]) return dest.set(keys[1], keys[2], keys[3])
        if (t >= keys[last]) return dest.set(keys[last + 1], keys[last + 2], keys[last + 3])

        var i = 0
        while (keys[(i + 1) * STRIDE] < t) i++
        val a = i * STRIDE
        val b = a + STRIDE
        val p = if (i == 0) a else a - STRIDE
        val n = if (b == last) b else b + STRIDE
        val f = (t - keys[a]) / (keys[b] - keys[a])
        return dest.set(
            spline(keys[p + 1], keys[a + 1], keys[b + 1], keys[n + 1], f),
            spline(keys[p + 2], keys[a + 2], keys[b + 2], keys[n + 2], f),
            spline(keys[p + 3], keys[a + 3], keys[b + 3], keys[n + 3], f),
        )
    }

    private fun spline(p: Float, x: Float, y: Float, n: Float, f: Float): Float {
        val v0 = (y - p) * 0.5f
        val v1 = (n - x) * 0.5f
        val f2 = f * f
        val f3 = f * f2
        return (2f * f3 - 3f * f2 + 1f) * x +
                (3f * f2 - 2f * f3) * y +
                (f3 - 2f * f2 + f) * v0 +
                (f3 - f2) * v1
    }
}
