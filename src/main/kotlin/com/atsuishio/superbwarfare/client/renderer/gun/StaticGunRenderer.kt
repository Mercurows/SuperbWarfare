package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel
import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoReadout
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.attachment.*
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun_skin.GunSkin
import com.atsuishio.superbwarfare.data.gun_skin.GunSkinInfo
import com.atsuishio.superbwarfare.resource.gun.DefaultGunResource
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.resource.model.AttachmentModelReloadListener
import com.atsuishio.superbwarfare.tools.RenderDistanceHelper
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.MeshFlushStats
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.StaticMeshBufferSource
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.StaticMeshRenderer
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.cache.StaticMeshCache
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.capture.GeometryCollector
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.gpu.MeshVertexFormat
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.render.EmissiveMeshRenderTypes
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Matrix4f
import java.time.Duration
import java.util.*

/** Bind-pose FIXED/GROUND geometry, shared VBOs, and one material batch per world render pass. */
@Mod.EventBusSubscriber(modid = "superbwarfare", value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.FORGE)
object StaticGunRenderer : ResourceManagerReloadListener {
    private val owner = loc("static_guns")
    private val retention = Duration.ofSeconds(60)
    private var depth = 0
    private var wasEnabled = true
    private val appearances = WeakHashMap<ItemStack, Appearance>()
    private val models = WeakHashMap<GeoGunModel, GeoGunModel>()
    private var geometryRenderer: GeometryRenderer? = null
    private var worldPass = false
    private var batch: StaticMeshBufferSource? = null
    var lastBatchStats: MeshFlushStats? = null
        private set
    private var capturedMeshes = 0L
    private var lastReport = 0L
    private var queuedDraws = 0L
    private var immediateDraws = 0L
    private var pendingDraws = 0L
    private var fallbackDraws = 0L
    private var checkedFrame = Long.MIN_VALUE
    private var pipelineState: ShaderCompat.PipelineState? = null
    private var vertexFormat: VertexFormat? = null
    private var formatCheckFrame = Long.MIN_VALUE

    private fun modelInstance(original: GeoGunModel): GeoGunModel =
        models.getOrPut(original) { GeoGunModel(original.baseModel, false) }

    fun <T> renderItem(render: () -> T): T {
        depth++
        return try { render() } finally { depth-- }
    }

    /** Shared by normal rendering and its cache key, including when GPU caching is disabled. */
    fun shouldUseLOD(context: ItemDisplayContext): Boolean {
        if (context.firstPerson() || GunMountContext.active) return false
        return DisplayConfig.ENABLE_GUN_LOD.get() && !RenderDistanceHelper.isInGui()
    }

    private class Key(
        val item: CompoundTag, val resource: DefaultGunResource, val lod: Boolean,
        val dataVersion: Int, val skin: GunSkinInfo?, val context: ItemDisplayContext
    ) {
        private val hash = 31 * (31 * (31 * (31 * item.hashCode() + System.identityHashCode(resource)) +
                lod.hashCode()) + dataVersion) + (skin?.hashCode() ?: 0) + 31 * context.hashCode()
        override fun hashCode() = hash
        override fun equals(other: Any?) = other is Key && resource === other.resource &&
                context == other.context && lod == other.lod && dataVersion == other.dataVersion && skin == other.skin && item == other.item
    }

    private class Appearance(
        val key: Key, val stack: ItemStack, val sourceTag: CompoundTag?,
        val revision: Long, val nextValidation: Long
    ) {
        var unsupported = false
    }

