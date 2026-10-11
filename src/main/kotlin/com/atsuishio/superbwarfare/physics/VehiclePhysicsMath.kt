package com.atsuishio.superbwarfare.physics

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf

internal fun Vec3.bullet() = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
internal fun Vector3f.minecraft() = Vec3(x.toDouble(), y.toDouble(), z.toDouble())
internal fun Quaternionf.bullet() = Quaternion(x, y, z, w)
internal fun Quaternion.joml() = Quaternionf(x, y, z, w)
internal fun Quaternionf.rotate(v: Vec3): Vec3 {
    val out = transform(org.joml.Vector3f(v.x.toFloat(), v.y.toFloat(), v.z.toFloat()))
    return Vec3(out.x.toDouble(), out.y.toDouble(), out.z.toDouble())
}
internal fun Vec3.finite() = x.isFinite() && y.isFinite() && z.isFinite()
internal fun Vec3.limited(max: Double): Vec3 = if (lengthSqr() > max * max) normalize().scale(max) else this

/** Minecraft velocity is blocks/tick; the native world uses metres/second. */
object VehiclePhysicsUnits {
    const val TICKS_PER_SECOND = 20.0
    fun massKg(tonnes: Float) = if (tonnes.isFinite()) (tonnes * 1000f).coerceIn(0.1f, 1e8f) else 1000f
    fun velocity(motion: Vec3): Vec3 = motion.scale(TICKS_PER_SECOND)
    fun motion(velocity: Vec3): Vec3 = velocity.scale(1.0 / TICKS_PER_SECOND)
    fun acceleration(deltaMotion: Vec3): Vec3 = deltaMotion.scale(TICKS_PER_SECOND * TICKS_PER_SECOND)
}
