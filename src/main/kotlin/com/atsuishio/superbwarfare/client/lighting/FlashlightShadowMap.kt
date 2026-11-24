package com.atsuishio.superbwarfare.client.lighting

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL30
import java.nio.FloatBuffer
import kotlin.math.*

/** 沿手电光轴采样方块深度，覆盖观察者屏幕外的遮挡物。 */
object FlashlightShadowMap {
    private const val SIZE = 32
    private const val UPDATES_PER_FRAME = 2
    private const val MAX_AGE = 4L
    private const val MIN_DIRECTION_DOT = 0.9994 // 约两度
    private const val MAX_OFFSET_SQUARED = 0.15 * 0.15
    private const val ANGLE_MARGIN = 0.0873 // 五度，留给缓存期间的光轴转动

    private class Map(
        val origin: Vec3,
        val axis: Vec3,
        val right: Vec3,
        val up: Vec3,
        val cone: Float,
        val halfAngle: Float,
        val range: Float,
        val tick: Long,
        val depths: FloatBuffer,
    )

    private val maps = mutableMapOf<String, Map>()
    private var world: ClientLevel? = null
    private var texture = 0
    private var nextLight = 0

    fun update(lights: List<GunMuzzleLightHandler.Light>) {
        val level = Minecraft.getInstance().level ?: return
        if (world !== level) {
            maps.clear()
            nextLight = 0
            world = level
        }
        val tick = level.gameTime
        // 保留短暂熄灭的爆闪灯采样，避免下一次点亮又从头计算。
        maps.values.removeIf { tick - it.tick !in 0..20 }
        val flashlights = lights.mapNotNull { light -> light.flashlight?.let { it.key to light } }
        var updates = 0
        repeat(flashlights.size) {
            val (key, light) = flashlights[nextLight % flashlights.size]
            nextLight = (nextLight + 1) % flashlights.size
            val map = maps[key]
            if (map != null && tick - map.tick in 0 until MAX_AGE &&
                map.origin.distanceToSqr(light.position) <= MAX_OFFSET_SQUARED &&
                map.axis.dot(light.direction) >= MIN_DIRECTION_DOT &&
                map.range == light.radius && map.cone == light.coneCosOuter
            ) return@repeat

            maps[key] = sample(light, tick, map?.depths ?: BufferUtils.createFloatBuffer(SIZE * SIZE))
            updates++
            if (updates >= UPDATES_PER_FRAME) return
        }
    }

    private fun sample(light: GunMuzzleLightHandler.Light, tick: Long, depths: FloatBuffer): Map {
        val axis = light.direction
        val reference = if (abs(axis.y) < 0.99) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)
        val right = reference.cross(axis).normalize()
        val up = axis.cross(right)
        val halfAngle = acos(light.coneCosOuter.toDouble()) + ANGLE_MARGIN
        val level = light.holder.level()
        val range = light.radius.toDouble()
        val origin = light.position
        depths.clear()
        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                val u = ((x + 0.5) * 2.0 / SIZE - 1.0) * halfAngle
                val v = ((y + 0.5) * 2.0 / SIZE - 1.0) * halfAngle
                val angle = sqrt(u * u + v * v)
                if (angle > halfAngle + halfAngle * 2.0 / SIZE) {
                    depths.put(light.radius)
                    continue
                }
                val tangent = sin(angle) / angle
                val direction = axis.scale(cos(angle)).add(right.scale(u * tangent)).add(up.scale(v * tangent))
                val hit = level.clip(
                    ClipContext(origin, origin.add(direction.scale(range)),
                        ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, light.holder)
                )
                val distance = if (hit.type == HitResult.Type.MISS) range else origin.distanceTo(hit.location)
                depths.put(distance.toFloat())
            }
        }
        depths.flip()
        return Map(origin, axis, right, up, light.coneCosOuter, halfAngle.toFloat(), light.radius, tick, depths)
    }

    fun bind(shader: ShaderInstance, light: GunMuzzleLightHandler.Light, camera: Vec3): Boolean {
        val emitter = light.flashlight
        if (emitter == null) {
            shader.safeGetUniform("BlockShadowAngle").set(0f)
            return true
        }
        val map = maps[emitter.key]
        if (map == null || map.origin.distanceToSqr(light.position) > 0.5 * 0.5 ||
            map.axis.dot(light.direction) < cos(ANGLE_MARGIN) ||
            map.range != light.radius || map.cone != light.coneCosOuter
        ) return false

        RenderSystem.activeTexture(GL13.GL_TEXTURE3)
        if (texture == 0) {
            texture = GlStateManager._genTexture()
            GlStateManager._bindTexture(texture)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R32F, SIZE, SIZE, 0, GL11.GL_RED, GL11.GL_FLOAT, map.depths)
        } else {
            GlStateManager._bindTexture(texture)
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, SIZE, SIZE, GL11.GL_RED, GL11.GL_FLOAT, map.depths)
        }
        shader.setSampler("BlockShadowDepth", texture)
        shader.safeGetUniform("BlockShadowAngle").set(map.halfAngle)
        val origin = map.origin.subtract(camera)
        shader.safeGetUniform("BlockShadowOrigin").set(origin.x.toFloat(), origin.y.toFloat(), origin.z.toFloat())
        shader.safeGetUniform("BlockShadowAxis").set(map.axis.x.toFloat(), map.axis.y.toFloat(), map.axis.z.toFloat())
        shader.safeGetUniform("BlockShadowRight").set(map.right.x.toFloat(), map.right.y.toFloat(), map.right.z.toFloat())
        shader.safeGetUniform("BlockShadowUp").set(map.up.x.toFloat(), map.up.y.toFloat(), map.up.z.toFloat())
        return true
    }

    fun close() {
        maps.clear()
        world = null
        nextLight = 0
        if (texture != 0) GlStateManager._deleteTexture(texture)
        texture = 0
    }
}
