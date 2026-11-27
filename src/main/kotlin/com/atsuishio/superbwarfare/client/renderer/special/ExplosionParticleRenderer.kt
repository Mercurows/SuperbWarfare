package com.atsuishio.superbwarfare.client.renderer.special

import com.atsuishio.superbwarfare.client.particle.NuclearSmokeParticle
import com.atsuishio.superbwarfare.client.renderer.ModParticleRenderTypes
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.atsuishio.superbwarfare.mixins.LevelRendererParticleAccessor
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import net.minecraft.client.Camera
import net.minecraft.client.particle.Particle
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.world.level.Level
import org.joml.Matrix4f
import java.util.*

object ExplosionParticleRenderer {
    private data class ParticleDraw(val particle: Particle, val distanceSquared: Double, val flash: Boolean)

    private val explosionParticles = WeakHashMap<Particle, Boolean>()
    private val queuedParticles = IdentityHashMap<Particle, Unit>()
    private val draws = ArrayList<ParticleDraw>()
    private val buffers = MultiBufferSource.immediate(BufferBuilder(256))
    private val projection = Matrix4f()
    private val modelView = Matrix4f()
    private var camera: Camera? = null
    private var partialTick = 0f
    private var deferThisFrame = false
    private var shadowPass = false

    fun spawn(
        level: Level, options: ParticleOptions, x: Double, y: Double, z: Double,
        vx: Double, vy: Double, vz: Double, force: Boolean = true, flash: Boolean = false,
    ) {
        if (level !== mc.level) return
        val renderer = mc.levelRenderer as LevelRendererParticleAccessor
        val particle = renderer.`superbWarfare$addParticle`(options, force, x, y, z, vx, vy, vz) ?: return
        explosionParticles[particle] = flash
    }

    @JvmStatic
    fun beginFrame() {
        queuedParticles.clear()
        draws.clear()
        camera = null
        deferThisFrame = ModParticleRenderTypes.softShader() != null && ShaderCompat.isShaderPackActive()
    }

    @JvmStatic
    fun beginParticlePass() {
        shadowPass = deferThisFrame && ShaderCompat.isRenderingShadowPass()
    }

    @JvmStatic
    fun defer(particle: Particle, camera: Camera, partialTick: Float): Boolean {
        if (!deferThisFrame) return false
        val flash = explosionParticles[particle] ?: if (particle is NuclearSmokeParticle) false else return false
        // Shader packs can traverse the same particle in more than one phase.
        if (shadowPass || queuedParticles.put(particle, Unit) != null) return true

        if (draws.isEmpty()) {
            projection.set(RenderSystem.getProjectionMatrix())
            modelView.set(RenderSystem.getModelViewMatrix())
            this.camera = camera
            this.partialTick = partialTick
        }
        val bounds = particle.boundingBox
        val distanceSquared = camera.position.distanceToSqr(
            (bounds.minX + bounds.maxX) * 0.5, (bounds.minY + bounds.maxY) * 0.5, (bounds.minZ + bounds.maxZ) * 0.5,
        )
        draws.add(ParticleDraw(particle, distanceSquared, flash))
        return true
    }

    @JvmStatic
    fun renderAfterComposite() {
        val camera = camera ?: return
        val previousProjection = RenderSystem.getProjectionMatrix()
        val previousSorting = RenderSystem.getVertexSorting()
        val previousShader = RenderSystem.getShader()
        val previousTexture = RenderSystem.getShaderTexture(0)
        val previousLightmap = RenderSystem.getShaderTexture(2)
        val viewStack = RenderSystem.getModelViewStack()
        viewStack.pushPose()
        try {
            viewStack.last().pose().set(modelView)
            RenderSystem.applyModelViewMatrix()
            RenderSystem.setProjectionMatrix(projection, previousSorting)
            val atlasTexture = TextureAtlas.LOCATION_PARTICLES
            val atlas = mc.textureManager.getTexture(atlasTexture) as? TextureAtlas
            val flipbook = atlas?.getSprite(NuclearSmokeParticle.FLIPBOOK_SPRITE)
                ?.takeIf { it.contents().name() == NuclearSmokeParticle.FLIPBOOK_SPRITE }
            val atlasType = ModRenderTypes.EXPLOSION_SMOKE.apply(atlasTexture)
            val nuclearType = if (flipbook == null) ModRenderTypes.EXPLOSION_SMOKE.apply(NuclearSmokeParticle.TEXTURE)
            else atlasType
            draws.sortWith { first, second -> second.distanceSquared.compareTo(first.distanceSquared) }
            for (draw in draws) {
                val particle = draw.particle
                val nuclear = particle as? NuclearSmokeParticle
                val type = when {
                    draw.flash -> ModRenderTypes.EXPLOSION_FLASH
                    nuclear != null -> nuclearType
                    else -> atlasType
                }
                val vertices = buffers.getBuffer(type)
                if (nuclear != null && flipbook != null) {
                    nuclear.renderInAtlas(vertices, camera, partialTick, flipbook)
                } else {
                    particle.render(vertices, camera, partialTick)
                }
            }
            buffers.endBatch()
        } finally {
            queuedParticles.clear()
            draws.clear()
            this.camera = null
            viewStack.popPose()
            RenderSystem.applyModelViewMatrix()
            RenderSystem.setProjectionMatrix(previousProjection, previousSorting)
            RenderSystem.setShader { previousShader }
            RenderSystem.setShaderTexture(0, previousTexture)
            RenderSystem.setShaderTexture(2, previousLightmap)
        }
    }
}
