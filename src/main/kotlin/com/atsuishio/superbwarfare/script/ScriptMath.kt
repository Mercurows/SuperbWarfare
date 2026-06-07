package com.atsuishio.superbwarfare.script

import org.joml.Quaternionf

object ScriptMath {
    const val DEG_TO_RAD: Double = Math.PI / 180.0

    const val RAD_TO_DEG: Double = 180.0 / Math.PI

    fun lerp(delta: Double, start: Double, end: Double): Double {
        return start + (end - start) * delta
    }

    fun clamp(value: Double, min: Double, max: Double): Double {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }

    @JvmField
    val Axis = AxisHolder

    object AxisHolder {
        @JvmField
        val XP = AxisRotation(1f, 0f, 0f)
        @JvmField
        val YP = AxisRotation(0f, 1f, 0f)
        @JvmField
        val ZP = AxisRotation(0f, 0f, 1f)
    }

    class AxisRotation(private val axisX: Float, private val axisY: Float, private val axisZ: Float) {
        fun rotation(angle: Double): Quaternionf {
            return Quaternionf().rotateAxis(angle.toFloat(), axisX, axisY, axisZ)
        }

        fun rotationDegrees(angle: Double): Quaternionf {
            return rotation(Math.toRadians(angle))
        }
    }
}