    fun tryRender(
        stack: ItemStack, context: ItemDisplayContext,
        pose: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int
    ): Boolean {
        if (context != ItemDisplayContext.FIXED && context != ItemDisplayContext.GROUND) return false
        val enabled = DisplayConfig.ENABLE_STATIC_GUN_RENDERER.get()
        if (!enabled) {
            if (wasEnabled) {
                appearances.clear()
                StaticMeshRenderer.invalidateOwner(owner)
            }
            wasEnabled = false
            return false
        }
        wasEnabled = true
        if (depth > 0 && !DisplayConfig.ENABLE_DISPLAY_BOARD_GUN_CACHE.get()) return false
        if (GunMountContext.active) return false
        updateRenderEnvironment()
        if (!StaticMeshRenderer.isOwnerEnabled(owner) ||
            !StaticMeshRenderer.supportsInstanceAttributes(light, overlay)) {
            fallbackDraws++
            reportDiagnostics()
            return false
        }

        val resource = GunResource.compute(stack)

        // Include capabilities, exact ammo count, skins, attachment offsets and shield charge.
        // Reuse the immutable key/hash while unchanged; no per-frame bone/pose snapshot.
        val lod = shouldUseLOD(context)
        val skin = GunSkin.getSkin(stack)
        val tick = Minecraft.getInstance().level?.gameTime ?: 0L
        val revision = stack.tag?.getCompound(GunData.KEY_GUN_DATA)?.getLong(GunData.KEY_REVISION) ?: 0L
        var appearance = appearances[stack]
        if (appearance == null || appearance.sourceTag !== stack.tag || appearance.revision != revision ||
            tick >= appearance.nextValidation || appearance.key.resource !== resource ||
            appearance.key.lod != lod || appearance.key.dataVersion != GunData.DATA_VERSION ||
            appearance.key.skin != skin || appearance.key.context != context) {
            // Board updates replace the stack; GunData edits bump Revision. Periodically recheck
            // other NBT/capability edits too, spread across ticks instead of allocating every frame.
            val item = stack.serializeNBT()
            stripIdentity(item)
            val key = Key(item, resource, lod, GunData.DATA_VERSION, skin, context)
            val previous = appearance
            appearance = Appearance(key, stack.copy(), stack.tag, revision,
                tick + 20 + (System.identityHashCode(stack) and 15))
            if (previous?.key == key) appearance.unsupported = previous.unsupported
            appearances[stack] = appearance
        }
        if (appearance.unsupported) {
            fallbackDraws++
            reportDiagnostics()
            return false
        }
        val selected = appearance
        val renderer = geometryRenderer ?: GeometryRenderer().also { geometryRenderer = it }
        val provider = com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.capture.MeshGeometryProvider { collector ->
            capturedMeshes++
            val capture = Capture(collector)
            val available = renderer.capture(selected.stack, context, lod, capture)
            selected.unsupported = !capture.supported
            capture.supported && available
        }
        // Enqueue entities and block entities into the SAME frame batch. Outside the world pass
        // (GUI previews etc.), use the same static renderer without deferring its draw.
        val useBatch = worldPass && pipelineState?.active != true && !ShaderCompat.isRenderingShadowPass() &&
                buffers === Minecraft.getInstance().renderBuffers().bufferSource()
        if (useBatch) {
            val current = batch ?: StaticMeshRenderer.openBatch(owner, StaticMeshRenderer.BatchOrder.MATERIAL)
                .also { batch = it }
            if (current.samePass()) {
                val result = current.submit(selected.key, null, provider, pose, light, overlay, retention)
                if (result.hasMesh()) queuedDraws++ else if (result.owned()) pendingDraws++ else fallbackDraws++
                reportDiagnostics()
                return !selected.unsupported && result.owned()
            }
        }
        val result = StaticMeshRenderer.tryDraw(owner, selected.key, null, provider, pose, light, overlay, retention)
        if (result.drawn()) immediateDraws++ else if (result.owned()) pendingDraws++ else fallbackDraws++
        reportDiagnostics()
        return !selected.unsupported && result.owned()
        // PENDING is owned: do not rebuild/render the high-poly gun again while an upload is queued.
    }

