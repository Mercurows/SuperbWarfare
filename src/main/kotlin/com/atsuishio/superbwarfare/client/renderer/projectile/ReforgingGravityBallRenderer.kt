package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.LOGGER
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.ClientRenderHandler
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.client.shader.ThermalShaderHandler
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.entity.projectile.ReforgingGravityBallEntity
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.client.event.RegisterShadersEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.*
import java.util.function.Consumer
import kotlin.math.*

class ReforgingGravityBallRenderer(context: EntityRendererProvider.Context) :
    EntityRenderer<ReforgingGravityBallEntity>(context) {

    override fun render(
        entity: ReforgingGravityBallEntity, entityYaw: Float, partialTicks: Float,
        pose: PoseStack, buffers: MultiBufferSource, packedLight: Int,
    ) {
        if (ShaderCompat.isRenderingShadowPass() || entity.collapseVisualPlayed) return
        pose.pushPose()
        ClientRenderHandler.transformVirtualRenderPosition(pose, entity, partialTicks)
        val expansion = entity.expansionProgress(partialTicks).toDouble()
        val time = entity.level().gameTime - entity.anchorTime + partialTicks.toDouble()
        val coreRotation = (time * 0.008).toFloat()
        val deferredCore = queueLensing(entity, pose.last().pose(), expansion)
        // Keep the scene behind the hole intact for refraction. The post pass paints its shadow.
        if (!deferredCore) {
            pose.pushPose()
            pose.mulPose(entityRenderDispatcher.cameraOrientation())
            if (entity.anchored) {
                blackHoleCore(buffers, pose.last().pose(), max(0.0275, entity.attackRadius * 0.3 * HORIZON_RADIUS * expansion))
            } else {
                cubeCore(buffers, pose.last().pose(), 0.055, coreRotation)
            }
            pose.popPose()
        }
        val buffer = buffers.getBuffer(ModRenderTypes.REFORGING_GLOW)

        if (entity.anchored && expansion > 0.001) {
            val diskRadius = entity.attackRadius * expansion
            val fieldPose = Matrix4f(pose.last().pose())
            val orbitProgress = entity.orbitProgress(partialTicks).toDouble()
            if (worldRendering) {
                // Draw after entity-mask capture so disk/orbit colors remain part of the refracted world.
                fieldGlows.add { source ->
                    val glow = source.getBuffer(ModRenderTypes.REFORGING_GLOW)
                    accretionDisk(glow, fieldPose, diskRadius, expansion, time)
                    orbitingSpheres(glow, fieldPose, orbitProgress, expansion, time,
                        entity.attackRadius, entity.expansionDuration.toDouble())
                }
            } else {
                accretionDisk(buffer, fieldPose, diskRadius, expansion, time)
                orbitingSpheres(buffer, fieldPose, orbitProgress, expansion, time,
                    entity.attackRadius, entity.expansionDuration.toDouble())
            }
        }
        pose.popPose()
        super.render(entity, entityYaw, partialTicks, pose, buffers, packedLight)
    }

    private fun accretionDisk(
        buffer: VertexConsumer, pose: Matrix4f, outerRadius: Double, expansion: Double, time: Double,
    ) {
        if (outerRadius <= 0.0) return
        // Keep the disk denser than the original 18 rings / 22 motes, even at small radii.
        val ringCount = ceil(outerRadius * 3.0).toInt().coerceIn(24, 96)
        val moteCount = ceil(outerRadius * 6.0).toInt().coerceIn(40, 192)
        // All rings stay in the world XZ plane, independent of projectile and camera rotations.
        repeat(ringCount) { ring ->
            val fraction = ring.toDouble() / (ringCount - 1)
            val radius = outerRadius * (0.12 + 0.88 * fraction.pow(1.6))
            val rotation = time * (0.012 + (1.0 - fraction) * 0.045) + ring * 2.39996
            // Add segments on wider rings so a large disk keeps smooth arcs.
            val segmentCount = ceil(2.0 * PI * radius / 0.65).toInt().coerceIn(48, 128)
            repeat(segmentCount) { segment ->
                val angle = segment * PI * 2.0 / segmentCount
                val nextAngle = (segment + 1) * PI * 2.0 / segmentCount
                val wave = 0.5 + 0.5 * sin(angle * 2.0 + rotation)
                val alpha = ((0.06 + 0.3 * wave.pow(3)) * (1.0 - fraction * 0.65) * expansion).toFloat()
                goldBeam(
                    buffer, pose, diskPoint(radius, angle, rotation),
                    diskPoint(radius, nextAngle, rotation), (0.004 + wave * 0.004) * expansion, alpha
                )
            }
        }
        repeat(moteCount) { index ->
            // Each mote spirals inward, then respawns at the edge.
            val travel = (time / (48.0 + index % 5 * 7) + index * 0.618034) % 1.0
            val radius = outerRadius * (1.0 - travel).pow(0.8)
            val angle = index * 2.39996 + travel * PI * 3 + time * 0.012
            val point = Vec3(cos(angle) * radius, sin(angle * 2 + index) * 0.025 * expansion, sin(angle) * radius)
            val fade = (sin(PI * travel) * expansion).toFloat()
            sphere(
                buffer, pose, point, (0.009 + index % 3 * 0.004) * expansion,
                1f, 0.82f, 0.38f, fade * 0.7f
            )
        }
    }

    private fun diskPoint(radius: Double, angle: Double, rotation: Double): Vec3 =
        Vec3(cos(angle + rotation) * radius, 0.0, sin(angle + rotation) * radius)

    private fun orbitingSpheres(
        buffer: VertexConsumer, pose: Matrix4f, progress: Double, expansion: Double, time: Double,
        diskRadius: Double, expansionTicks: Double,
    ) {
        if (progress >= 1.0) return
        // Start on the unfolding disk's outer edge and spiral inward throughout its lifetime.
        val sphereSize = (diskRadius * 0.025).coerceIn(0.16, 0.28)
        val outerRadius = (diskRadius - sphereSize).coerceAtLeast(0.0)
        val radius = outerRadius * (1.0 - progress).pow(0.7) * expansion
        val visibility = ((1.0 - progress) * 24.0).coerceIn(0.0, 1.0)
        val points = orbitPoints(time, radius, progress, expansionTicks)
        val duration = if (progress > 1.0E-6) (time - expansionTicks) / progress else Double.POSITIVE_INFINITY
        val tails = Array(9) {
            val sampleTime = (time - (it + 1) * 0.7).coerceAtLeast(0.0)
            orbitPoints(sampleTime, radius, ((sampleTime - expansionTicks) / duration).coerceIn(0.0, 1.0), expansionTicks)
        }
        for (index in points.indices) {
            goldBeam(
                buffer, pose, points[index], points[(index + 1) % 3], 0.008 * expansion,
                (0.65 * visibility).toFloat()
            )
            var previous = points[index]
            for (tail in tails.indices) {
                val point = tails[tail][index]
                goldBeam(
                    buffer, pose, previous, point, 0.012 * expansion,
                    ((1.0 - (tail + 1) / 10.0) * visibility * 0.3).toFloat()
                )
                previous = point
            }
            val size = sphereSize * expansion * visibility
            sphere(buffer, pose, points[index], size * 1.6, 1f, 0.65f, 0.18f, 0.1f)
            sphere(buffer, pose, points[index], size, 1f, 0.8f, 0.35f, 0.9f)
        }
    }

    /** Equal radii and 120-degree spacing keep the rotating triangle equilateral as it contracts. */
    private fun orbitPoints(time: Double, radius: Double, progress: Double, expansionTicks: Double): Array<Vec3> {
        // Integrated omega = 0.065 / (1 - 0.95 * progress): continuous phase, up to 20x speed.
        val gain = if (progress < 1.0E-6) 1.0 else -ln(1.0 - 0.95 * progress) / (0.95 * progress)
        val rotation = 0.065 * (min(time, expansionTicks) + (time - expansionTicks).coerceAtLeast(0.0) * gain)
        return Array(3) { index ->
            val angle = rotation + index * PI * 2 / 3
            Vec3(cos(angle) * radius, 0.0, sin(angle) * radius)
        }
    }

    private fun goldBeam(
        buffer: VertexConsumer, pose: Matrix4f, start: Vec3, end: Vec3, width: Double, alpha: Float,
    ) = beam(buffer, pose, start, end, width, 1f, 0.77f, 0.3f, alpha)

    override fun getTextureLocation(entity: ReforgingGravityBallEntity) = TEXTURE

    companion object {
        private fun beam(
            buffer: VertexConsumer, pose: Matrix4f, start: Vec3, end: Vec3,
            width: Double, red: Float, green: Float, blue: Float, alpha: Float,
        ) {
            if (alpha <= 0f || width <= 0.0 || start.distanceToSqr(end) < 1.0E-10) return
            val direction = end.subtract(start).normalize()
            val reference = if (abs(direction.y) > 0.9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)
            val side = direction.cross(reference).normalize().scale(width)
            val up = direction.cross(side).normalize().scale(width)
            val offsets = arrayOf(side.add(up), side.subtract(up), side.scale(-1.0).subtract(up), up.subtract(side))
            for (i in offsets.indices) {
                val next = (i + 1) % offsets.size
                vertex(buffer, pose, start.add(offsets[i]), red, green, blue, alpha)
                vertex(buffer, pose, end.add(offsets[i]), red, green, blue, alpha)
                vertex(buffer, pose, end.add(offsets[next]), red, green, blue, alpha)
                vertex(buffer, pose, start.add(offsets[next]), red, green, blue, alpha)
            }
        }

        private val cubeCorners = List(8) { index ->
            Vec3(if (index and 1 == 0) -1.0 else 1.0, if (index and 2 == 0) -1.0 else 1.0,
                if (index and 4 == 0) -1.0 else 1.0)
        }
        private val cubeFaces = intArrayOf(0, 1, 3, 2, 4, 6, 7, 5, 0, 4, 5, 1,
            2, 3, 7, 6, 0, 2, 6, 4, 1, 5, 7, 3)

        /** Compact isometric cube used only by the flying projectile. */
        private fun cubeCore(buffers: MultiBufferSource, pose: Matrix4f, halfWidth: Double, rotation: Float) {
            val transform = Matrix4f(pose).rotateZ(rotation + (PI / 6).toFloat())
                .rotateX(asin(1.0 / sqrt(3.0)).toFloat()).rotateY((PI / 4).toFloat())
            val opaque = buffers.getBuffer(ModRenderTypes.SINGULARITY)
            for (index in cubeFaces) vertex(opaque, transform, cubeCorners[index].scale(halfWidth), 0f, 0f, 0f, 1f)
            val glow = buffers.getBuffer(ModRenderTypes.REFORGING_GLOW)
            for (index in cubeCorners.indices) for (axis in intArrayOf(1, 2, 4)) {
                if (index and axis != 0) continue
                val start = cubeCorners[index].scale(halfWidth)
                val end = cubeCorners[index or axis].scale(halfWidth)
                beam(glow, transform, start, end, halfWidth * 0.02, 1f, 0.98f, 0.82f, 1f)
                beam(glow, transform, start, end, halfWidth * 0.045, 1f, 0.72f, 0.2f, 0.65f)
                beam(glow, transform, start, end, halfWidth * 0.09, 1f, 0.65f, 0.15f, 0.08f)
            }
        }

        // Cache unit sphere vertices instead of rebuilding the trigonometry every frame.
        private fun sphereMesh(latitudes: Int, longitudes: Int): List<Vec3> = buildList {
            fun point(latitude: Int, longitude: Int): Vec3 {
                val pitch = PI * latitude / latitudes
                val yaw = PI * longitude * 2 / longitudes
                return Vec3(sin(pitch) * cos(yaw), cos(pitch), sin(pitch) * sin(yaw))
            }
            for (latitude in 0 until latitudes) for (longitude in 0 until longitudes) {
                add(point(latitude, longitude))
                add(point(latitude, longitude + 1))
                add(point(latitude + 1, longitude + 1))
                add(point(latitude + 1, longitude))
            }
        }

        private val smallSphere = sphereMesh(6, 12)
        private val burstSphere = sphereMesh(16, 32)
        private val horizonSphere = sphereMesh(24, 48)

        /** Sphere fallback when post-processing is unavailable, including shader-pack fallback paths. */
        private fun blackHoleCore(buffers: MultiBufferSource, pose: Matrix4f, radius: Double) {
            sphere(buffers.getBuffer(ModRenderTypes.SINGULARITY), pose, Vec3.ZERO, radius,
                0f, 0f, 0f, 1f, horizonSphere)
            val center = pose.transform(Vector4f(0f, 0f, 0f, 1f))
            val distanceSquared = (center.x * center.x + center.y * center.y + center.z * center.z).toDouble()
            if (distanceSquared <= radius * radius) return
            // The tangent circle matches the sphere's silhouette even at close range or off screen-center.
            val tangent = 1.0 - radius * radius / distanceSquared
            val normal = Vector3f(center.x, center.y, center.z).normalize()
            val up = if (abs(normal.y) > 0.99f) Vector3f(1f, 0f, 0f) else Vector3f(0f, 1f, 0f)
            val rimPose = Matrix4f().translation((center.x * tangent).toFloat(), (center.y * tangent).toFloat(),
                (center.z * tangent).toFloat()).rotateTowards(normal, up)
            val rimRadius = radius * sqrt(tangent)
            val glow = buffers.getBuffer(ModRenderTypes.REFORGING_GLOW)
            // Match the post shader: white at the horizon, gold farther out, then a broad soft fade.
            fun glowColor(outside: Double, layer: Int): Vector4f {
                val white = exp(-(outside / 0.032).pow(2)) * 1.65
                val gold = exp(-((outside - 0.052) / 0.05).pow(2)) * 0.85 +
                        exp(-(outside / 0.15).pow(2)) * 0.12
                val energy = white + gold
                return Vector4f(1f, ((white * 0.98 + gold * 0.75) / energy).toFloat(),
                    ((white * 0.93 + gold * 0.2) / energy).toFloat(), (energy - layer).coerceIn(0.0, 1.0).toFloat())
            }
            repeat(16) { band ->
                val inner = band * 0.24 / 16
                val outer = (band + 1) * 0.24 / 16
                for (layer in 0..1) {
                    val innerColor = glowColor(inner, layer)
                    val outerColor = glowColor(outer, layer)
                    if (innerColor.w <= 0f && outerColor.w <= 0f) continue
                    repeat(96) { index ->
                        val angle = index * PI / 48
                        val next = (index + 1) * PI / 48
                        for ((outside, theta) in arrayOf(inner to angle, outer to angle, outer to next, inner to next)) {
                            val color = if (outside == inner) innerColor else outerColor
                            val r = rimRadius * (1.0 + outside / HORIZON_RADIUS)
                            vertex(glow, rimPose, Vec3(cos(theta) * r, sin(theta) * r, 0.0),
                                color.x, color.y, color.z, color.w)
                        }
                    }
                }
            }
        }

        private fun sphere(
            buffer: VertexConsumer, pose: Matrix4f, center: Vec3, radius: Double,
            red: Float, green: Float, blue: Float, alpha: Float,
            mesh: List<Vec3> = smallSphere,
        ) {
            if (radius <= 0.0001 || alpha <= 0f) return
            for (point in mesh) vertex(buffer, pose, center.add(point.scale(radius)), red, green, blue, alpha)
        }

        private fun vertex(
            buffer: VertexConsumer, pose: Matrix4f, point: Vec3,
            red: Float, green: Float, blue: Float, alpha: Float,
        ) {
            buffer.addVertex(pose, point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
                .setColor(red, green, blue, alpha)
        }

        private val TEXTURE = loc("textures/bedrock/projectile/reforging_gravity_ball.png")
        private const val MAX_LENSES = 8
        // Half the original horizon radius; the shader extends refraction inward to meet it.
        private const val HORIZON_RADIUS = 0.21
        private const val BURST_DURATION = 10.0
        private var lensShader: ShaderInstance? = null
        private var lensScene: RenderTarget? = null
        private var lensFailed = false
        private var canLensThisFrame = false
        private var shaderPackThisFrame = false
        private var lensCompatibilityWarned = false
        private val lenses = linkedMapOf<Int, Vector4f>()
        private val lensStrengths = hashMapOf<Int, Float>()
        private val deferredCores = linkedMapOf<Int, Vector4f>()
        private var worldRendering = false
        private val fieldGlows = mutableListOf<(MultiBufferSource) -> Unit>()
        private var entityBefore: RenderTarget? = null
        private var entityAfter: RenderTarget? = null
        private var captureEntities = false
        private var entityBeforeReady = false
        private var entityMaskReady = false
        private var entityCaptureTexture = 0
        private var entityCaptureType = 0
        private var entityCaptureWarned = false
        private var burstLevel: ClientLevel? = null
        private val collapseBursts = mutableListOf<Triple<Vec3, Long, Double>>()

        fun registerLensingShader(event: RegisterShadersEvent) {
            ReforgingGravityBallEntity.playCollapseEffect = Consumer { entity ->
                val level = Minecraft.getInstance().level
                if (level != null && entity.level() === level) {
                    if (burstLevel !== level) collapseBursts.clear()
                    burstLevel = level
                    if (collapseBursts.size >= 32) collapseBursts.removeAt(0)
                    collapseBursts.add(Triple(entity.position(), level.gameTime,
                        (entity.attackRadius * 0.5).coerceIn(2.0, 8.0)))
                }
            }
            event.registerShader(
                ShaderInstance(event.resourceProvider, loc("reforging_gravity_lens"), DefaultVertexFormat.POSITION)
            ) {
                lensShader = it
                lensFailed = false
                lensCompatibilityWarned = false
                entityCaptureWarned = false
                clearLensing()
            }
        }

        fun clearLensing() {
            lenses.clear()
            lensStrengths.clear()
            deferredCores.clear()
            canLensThisFrame = false
            worldRendering = false
            fieldGlows.clear()
            captureEntities = false
            entityBeforeReady = false
            entityMaskReady = false
            entityBefore?.destroyBuffers()
            entityAfter?.destroyBuffers()
            entityBefore = null
            entityAfter = null
            lensScene?.destroyBuffers()
            lensScene = null
        }

        fun clearWorldEffects() {
            clearLensing()
            collapseBursts.clear()
            burstLevel = null
        }

        private fun renderCollapseBursts(event: RenderLevelStageEvent) {
            val mc = Minecraft.getInstance()
            val level = mc.level
            if (level == null || burstLevel !== level) {
                collapseBursts.clear()
                burstLevel = null
                return
            }
            collapseBursts.removeAll { level.gameTime - it.second >= BURST_DURATION || level.gameTime < it.second }
            if (collapseBursts.isEmpty()) return
            val buffers = mc.renderBuffers().bufferSource()
            val buffer = buffers.getBuffer(ModRenderTypes.REFORGING_GLOW)
            val camera = event.camera.position
            for ((center, start, peakRadius) in collapseBursts) {
                val progress = ((level.gameTime - start + event.partialTick.gameTimeDeltaTicks) / BURST_DURATION).coerceIn(0.0, 1.0)
                val radius = peakRadius * (0.12 + 0.88 * (1.0 - (1.0 - progress).pow(3)))
                val fade = (1.0 - progress).pow(1.5).toFloat()
                val pose = Matrix4f(event.poseStack.last().pose()).translate(
                    (center.x - camera.x).toFloat(), (center.y - camera.y).toFloat(), (center.z - camera.z).toFloat())
                // Layered white shells give a solid flash with a soft edge, then expand and fade.
                sphere(buffer, pose, Vec3.ZERO, radius, 1f, 1f, 1f, fade, burstSphere)
                sphere(buffer, pose, Vec3.ZERO, radius * 1.12, 1f, 1f, 1f, fade * 0.22f, burstSphere)
                sphere(buffer, pose, Vec3.ZERO, radius * 1.28, 1f, 1f, 1f, fade * 0.06f, burstSphere)
            }
            // Flush while Iris is still rendering the world so its native glow shader can process it.
            if (!ShaderCompat.endBatch(buffers)) buffers.endBatch(ModRenderTypes.REFORGING_GLOW)
        }

        fun renderLensing(event: RenderLevelStageEvent) {
            if (ShaderCompat.isRenderingShadowPass()) return
            if (event.stage === RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                if (fieldGlows.isNotEmpty()) {
                    val buffers = Minecraft.getInstance().renderBuffers().bufferSource()
                    try {
                        fieldGlows.forEach { it(buffers) }
                        if (!ShaderCompat.endBatch(buffers)) buffers.endBatch(ModRenderTypes.REFORGING_GLOW)
                    } finally {
                        fieldGlows.clear()
                    }
                }
                renderCollapseBursts(event)
                return
            }
            if (event.stage === RenderLevelStageEvent.Stage.AFTER_SKY) {
                val shaderPackActive = ShaderCompat.isShaderPackActive()
                if (shaderPackActive != shaderPackThisFrame) {
                    clearLensing()
                    lensFailed = false
                    lensCompatibilityWarned = false
                    entityCaptureWarned = false
                    shaderPackThisFrame = shaderPackActive
                }
                lenses.clear()
                lensStrengths.clear()
                deferredCores.clear()
                fieldGlows.clear()
                entityBeforeReady = false
                entityMaskReady = false
                canLensThisFrame = DisplayConfig.ENABLE_SPECIAL_PROJECTILE_EFFECTS.get() && lensShader != null && !lensFailed &&
                    !ThermalShaderHandler.isActive() &&
                    // Fabulous composites additional entity targets later; a single-color mask is incomplete there.
                    !Minecraft.useShaderTransparency() &&
                    (!shaderPackActive || ShaderCompat.getPostProcessWorldState(false) != null)
                if (!canLensThisFrame && (lensScene != null || entityBefore != null)) clearLensing()
                worldRendering = true
                captureEntities = canLensThisFrame && Minecraft.getInstance().level?.entitiesForRendering()?.any {
                    it is ReforgingGravityBallEntity && it.anchored && !it.collapseVisualPlayed
                } == true
            } else if (event.stage === RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS && captureEntities) {
                entityBeforeReady = captureEntityScene(true)
            } else if (event.stage === RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES && entityBeforeReady) {
                val buffers = Minecraft.getInstance().renderBuffers().bufferSource()
                // Flush translucent and emissive model layers, including ones that never write depth.
                if (!ShaderCompat.endBatch(buffers)) buffers.endBatch()
                entityMaskReady = captureEntityScene(false)
            } else if (event.stage === RenderLevelStageEvent.Stage.AFTER_LEVEL) {
                try {
                    if (canLensThisFrame && lenses.isNotEmpty()) {
                        if (!entityMaskReady) {
                            // Never use an incomplete/stale mask on an unsupported framebuffer path.
                            renderDeferredCores(false)
                            return
                        }
                        if (shaderPackThisFrame) {
                            val worldState = ShaderCompat.getPostProcessWorldState()
                            if (worldState == null) {
                                if (!lensCompatibilityWarned) {
                                    LOGGER.warn("Gravity lensing skipped: shader pipeline world projection/depth is unavailable")
                                    lensCompatibilityWarned = true
                                }
                                renderDeferredCores(false)
                                return
                            }
                            renderDeferredCores(true)
                            if (!applyLensing(worldState.first, event.partialTick.gameTimeDeltaTicks, worldState.second)) renderDeferredCores(false)
                        } else {
                            renderDeferredCores(true)
                            if (!applyLensing(event.projectionMatrix, event.partialTick.gameTimeDeltaTicks)) renderDeferredCores(false)
                        }
                    }
                } finally {
                    lenses.clear()
                    lensStrengths.clear()
                    deferredCores.clear()
                    canLensThisFrame = false
                    worldRendering = false
                    captureEntities = false
                    entityBeforeReady = false
                    entityMaskReady = false
                    fieldGlows.clear()
                }
            }
        }

        private fun queueLensing(entity: ReforgingGravityBallEntity, pose: Matrix4f, expansion: Double): Boolean {
            if (!canLensThisFrame || !entity.anchored || expansion < 0.025) return false
            // Capture the actual view-space render position, including the virtual muzzle offset.
            val center = pose.transform(Vector4f(0f, 0f, 0f, 1f))
            val radius = (entity.attackRadius * 0.3 * expansion).toFloat()
            if (radius <= 0f || center.z >= -radius * 0.25f) return false
            val projection = RenderSystem.getProjectionMatrix()
            val clip = projection.transform(Vector4f(center))
            val extent = radius * maxOf(abs(projection.m00()), abs(projection.m11()))
            if (clip.w <= 0f || abs(clip.x) > clip.w + extent || abs(clip.y) > clip.w + extent) return false
            val mc = Minecraft.getInstance()
            if (radius / -center.z * abs(projection.m11()) * mc.mainRenderTarget.height < 1.5f) return false
            deferredCores[entity.id] = Vector4f(center.x, center.y, center.z, radius)
            if (!lenses.containsKey(entity.id) && lenses.size >= MAX_LENSES) {
                val farthest = lenses.minBy { it.value.z }
                if (center.z <= farthest.value.z) return true
                lenses.remove(farthest.key)
                lensStrengths.remove(farthest.key)
            }
            val nearFade = ((-center.z - radius * 0.25f) / (radius * 0.75f)).coerceIn(0f, 1f)
            val collapseBoost = if (entity.collapsing) 1f + sin(PI * (1.0 - expansion)).toFloat() * 0.6f else 1f
            lenses[entity.id] = center.apply { w = radius }
            lensStrengths[entity.id] = 0.2f * expansion.toFloat() * nearFade * collapseBoost
            return true
        }

        private fun renderDeferredCores(skipLensed: Boolean) {
            val buffers = Minecraft.getInstance().renderBuffers().bufferSource()
            var drawn = false
            for ((id, center) in deferredCores) {
                if (skipLensed && id in lenses) continue
                val pose = Matrix4f().translation(center.x, center.y, center.z)
                blackHoleCore(buffers, pose, center.w * HORIZON_RADIUS)
                drawn = true
            }
            if (drawn) {
                buffers.endBatch(ModRenderTypes.SINGULARITY)
                buffers.endBatch(ModRenderTypes.REFORGING_GLOW)
            }
        }

        /** Capture actual model pixels, including layers that do not write depth or exceed their hitbox. */
        private fun captureEntityScene(before: Boolean): Boolean {
            val main = Minecraft.getInstance().mainRenderTarget
            val drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
            val readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
            val drawBuffer = GL11.glGetInteger(GL20.GL_DRAW_BUFFER0)
            val viewport = BufferUtils.createIntBuffer(4)
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport)
            if (drawFramebuffer == 0 || drawBuffer < GL30.GL_COLOR_ATTACHMENT0 ||
                viewport[0] != 0 || viewport[1] != 0 || viewport[2] != main.width || viewport[3] != main.height ||
                GL11.glGetInteger(GL13.GL_SAMPLES) != 0
            ) return false
            val attachmentType = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER,
                drawBuffer, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE)
            val attachment = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_DRAW_FRAMEBUFFER,
                drawBuffer, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME)
            if (attachment == 0 || (!before &&
                (attachment != entityCaptureTexture || attachmentType != entityCaptureType))) return false
            if (before) {
                // Iris may switch FBOs between terrain and entities while keeping the same color attachment.
                entityCaptureTexture = attachment
                entityCaptureType = attachmentType
            }
            val depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
            val scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
            val scissorBox = BufferUtils.createIntBuffer(4)
            if (scissor) GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox)
            val clearColor = BufferUtils.createFloatBuffer(4)
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor)
            val activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)
            RenderSystem.activeTexture(GL13.GL_TEXTURE0)
            val texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
            var sourceReadBuffer: Int? = null
            return try {
                RenderSystem.disableScissor()
                var target = if (before) entityBefore else entityAfter
                if (target == null || target.width != main.width || target.height != main.height) {
                    target?.destroyBuffers()
                    target = TextureTarget(main.width, main.height, false, Minecraft.ON_OSX)
                    if (before) entityBefore = target else entityAfter = target
                    // Preserve shader-pack HDR colors: clamping both captures to white would hide emissive layers.
                    GlStateManager._bindTexture(target.colorTextureId)
                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, main.width, main.height,
                        0, GL11.GL_RGBA, GL11.GL_FLOAT, 0L)
                    target.setFilterMode(GL11.GL_NEAREST)
                }
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, drawFramebuffer)
                sourceReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER)
                GL11.glReadBuffer(drawBuffer)
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId)
                if (GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
                    false
                } else {
                    GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
                        GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST)
                    true
                }
            } catch (exception: Exception) {
                if (!entityCaptureWarned) {
                    LOGGER.warn("Gravity lensing skipped: entity color capture is unavailable", exception)
                    entityCaptureWarned = true
                }
                false
            } finally {
                if (sourceReadBuffer != null) {
                    GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, drawFramebuffer)
                    GL11.glReadBuffer(sourceReadBuffer)
                }
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer)
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer)
                RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
                RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3])
                if (depthTest) RenderSystem.enableDepthTest() else RenderSystem.disableDepthTest()
                if (scissor) RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3])
                else RenderSystem.disableScissor()
                GlStateManager._bindTexture(texture)
                RenderSystem.activeTexture(activeTexture)
            }
        }

        private fun applyLensing(projection: Matrix4f, partialTick: Float, preHandDepth: Int = 0): Boolean {
            val mc = Minecraft.getInstance()
            val main = mc.mainRenderTarget
            val shader = lensShader ?: return false
            if (main.width <= 0 || main.height <= 0 || main.depthTextureId < 0) return false
            if (preHandDepth != 0 && !GL11.glIsTexture(preHandDepth)) return false
            val previousShader = RenderSystem.getShader()
            val previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)
            val drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
            val readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
            val viewport = BufferUtils.createIntBuffer(4)
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport)
            val colorMask = BufferUtils.createByteBuffer(4)
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMask)
            val scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
            val scissorBox = BufferUtils.createIntBuffer(4)
            if (scissor) GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox)
            val stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST)
            val depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
            val depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)
            val blending = GL11.glIsEnabled(GL11.GL_BLEND)
            val culling = GL11.glIsEnabled(GL11.GL_CULL_FACE)
            val activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)
            val textureBindings = IntArray(5) { unit ->
                RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit)
                GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
            }
            val capabilities = GL.getCapabilities()
            val samplerBindings = if (capabilities.OpenGL33 || capabilities.GL_ARB_sampler_objects) {
                IntArray(5) { GL30.glGetIntegeri(GL33.GL_SAMPLER_BINDING, it) }
            } else null
            return try {
                RenderSystem.disableScissor()
                if (stencil) GL11.glDisable(GL11.GL_STENCIL_TEST)
                RenderSystem.colorMask(true, true, true, true)
                samplerBindings?.indices?.forEach { GL33.glBindSampler(it, 0) }
                var scene = lensScene
                if (scene == null || scene.width != main.width || scene.height != main.height ||
                    scene.isStencilEnabled != main.isStencilEnabled
                ) {
                    scene?.destroyBuffers()
                    scene = TextureTarget(main.width, main.height, true, Minecraft.ON_OSX)
                    lensScene = scene
                    // Depth blits require matching formats, including the scope's stencil attachment.
                    if (main.isStencilEnabled) scene.enableStencil()
                    scene.setFilterMode(GL11.GL_LINEAR)
                }
                // Copy once for all lenses; never sample from an attachment being written to.
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId)
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scene.frameBufferId)
                GL30.glBlitFramebuffer(
                    0, 0, main.width, main.height, 0, 0, main.width, main.height,
                    GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST
                )
                main.bindWrite(true)

                shader.setSampler("SceneColor", scene.colorTextureId)
                shader.setSampler("SceneDepth", scene.depthTextureId)
                // Iris renders hands before final compositing. depthtex2 identifies those pixels.
                shader.setSampler("WorldDepth", if (preHandDepth != 0) preHandDepth else scene.depthTextureId)
                shader.setSampler("EntitiesBefore", entityBefore!!.colorTextureId)
                shader.setSampler("EntitiesAfter", entityAfter!!.colorTextureId)
                shader.safeGetUniform("EntityImageMask").set(1)
                shader.safeGetUniform("ShaderPackMode").set(if (preHandDepth != 0) 1 else 0)
                shader.safeGetUniform("LensProjection").set(projection)
                shader.safeGetUniform("InverseProjection").set(Matrix4f(projection).invert())
                shader.safeGetUniform("LensCount").set(lenses.size)
                shader.safeGetUniform("LensTime").set(((mc.level!!.gameTime % 24000) + partialTick) * 0.05f)
                val strengths = FloatArray(MAX_LENSES)
                for ((index, entry) in lenses.entries.withIndex()) {
                    val center = entry.value
                    shader.safeGetUniform("Lens$index").set(center.x, center.y, center.z, center.w)
                    strengths[index] = lensStrengths[entry.key] ?: 0f
                }
                shader.safeGetUniform("LensStrengths0").set(strengths[0], strengths[1], strengths[2], strengths[3])
                shader.safeGetUniform("LensStrengths1").set(strengths[4], strengths[5], strengths[6], strengths[7])

                RenderSystem.disableDepthTest()
                RenderSystem.depthMask(false)
                RenderSystem.disableBlend()
                RenderSystem.disableCull()
                RenderSystem.setShader { shader }
                val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
                builder.addVertex(-1.0F, -1.0F, 0.0F)
                builder.addVertex(1.0F, -1.0F, 0.0F)
                builder.addVertex(1.0F, 1.0F, 0.0F)
                builder.addVertex(-1.0F, 1.0F, 0.0F)
                BufferUploader.drawWithShader(builder.buildOrThrow())
                true
            } catch (exception: Exception) {
                lensFailed = true
                lensScene?.destroyBuffers()
                lensScene = null
                LOGGER.warn("Gravity ball lensing disabled until the next resource reload", exception)
                false
            } finally {
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer)
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer)
                RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
                RenderSystem.colorMask(colorMask[0].toInt() != 0, colorMask[1].toInt() != 0,
                    colorMask[2].toInt() != 0, colorMask[3].toInt() != 0)
                if (scissor) RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3])
                else RenderSystem.disableScissor()
                if (stencil) GL11.glEnable(GL11.GL_STENCIL_TEST)
                RenderSystem.depthMask(depthWrite)
                if (depthTest) RenderSystem.enableDepthTest() else RenderSystem.disableDepthTest()
                if (blending) RenderSystem.enableBlend() else RenderSystem.disableBlend()
                if (culling) RenderSystem.enableCull() else RenderSystem.disableCull()
                textureBindings.forEachIndexed { unit, texture ->
                    RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit)
                    GlStateManager._bindTexture(texture)
                }
                samplerBindings?.forEachIndexed { unit, sampler -> GL33.glBindSampler(unit, sampler) }
                RenderSystem.activeTexture(activeTexture)
                RenderSystem.setShader { previousShader }
                GlStateManager._glUseProgram(previousProgram)
            }
        }
    }
}
