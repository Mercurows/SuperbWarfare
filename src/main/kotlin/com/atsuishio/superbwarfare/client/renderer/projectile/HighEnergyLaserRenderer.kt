package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.entity.projectile.HighEnergyLaserEntity
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Brilliant core, parallel energy rails and a soft circular muzzle glow. */
class HighEnergyLaserRenderer(context: EntityRendererProvider.Context) : LaserBeamRenderer<HighEnergyLaserEntity>(context) {
    override fun renderEffects(entity: HighEnergyLaserEntity, buffer: VertexConsumer, beam: BeamFrame) {
        with(beam) {
            val r = color.x.toFloat()
            val g = color.y.toFloat()
            val b = color.z.toFloat()
            // Long parallel bands give the beam the flat, high-energy silhouette of the reference.
            for (rail in 0..5) {
                val angle = rail * PI / 3 + age * 0.035
                val offset = right.scale(cos(angle) * radius * 1.7).add(up.scale(sin(angle) * radius * 1.7))
                ribbon(buffer, matrix, offset, offset.add(direction.scale(length)), radius * 0.09, r, g, b, fade * 0.8f)
            }
            // Fixed budgets: the effect does not create a particle/emitter for each block of range.
            val frames = 12
            for (i in 0 until frames) {
                val t = (i + 0.5 + (age * 0.025) % 0.5) / frames
                val distance = t * length
                square(buffer, matrix, direction.scale(distance), right, up,
                    radius * (1.7 + sin(t * PI) * 0.5), age * 0.11 + i * PI / 6,
                    radius * 0.055, fade * 0.55f, color)
            }
            muzzleGlow(buffer, beam)
            if (entity.hitBlock) {
                val end = direction.scale((length - 0.03).coerceAtLeast(0.0))
                square(buffer, matrix, end, right, up, radius * (2.2 + progress * 3), age * 0.2,
                    radius * 0.1, fade, color)
            }
        }
    }

    private fun muzzleGlow(buffer: VertexConsumer, beam: BeamFrame) {
        with(beam) {
            // Camera-facing concentric bands blend from a white center into a transparent colored edge.
            val center = direction.scale(minOf(0.04, length * 0.5))
            val size = radius * (3.2 + sin(age * 0.35) * 0.15)
            val radii = doubleArrayOf(0.0, 0.25, 0.55, 1.0)
            val alphas = floatArrayOf(1f, 0.85f, 0.3f, 0f)
            val whites = doubleArrayOf(0.95, 0.8, 0.1, 0.0)
            fun point(ring: Int, angle: Double) = center
                .add(cameraRight.scale(cos(angle) * size * radii[ring]))
                .add(cameraUp.scale(sin(angle) * size * radii[ring]))
            fun emit(ring: Int, angle: Double) {
                val white = whites[ring]
                vertex(buffer, matrix, point(ring, angle),
                    (color.x + (1 - color.x) * white).toFloat(),
                    (color.y + (1 - color.y) * white).toFloat(),
                    (color.z + (1 - color.z) * white).toFloat(), alphas[ring] * fade)
            }
            for (ring in 0 until radii.lastIndex) {
                for (segment in 0 until 32) {
                    val angle = segment * PI / 16
                    val next = (segment + 1) * PI / 16
                    emit(ring, angle)
                    emit(ring + 1, angle)
                    emit(ring + 1, next)
                    emit(ring, next)
                }
            }
        }
    }
}