    private fun reportDiagnostics() {
        val now = System.nanoTime()
        if (DisplayConfig.STATIC_GUN_RENDER_DIAGNOSTICS.get() && now - lastReport >= 5_000_000_000L) {
            lastReport = now
            com.atsuishio.superbwarfare.Mod.LOGGER.info(
                "Static guns: queued={}, immediate={}, pending={}, fallback={}, captures={}, shaders={}, batch={}",
                queuedDraws, immediateDraws, pendingDraws, fallbackDraws, capturedMeshes, pipelineState?.active, lastBatchStats)
        }
    }

    internal fun pipelineChanged(previous: ShaderCompat.PipelineState?, current: ShaderCompat.PipelineState): Boolean =
        previous != null && (previous.pipeline !== current.pipeline || previous.revision != current.revision ||
                previous.active != current.active)

    private fun updateRenderEnvironment() {
        val frame = StaticMeshCache.currentFrame()
        if (checkedFrame == frame) return
        checkedFrame = frame
        val current = ShaderCompat.pipelineState()
        val changed = pipelineChanged(pipelineState, current)
        pipelineState = current
        if (changed || formatCheckFrame == Long.MIN_VALUE || frame - formatCheckFrame >= 60) {
            val format = MeshVertexFormat.probe()
            formatCheckFrame = frame
            val formatChanged = format != null && vertexFormat != null && vertexFormat !== format
            vertexFormat = format
            if (changed || formatChanged) {
                // invalidateOwner only drops entries; it does not reset SBM's failed-owner / integer
                // attribute state. A global vertex-layout change invalidates all immediate VBOs.
                discardBatch()
                StaticMeshCache.clear()
                appearances.clear()
                lastBatchStats = null
            }
        }
    }

    internal fun stripIdentity(item: CompoundTag) {
        val gunTag = item.getCompound("tag").getCompound(GunData.KEY_GUN_DATA)
        gunTag.remove(GunData.KEY_UUID)
        gunTag.remove(GunData.KEY_REVISION)
        gunTag.remove("CreatedTime")
    }

    /** Models declare textures explicitly instead of reflecting into RenderType internals. */
    fun registerMaterials(
        buffers: MultiBufferSource, texture: ResourceLocation, readout: AmmoReadout,
        emissive: ResourceLocation? = null
    ) {
        val capture = buffers as? Capture ?: return
        capture.materials[RenderType.entityTranslucent(texture)] = Material(
            RenderType.entityCutout(texture), EmissiveMeshRenderTypes.cutout(texture, VertexFormat.Mode.QUADS))
        capture.materials[BedrockModelRenderTypes.polyMeshCutout(texture)] = Material(
            BedrockModelRenderTypes.polyMeshCutout(texture),
            EmissiveMeshRenderTypes.cutout(texture, VertexFormat.Mode.TRIANGLES))
        if (emissive != null) {
            capture.materials[RenderType.eyes(emissive)] = Material(RenderType.eyes(emissive), null)
            capture.materials[ModRenderTypes.polyMeshEyes(emissive)] = Material(ModRenderTypes.polyMeshEyes(emissive), null)
        }
    }

    private data class Material(val ordinary: RenderType, val illuminated: RenderType?)

    private class Capture(val collector: GeometryCollector) : MultiBufferSource {
        val materials = HashMap<RenderType, Material>()
        val attachments = WeakHashMap<BedrockAttachmentModel, BedrockAttachmentModel>()
        var drawingAttachment = false
        var supported = true

        override fun getBuffer(type: RenderType): VertexConsumer {
            val mode = type.mode()
            if (!materials.containsKey(type) ||
                (mode != VertexFormat.Mode.QUADS && mode != VertexFormat.Mode.TRIANGLES)) {
                // Finish rendering to restore shared state, but never upload unknown text/shaders.
                supported = false
                return collector.buffer(type, VertexFormat.Mode.QUADS)
            }
            val material = materials.getValue(type)
            return if (material.illuminated == null) UnlitVertexConsumer(collector.buffer(material.ordinary, mode))
                else collector.buffer(material.ordinary, material.illuminated, mode)
        }
    }

