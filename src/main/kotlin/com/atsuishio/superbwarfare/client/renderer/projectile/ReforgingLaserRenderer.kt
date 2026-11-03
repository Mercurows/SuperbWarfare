package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.entity.projectile.LaserBeamEntity
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Reforging's five focusing frames, crimson coils and impact seal; beam rendering is shared. */
class ReforgingLaserRenderer(context: EntityRendererProvider.Context) : LaserBeamRenderer<LaserBeamEntity>(context) {
    override fun renderEffects(entity: LaserBeamEntity, buffer: VertexConsumer, beam: BeamFrame) {
        with(beam) {
            for (frame in 0 until 5) {
                val distance = 0.12 + frame * 0.48
                if (distance >= length) break
                val size = (0.68 - frame * 0.105) * (0.55 + entity.power * 0.45)
                square(buffer, matrix, direction.scale(distance), right, up, size,
                    frame * PI / 7 + age * 0.12, radius * 0.13, fade, color)
            }

            val coilLength = minOf(10.0, length)
            for (coil in 0..1) {
                var previous: Vec3? = null
                for (segment in 0..28) {
                    val t = segment / 28.0
                    val angle = t * PI * 5 + coil * PI - age * 0.3
                    val size = radius * (1.6 + 0.8 * (1 - t))
                    val point = direction.scale(coilLength * t)
                        .add(right.scale(cos(angle) * size)).add(up.scale(sin(angle) * size))
                    previous?.let { ribbon(buffer, matrix, it, point, radius * 0.06,
                        color.x.toFloat(), color.y.toFloat(), color.z.toFloat(), fade * 0.7f) }
                    previous = point
                }
            }
            if (entity.hitBlock) {
                val center = direction.scale((length - 0.025).coerceAtLeast(0.0))
                for (ring in 0..1) {
                    square(buffer, matrix, center, right, up,
                        radius * (2.2 + progress * 5 + ring), age * 0.16 + ring * PI / 4,
                        radius * 0.12, fade * (1f - ring * 0.3f), color)
                }
            }
        }
    }
}
