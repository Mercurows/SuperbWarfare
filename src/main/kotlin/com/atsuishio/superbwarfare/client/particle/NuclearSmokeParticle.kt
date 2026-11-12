package com.atsuishio.superbwarfare.client.particle

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.renderer.ModParticleRenderTypes
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Camera
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.TextureSheetParticle
import kotlin.math.pow

/** Reuses the existing four-frame muzzle smoke texture and soft-alpha render pipeline. */
class NuclearSmokeParticle(
    level: ClientLevel, x: Double, y: Double, z: Double,
    vx: Double, vy: Double, vz: Double,
    size: Float, life: Int, dust: Boolean
) : TextureSheetParticle(level, x, y, z) {
    private var frame = 0
    private val spin = (random.nextFloat() - 0.5f) * 0.014f
    private val baseSize = size
    private val opacity = if (dust) 0.24f else 0.19f

    init {
        xd = vx
        yd = vy
        zd = vz
        lifetime = life
        gravity = if (dust) 0.006f else -0.004f
        friction = 0.965f
        hasPhysics = dust
        setSize(0.35f, 0.35f)
        quadSize = size
        roll = random.nextFloat() * (Math.PI * 2).toFloat()
        oRoll = roll
        alpha = 0f
        if (dust) setColor(0.51f, 0.40f, 0.28f) else setColor(0.56f, 0.53f, 0.49f)
    }

    override fun getRenderType() = SMOKE_RENDER_TYPE
    override fun getU0() = frame / 4f
    override fun getU1() = (frame + 1) / 4f
    override fun getV0() = 0f
    override fun getV1() = 1f

    override fun getQuadSize(partialTicks: Float): Float {
        val progress = ((age + partialTicks) / lifetime).coerceIn(0f, 1f)
        return baseSize * (0.65f + 0.75f * progress)
    }

    override fun tick() {
        super.tick()
        oRoll = roll
        roll += spin
    }

    override fun render(buffer: VertexConsumer, camera: Camera, partialTicks: Float) {
        val progress = ((age + partialTicks) / lifetime).coerceIn(0f, 1f)
        val fadeIn = ((age + partialTicks) / 8f).coerceIn(0f, 1f)
        val opacityNow = opacity * fadeIn * (1f - progress).pow(1.4f)
        val position = progress * 3f
        frame = position.toInt().coerceAtMost(3)
        val blend = position - frame
        alpha = opacityNow * (1f - blend)
        super.render(buffer, camera, partialTicks)
        if (frame < 3 && blend > 0f) {
            frame++
            alpha = opacityNow * blend
            super.render(buffer, camera, partialTicks)
        }
        alpha = opacityNow
    }

    companion object {
        private val SMOKE_RENDER_TYPE = ModParticleRenderTypes.softTranslucent(loc("textures/particle/muzzle/smoke.png"))
    }
}