    /** Route attachment draws to separate bind-pose instances. A held gun's animated/shared model
     * must never supply the geometry of a cached display gun of the same type.
     */
    fun renderAttachment(
        buffers: MultiBufferSource, original: BedrockAttachmentModel, pose: PoseStack,
        texture: ResourceLocation, companion: ScopeMode?
    ): Boolean {
        val capture = buffers as? Capture ?: return false
        if (capture.drawingAttachment) return false
        val model = capture.attachments.getOrPut(original) { BedrockAttachmentModel(original.baseModel) }
        capture.drawingAttachment = true
        try {
            model.renderToBuffer(pose, capture, texture, 0, OverlayTexture.NO_OVERLAY, companion, AmmoReadout())
        } finally {
            capture.drawingAttachment = false
        }
        return true
    }

    /** Reuses attachment selection/mounting rules, never GeoGunRenderer.renderModel or its effects. */
    private class GeometryRenderer : GeoGunRenderer() {

        fun capture(stack: ItemStack, context: ItemDisplayContext, lod: Boolean, source: Capture): Boolean {
            val resource = GunResource.compute(stack)
            val modelResource = resource.getModel()
            val selected = selectGunModel(modelResource, GunSkin.getSkin(stack), lod) ?: return false
            val model = modelInstance(selected.model)
            model.resetPose()
            val pose = PoseStack()
            try {
                applyModelBonePositioning(pose, model, modelResource, context)
                renderBarrelExtension(stack, model)
                renderAttachments(stack, model, context, pose, source, 0, OverlayTexture.NO_OVERLAY)
                val divisions = model.builtinScopeRenderer.hideDivisionBones()
                try {
                    model.renderToBuffer(pose, source, selected.texture, 0, OverlayTexture.NO_OVERLAY,
                        emissiveTexture = GunEmissiveTextures.get(selected.texture))
                } finally {
                    model.builtinScopeRenderer.restoreDivisionBones(divisions)
                }
                return true
            } finally {
                model.resetPose()
            }
        }

        override fun renderAttachments(
            stack: ItemStack, model: GeoGunModel, transformType: ItemDisplayContext,
            poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int
        ) {
            renderMagazine(stack, model)
            renderBulletChain(stack, model)
            renderProjectileBone(stack, model)
            renderScopeMount(stack, model)
            renderScopeAttachment(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
            renderStock(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
            renderGripHandGuard(stack, model)
            renderGripAttachment(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
            renderOemScope(stack, model)
            renderOemMuzzle(stack, model)
            renderMuzzleAttachment(stack, model, poseStack, bufferSource, packedLight, packedOverlay)
            renderRegisteredAttachments(stack, model, transformType, poseStack, bufferSource, packedLight, packedOverlay)
        }

        override fun renderRegisteredAttachments(
            stack: ItemStack, model: GeoGunModel, transformType: ItemDisplayContext,
            poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int,
            packedOverlay: Int, hand: InteractionHand
        ) {
            val data = GunData.from(stack)
            for (slot in AttachmentSlots.ALL) {
                if (slot.renderMode != AttachmentRenderMode.GENERIC) continue
                val id = data.attachment.id(slot.type) ?: continue
                val definition = AttachmentDefinition.from(id) ?: continue
                val (path, texture) = definition.resolveAssets(data, slot.type) ?: continue
                val bone = AttachmentSlots.mountBoneOf(slot, definition) ?: continue
                val mount = model.getGlobalTransform(bone) ?: continue
                val attachment = AttachmentModelReloadListener.getModel(path) ?: continue
                poseStack.pushPose()
                try {
                    mulPoseWithNormal(poseStack, Matrix4f(mount).mul(attachmentTransform(stack, slot.type, definition)))
                    attachment.renderToBuffer(poseStack, bufferSource, texture, packedLight, packedOverlay)
                } finally {
                    poseStack.popPose()
                }
            }
        }

        override fun resolveAmmoReadout(
            stack: ItemStack, bars: List<AmmoBarEntry>, texts: List<AmmoTextEntry>, range: Int
        ) = AmmoReadout()

        override fun renderScopeAttachment(
            stack: ItemStack, model: GeoGunModel, poseStack: PoseStack,
            bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int
        ) {
            val scope = resolveScopeAttachmentRender(stack, model) ?: return
            for (host in scope.hosts) {
                poseStack.pushPose()
                try {
                    mulPoseWithNormal(poseStack, host.slotTransform)
                    host.model.renderToBuffer(poseStack, bufferSource, host.texture, packedLight, packedOverlay)
                } finally { poseStack.popPose() }
            }
            poseStack.pushPose()
            try {
                mulPoseWithNormal(poseStack, scope.slotTransform)
                scope.model.renderToBuffer(poseStack, bufferSource, scope.texture, packedLight, packedOverlay,
                    scope.companionSightMode)
            } finally { poseStack.popPose() }
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onRenderStage(event: RenderLevelStageEvent) {
        when (event.stage) {
            RenderLevelStageEvent.Stage.AFTER_SKY -> worldPass = true
            RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES -> {
                worldPass = false
                val current = batch
                batch = null
                current?.use { current ->
                    if (current.samePass()) {
                        Minecraft.getInstance().renderBuffers().bufferSource().endBatch()
                        lastBatchStats = current.endBatch()
                        reportDiagnostics()
                    }
                }
            }
            RenderLevelStageEvent.Stage.AFTER_LEVEL -> discardBatch()
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onRenderTick(event: TickEvent.RenderTickEvent) {
        if (event.phase == TickEvent.Phase.START) {
            discardBatch()
            checkedFrame = Long.MIN_VALUE
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onLevelUnload(event: LevelEvent.Unload) {
        if (event.level.isClientSide) {
            discardBatch()
            appearances.clear()
            models.clear()
            geometryRenderer = null
            pipelineState = null
            vertexFormat = null
            formatCheckFrame = Long.MIN_VALUE
            checkedFrame = Long.MIN_VALUE
        }
    }

    private fun discardBatch() {
        worldPass = false
        batch?.discard()
        batch = null
    }

    /** Eyes shaders ignore the lightmap. Canonicalize UV2 so normal and illuminated bones in the
     * same glow pass do not make SBM reject the WHOLE appearance as mixed per-vertex lighting.
     * Every chained setter must return this wrapper, otherwise vertex(...).uv2(...) bypasses it.
     */
    internal class UnlitVertexConsumer(private val target: VertexConsumer) : VertexConsumer {
        override fun vertex(x: Double, y: Double, z: Double): VertexConsumer {
            target.vertex(x, y, z); return this
        }
        override fun color(r: Int, g: Int, b: Int, a: Int): VertexConsumer {
            target.color(r, g, b, a); return this
        }
        override fun uv(u: Float, v: Float): VertexConsumer {
            target.uv(u, v); return this
        }
        override fun overlayCoords(u: Int, v: Int): VertexConsumer {
            target.overlayCoords(u, v); return this
        }
        override fun uv2(u: Int, v: Int): VertexConsumer {
            target.uv2(0, 0); return this
        }
        override fun normal(x: Float, y: Float, z: Float): VertexConsumer {
            target.normal(x, y, z); return this
        }
        override fun endVertex() = target.endVertex()
        override fun defaultColor(r: Int, g: Int, b: Int, a: Int) = target.defaultColor(r, g, b, a)
        override fun unsetDefaultColor() = target.unsetDefaultColor()
    }

    override fun onResourceManagerReload(resourceManager: ResourceManager) {
        discardBatch()
        appearances.clear()
        models.clear()
        geometryRenderer = null
        // Resource reload may also rebuild shader programs / formats and must allow a failed path
        // to recover. SBM's own resource listener resets its global cache too.
        StaticMeshCache.clear()
        pipelineState = null
        vertexFormat = null
        formatCheckFrame = Long.MIN_VALUE
        checkedFrame = Long.MIN_VALUE
    }
}
