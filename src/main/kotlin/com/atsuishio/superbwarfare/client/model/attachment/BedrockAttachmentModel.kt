package com.atsuishio.superbwarfare.client.model.attachment

import com.atsuishio.superbwarfare.client.renderer.scope.AmmoReadout
import com.atsuishio.superbwarfare.client.renderer.scope.ScopeStencilRenderHelper
import com.atsuishio.superbwarfare.data.attachment.*
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import java.util.regex.Pattern

class BedrockAttachmentModel(private val baseModel: TreeBedrockModel) {
    private val instance: TreeModelInstance = baseModel.createInstance()

    private val defaultScopeBodyIndex: Int
    private val dynamicDivisionIndex: Int
    private val ocularRingIndices = mutableMapOf<Int, Int>()
    private val ocularIndicesByGroup = mutableMapOf<Int, List<Int>>()
    private val isScopeOcularByGroup = mutableMapOf<Int, List<Boolean>>()
    private val modeScopeBodyGroups = mutableMapOf<Int, List<Int>>()
    private val divisionGroups = mutableMapOf<String, List<Int>>()
    private val illuminatedBoneIndices: IntArray = baseModel.bones()
        .asSequence()
        .filter { it.name().endsWith(ILLUMINATED_SUFFIX) }
        .map { it.index() }
        .toList()
        .toIntArray()

    init {
        markIlluminatedBones()

        val ocularsByGroup = mutableMapOf<Int, MutableList<OcularEntry>>()
        for (bone in baseModel.bones()) {
            val matcher = OCULAR_PATTERN.matcher(bone.name())
            if (!matcher.matches()) continue

            val num = matcher.group(3)?.toIntOrNull() ?: 0
            val isScope = OCULAR_SCOPE_NODE == matcher.group(1)
            ocularsByGroup.getOrPut(num) { mutableListOf() } += OcularEntry(bone.index(), isScope)
        }
        for ((group, entries) in ocularsByGroup) {
            ocularIndicesByGroup[group] = entries.map { it.index }
            isScopeOcularByGroup[group] = entries.map { it.isScope }
        }

        val ocularRingPattern = Pattern.compile("^${OCULAR_RING_NODE}(_\\d+)?$")
        for (bone in baseModel.bones()) {
            val matcher = ocularRingPattern.matcher(bone.name())
            if (!matcher.matches()) continue
            val num = matcher.group(1)?.removePrefix("_")?.toIntOrNull() ?: 0
            ocularRingIndices[num] = bone.index()
        }

        // Unnumbered scope_body stays common; scope_body_N is selected per mode.
        val scopeBodyPattern = Pattern.compile("^${SCOPE_BODY_NODE}(_\\d+)?$")
        for (bone in baseModel.bones()) {
            val matcher = scopeBodyPattern.matcher(bone.name())
            if (!matcher.matches()) continue
            val num = matcher.group(1)?.removePrefix("_")?.toIntOrNull() ?: 0
            if (num > 0) {
                val indices = mutableListOf<Int>()
                collectGeometryBones(bone.index(), indices)
                modeScopeBodyGroups[num] = indices
            }
        }

        val divisionPattern = Pattern.compile("^${DIVISION_NODE}(_\\d+)?$")
        for (bone in baseModel.bones()) {
            if (!divisionPattern.matcher(bone.name()).matches()) continue
            val indices = mutableListOf<Int>()
            addDivisionGeometry(bone.index(), bone.name(), indices)
            divisionGroups[bone.name()] = indices
        }

        defaultScopeBodyIndex = baseModel.getIndex(SCOPE_BODY_NODE)
        dynamicDivisionIndex = baseModel.getIndex(DYNAMIC_DIVISION_NODE)
    }

    private fun collectGeometryBones(boneIndex: Int, out: MutableList<Int>) {
        if (boneIndex < 0) return
        val bone = baseModel.bone(boneIndex)
        if (bone.hasQuads() || bone.hasVertices()) {
            out += boneIndex
            return
        }

        for (child in baseModel.bones()) {
            if (child.parentIndex() == boneIndex) {
                collectGeometryBones(child.index(), out)
            }
        }
    }

    private fun addDivisionGeometry(
        divisionIndex: Int,
        divisionRootName: String,
        out: MutableList<Int>
    ) {
        if (divisionIndex < 0) return

        val divisionBone = baseModel.bone(divisionIndex)
        if (divisionBone.hasQuads()) {
            out += divisionIndex
            setBoneVisible(divisionIndex, false)
            return
        }

        val children = baseModel.bones()
            .filter { it.parentIndex() == divisionIndex && it.name().startsWith("${divisionRootName}_") }
            .filter { it.hasQuads() }

        if (children.isEmpty()) {
            out += divisionIndex
            setBoneVisible(divisionIndex, false)
            return
        }

        for (child in children) {
            out += child.index()
            setBoneVisible(child.index(), false)
        }
    }

    fun getGlobalTransform(boneName: String): Matrix4f? {
        val index = baseModel.getIndex(boneName)
        return if (index >= 0) instance.getGlobalTransform(index) else null
    }

    fun renderToBuffer(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        texture: ResourceLocation,
        packedLight: Int,
        packedOverlay: Int,
        companionSightMode: ScopeMode? = null,
        readout: AmmoReadout = AmmoReadout()
    ) {
        val hiddenOculars = if (companionSightMode != null) ocularIndicesFor(companionSightMode) else emptyList()
        val originalOcularVisibility = BooleanArray(hiddenOculars.size)
        for (i in hiddenOculars.indices) {
            val bone = instance.getBone(hiddenOculars[i])
            if (bone != null) {
                originalOcularVisibility[i] = bone.visible
                bone.visible = false
            }
        }

        restoreScopeBodyVisibility()
        markIlluminatedBones()
        val ammoBarState = applyAmmoBar(readout.bars, readout.progress)
        val quadType = RenderType.entityTranslucent(texture)
        val triangleType = BedrockModelRenderTypes.polyMeshCutout(texture)
        baseModel.renderToBuffer(
            instance,
            poseStack,
            bufferSource,
            quadType,
            triangleType,
            packedLight,
            packedOverlay,
            1f,
            1f,
            1f,
            1f,
            true
        )
        renderAmmoBar(ammoBarState, poseStack, bufferSource, quadType, triangleType, packedLight, true)
        restoreAmmoBar(ammoBarState)

        // After the restore, so nothing is drawn while the model is still carrying the squashed
        // scales. A text anchored on the scope body is correct here as-is; one anchored below a
        // division lands inside the housing and gets depth tested away, which is why the aiming path
        // draws those itself.
        for (entry in readout.texts) {
            renderAmmoText(entry, readout.count, readout.progress, poseStack, bufferSource)
        }

        for (i in hiddenOculars.indices) {
            instance.getBone(hiddenOculars[i])?.visible = originalOcularVisibility[i]
        }
    }

    fun needsStencil(info: ScopeMode?): Boolean =
        info != null && ocularIndicesFor(info).isNotEmpty()

    fun renderWithStencil(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        texture: ResourceLocation,
        packedLight: Int,
        partialTicks: Float,
        info: ScopeMode,
        companionSightMode: ScopeMode? = null,
        readout: AmmoReadout = AmmoReadout()
    ) {
        markIlluminatedBones()
        updateDynamicDivisionScale()
        val ammoBarState = applyAmmoBar(readout.bars, readout.progress)
        val texts = buildAmmoTexts(readout)
        val quadType = RenderType.entityTranslucent(texture)
        val triangleType = BedrockModelRenderTypes.polyMeshCutout(texture)

        when (info.type) {
            ScopeType.SIGHT -> renderSight(
                poseStack,
                bufferSource,
                quadType,
                triangleType,
                packedLight,
                info,
                texts
            )

            ScopeType.SCOPE -> renderScope(
                poseStack,
                bufferSource,
                quadType,
                triangleType,
                packedLight,
                partialTicks,
                info,
                texts
            )
        }

        renderRemaining(
            poseStack,
            bufferSource,
            quadType,
            triangleType,
            packedLight,
            info,
            companionSightMode,
            ammoBarState,
            texts
        )

        restoreAmmoBar(ammoBarState)
    }

    /**
     * Squashes every bone in [entries] along its configured axis to [progress] (0-1) and returns the
     * state to hand back to [restoreAmmoBar] and [renderAmmoBar] once rendering is done.
     *
     * Returning the state instead of stashing it in a field keeps this reentrant, which matters
     * because a single [BedrockAttachmentModel] instance is shared by every stack using the same
     * model file and can be rendered more than once per frame.
     *
     * Entries that carry a color are also hidden here, because they are drawn separately by
     * [renderAmmoBar] so they can take a tint, and a hidden bone drops its whole subtree from the
     * model pass. Visibility deliberately has this single owner: [renderSight] and [renderScope] draw
     * `scope_body*` / `ocular*` through [renderBoneImmediate] long before [renderRemaining] runs, so
     * hiding only inside [renderRemaining] would let those paths draw an untinted bar.
     *
     * Unlike the gun model, the attachment instance is never reset through `resetPose`, so a caller
     * that skips [restoreAmmoBar] would leak the squashed scale into every later render.
     */
    private fun applyAmmoBar(entries: List<AmmoBarEntry>, progress: Float): AmmoBarState? {
        if (entries.isEmpty()) return null

        // coerceIn passes NaN straight through, and a NaN scale would poison the whole model's
        // vertices rather than just the bar, so clamp defensively.
        val scale = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 1f
        val boneIndices = IntArray(entries.size)
        val savedScales = FloatArray(entries.size * SCALES_PER_BONE)
        val savedVisible = BooleanArray(entries.size)
        val tints = IntArray(entries.size) { UNTINTED }
        var anyBone = false

        for (i in entries.indices) {
            val index = baseModel.getIndex(entries[i].bone)
            boneIndices[i] = index
            if (index < 0) continue
            val bone = instance.getBone(index) ?: continue

            anyBone = true
            savedScales[i * SCALES_PER_BONE] = bone.xScale
            savedScales[i * SCALES_PER_BONE + 1] = bone.yScale
            savedScales[i * SCALES_PER_BONE + 2] = bone.zScale
            savedVisible[i] = bone.visible

            when (entries[i].axis) {
                AmmoBarAxis.X -> {
                    bone.xScale = scale
                    bone.yScale = 1f
                    bone.zScale = 1f
                }

                AmmoBarAxis.Y -> {
                    bone.xScale = 1f
                    bone.yScale = scale
                    bone.zScale = 1f
                }

                AmmoBarAxis.Z -> {
                    bone.xScale = 1f
                    bone.yScale = 1f
                    bone.zScale = scale
                }

                // Nothing is written, so the bone keeps the scale the model gives it. The entry is
                // still captured and restored above, which is what lets it be tinted on its own.
                AmmoBarAxis.NONE -> {}
            }

            if (entries[i].isTinted()) {
                tints[i] = entries[i].colorAt(scale)
                bone.visible = false
            }
        }
        return if (anyBone) AmmoBarState(boneIndices, savedScales, savedVisible, tints) else null
    }

    /** Restores everything [applyAmmoBar] captured. */
    private fun restoreAmmoBar(state: AmmoBarState?) {
        if (state == null) return

        for (i in state.boneIndices.indices) {
            val index = state.boneIndices[i]
            if (index < 0) continue
            val bone = instance.getBone(index) ?: continue

            bone.xScale = state.savedScales[i * SCALES_PER_BONE]
            bone.yScale = state.savedScales[i * SCALES_PER_BONE + 1]
            bone.zScale = state.savedScales[i * SCALES_PER_BONE + 2]
            bone.visible = state.savedVisible[i]
        }
    }

    /**
     * Draws the subtree of every tinted ammo bar bone with its resolved color.
     *
     * This deliberately does not flush, unlike [renderBoneImmediate]. The model pass has already
     * queued geometry into the same buffers as part of one batch, so flushing here would split that
     * batch, and on the Oculus path it would drain everything else the caller had buffered up.
     */
    private fun renderAmmoBar(
        state: AmmoBarState?,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int,
        skipNormalVisibilityCull: Boolean
    ) {
        if (state == null) return

        for (i in state.boneIndices.indices) {
            val tint = state.tints[i]
            val index = state.boneIndices[i]
            if (tint == UNTINTED || index < 0) continue

            // applyAmmoBar hid this bone so the model pass would skip it, and renderBone bails out on
            // an invisible bone, so it has to be turned back on for this draw. restoreAmmoBar puts the
            // original value back.
            instance.getBone(index)?.visible = true

            instance.renderSingleBone(
                poseStack,
                index,
                bufferSource,
                quadType,
                triangleType,
                light,
                OverlayTexture.NO_OVERLAY,
                ((tint shr 16) and 0xFF) / 255f,
                ((tint shr 8) and 0xFF) / 255f,
                (tint and 0xFF) / 255f,
                ((tint ushr 24) and 0xFF) / 255f,
                skipNormalVisibilityCull
            )
        }
    }

    /**
     * Resolves every configured text to a bone index plus the division it belongs to, if any.
     *
     * Recomputed per render instead of cached: [readout] is handed in by the caller each frame, and
     * a single instance of this class is shared by every stack using the same model file.
     *
     * Nothing is filtered out here: which texts end up on screen is decided where they are drawn. A
     * text under a `division*` root goes out with the reticle, so the zoom gate in [renderDivisionOnly]
     * and [renderOcularAndDivisionInternal] covers it, and one anchored anywhere else is drawn by
     * [renderRemaining] regardless.
     *
     * This is the aiming path only — the whole-model pass draws its texts straight from [readout].
     * That has no bearing on the gate above: third person and the inventory draw the entire model,
     * housing included, and the housing is what hides a text mounted inside the tube from every angle
     * it can be seen from.
     */
    private fun buildAmmoTexts(readout: AmmoReadout): List<AmmoText> {
        if (readout.texts.isEmpty()) return emptyList()

        val texts = mutableListOf<AmmoText>()
        for (entry in readout.texts) {
            val index = baseModel.getIndex(entry.bone)
            if (index < 0) continue
            // -1 is kept rather than filtered out: an anchor outside a division subtree cannot be
            // drawn alongside a reticle, but it still has to be drawn by renderRemaining, otherwise
            // it would be visible only in third person and the inventory.
            texts += AmmoText(entry, readout.count, readout.progress, divisionAnchorOf(index))
        }
        return texts
    }

    /**
     * Index of the division bone [textBoneIndex] hangs under, or `-1` when it is not inside a
     * `division*` subtree.
     *
     * This decides how the text becomes visible: a division bone is hidden in the whole-model pass
     * and only drawn where the reticle is drawn separately, so an anchor below one only shows up
     * while aiming down the sights, and only once the zoom has reached [DIVISION_MIN_ZOOM]. `-1`
     * means the anchor sits elsewhere in the model — usually on the scope body — and is drawn by
     * [renderRemaining] instead, depth tested against the body.
     */
    private fun divisionAnchorOf(textBoneIndex: Int): Int {
        val anchors = divisionGroups.values.flatten().toHashSet()
        var index = baseModel.bone(textBoneIndex).parentIndex()
        while (index >= 0) {
            if (index in anchors) return index
            index = baseModel.bone(index).parentIndex()
        }
        return -1
    }

    /**
     * Draws one ammo readout line at its anchor bone, which supplies both the position and the
     * facing of the glyphs.
     *
     * The glyphs are always drawn at full brightness rather than with the light of the gun they sit
     * on. `rendertype_text.vsh` computes `vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0)`,
     * i.e. the packed light handed to [Font.drawInBatch] is a lightmap texel, so passing the world
     * light would dim the readout to the point of being unreadable in the dark — and inside a scope
     * tube there is no sky access to brighten it either. [LightTexture.FULL_BRIGHT] is the corner
     * texel of the 16x16 lightmap, which is the brightest it can be at the player's own brightness
     * setting, and is the same coordinate vanilla uses for GUI items.
     *
     * Note that the fragment shader never samples a lightmap itself; the multiply happens per vertex,
     * which is why this cannot be fixed by choosing a different [Font.DisplayMode].
     */
    private fun renderAmmoText(
        entry: AmmoTextEntry,
        count: Int,
        progress: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource
    ) {
        val index = baseModel.getIndex(entry.bone)
        if (index < 0) return

        val text = entry.resolve(count)
        if (text.isEmpty()) return

        val font = Minecraft.getInstance().font
        poseStack.pushPose()
        poseStack.mulPoseMatrix(instance.getGlobalTransform(index))
        // Glyphs are laid out for a space where +y points down, so Y has to be mirrored for the text
        // to come out upright. That mirroring alone would leave the determinant negative and reverse
        // the winding of every quad, and RenderType.text never calls setCullState, so it inherits the
        // default of backface culling and the text would vanish entirely. Mirroring Z as well puts the
        // determinant back to +scale³, and it costs nothing visually: all four vertices of a glyph
        // quad share the same z, so the quad is mapped onto itself.
        poseStack.scale(entry.scale, -entry.scale, -entry.scale)
        poseStack.translate(entry.offsetX(font.width(text)), GLYPH_BOX_CENTER, 0f)
        // DisplayMode.NORMAL only picks the glyph atlas and blending; whether depth testing applies is
        // left to the caller, which is what lets the reticle pass draw the text with depth testing off
        // while the whole-model pass keeps it on.
        font.drawInBatch(
            text,
            0f,
            0f,
            entry.colorAt(progress),
            entry.shadow,
            poseStack.last().pose(),
            bufferSource,
            Font.DisplayMode.NORMAL,
            0,
            LightTexture.FULL_BRIGHT
        )
        // drawInBatch queues the glyphs into the caller's own buffer source, and they would sit there
        // until something asks for a different render type. They have to go out now instead: what
        // limits where the text is visible is the stencil and depth state around this call, and by the
        // time the outer endBatch runs that window is long closed. endLastBatch() flushes exactly the
        // one render type that is currently open, so there is no need to rebuild the RenderType.text
        // instance, and no dependency on which font atlas a resource pack supplies.
        if (bufferSource is MultiBufferSource.BufferSource) bufferSource.endLastBatch()
        poseStack.popPose()
    }

    private fun renderSight(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int,
        info: ScopeMode,
        texts: List<AmmoText>
    ) {
        ScopeStencilRenderHelper.enableItemEntityStencilTest()
        RenderSystem.clearStencil(0)
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX)

        renderOcularStencil(poseStack, bufferSource, quadType, triangleType, light, false, info)
        renderDivisionOnly(
            poseStack,
            bufferSource,
            quadType,
            triangleType,
            light,
            divisionIndices(info),
            texts
        )

        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF)
        ScopeStencilRenderHelper.disableItemEntityStencilTest()

        for (bodyIndex in scopeBodyIndices(info)) {
            renderBoneImmediate(bodyIndex, poseStack, bufferSource, quadType, triangleType, light)
        }
    }

    private fun renderScope(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int,
        partialTicks: Float,
        info: ScopeMode,
        texts: List<AmmoText>
    ) {
        ScopeStencilRenderHelper.enableItemEntityStencilTest()
        RenderSystem.clearStencil(0)
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX)

        val ringIndex = ocularRingIndex(info)
        if (ringIndex >= 0) {
            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF)
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
            renderBoneImmediate(ringIndex, poseStack, bufferSource, quadType, triangleType, light)
        }

        renderOcularStencil(poseStack, bufferSource, quadType, triangleType, light, false, info)

        val bodyIndices = scopeBodyIndices(info)
        if (bodyIndices.isNotEmpty()) {
            RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF)
            for (bodyIndex in bodyIndices) {
                renderBoneImmediate(bodyIndex, poseStack, bufferSource, quadType, triangleType, light)
            }
        }

        renderOcularAndDivision(
            poseStack,
            bufferSource,
            quadType,
            triangleType,
            light,
            partialTicks,
            info,
            false,
            texts
        )

        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF)
        ScopeStencilRenderHelper.disableItemEntityStencilTest()
    }

    private fun renderOcularStencil(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int,
        selectScope: Boolean,
        info: ScopeMode
    ) {
        renderOcularStencilInternal(
            poseStack,
            bufferSource,
            quadType,
            triangleType,
            light,
            selectScope,
            ocularIndicesFor(info),
            isScopeOcularFor(info)
        )
    }

    private fun renderOcularStencilInternal(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int,
        selectScope: Boolean,
        ocularIndices: List<Int>,
        isScopeOcular: List<Boolean>
    ) {
        if (ocularIndices.isEmpty()) return

        RenderSystem.colorMask(false, false, false, false)
        RenderSystem.depthMask(false)
        RenderSystem.stencilMask(0xFF)
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE)

        for (i in ocularIndices.indices.reversed()) {
            if (selectScope == isScopeOcular[i]) {
                RenderSystem.stencilFunc(GL11.GL_GREATER, i + 1, 0xFF)
                renderBoneImmediate(ocularIndices[i], poseStack, bufferSource, quadType, triangleType, light)
            }
        }

        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
        RenderSystem.depthMask(true)
        RenderSystem.colorMask(true, true, true, true)
    }

    private fun renderDivisionOnly(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int,
        divisions: List<Int>,
        texts: List<AmmoText> = emptyList()
    ) {
        if (divisions.isEmpty()) return
        // The reticle and its readout are one subtree as far as the viewer is concerned, so both wait
        // for the zoom. See DIVISION_MIN_ZOOM.
        if (ClientEventHandler.zoomTime < DIVISION_MIN_ZOOM) return

        RenderSystem.disableDepthTest()
        for (i in divisions.indices) {
            RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF)
            renderBoneImmediate(divisions[i], poseStack, bufferSource, quadType, triangleType, light)
            // Same stencil value and depth state as the reticle it sits next to.
            for (text in texts) {
                if (text.divisionIndex == divisions[i]) {
                    renderAmmoText(text.entry, text.count, text.progress, poseStack, bufferSource)
                }
            }
        }
        RenderSystem.enableDepthTest()
    }

    private fun renderOcularAndDivision(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int,
        partialTicks: Float,
        info: ScopeMode,
        selective: Boolean,
        texts: List<AmmoText>
    ) {
        renderOcularAndDivisionInternal(
            poseStack,
            bufferSource,
            quadType,
            triangleType,
            light,
            partialTicks,
            info,
            ocularIndicesFor(info),
            isScopeOcularFor(info),
            divisionIndices(info),
            selective,
            texts
        )
    }

    private fun renderOcularAndDivisionInternal(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int,
        partialTicks: Float,
        info: ScopeMode,
        ocularIndices: List<Int>,
        isScopeOcular: List<Boolean>,
        divisions: List<Int>,
        selective: Boolean,
        texts: List<AmmoText> = emptyList()
    ) {
        if (ocularIndices.isEmpty()) return

        val builder: BufferBuilder = Tesselator.getInstance().builder
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INVERT)
        RenderSystem.colorMask(false, false, false, false)
        RenderSystem.depthMask(false)

        val aimingProgress = ClientEventHandler.zoomTime.coerceIn(0.0, 1.0).toFloat()
        val rad = 80f * info.viewRadiusModifier * aimingProgress

        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        for (i in ocularIndices.indices) {
            if (selective && !isScopeOcular[i]) continue

            RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF)
            val ocularCenter = getBoneCenter(poseStack, ocularIndices[i])
            val centerX = ocularCenter.x() * 16f * 90f
            val centerY = ocularCenter.y() * 16f * 90f

            builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR)
            builder.vertex(centerX.toDouble(), centerY.toDouble(), -90.0).color(255, 255, 255, 255).endVertex()
            for (j in 0..90) {
                val angle = j * ((Math.PI * 2.0) / 90.0)
                val sin = Mth.sin(angle.toFloat())
                val cos = Mth.cos(angle.toFloat())
                builder.vertex((centerX + cos * rad).toDouble(), (centerY + sin * rad).toDouble(), -90.0)
                    .color(255, 255, 255, 255)
                    .endVertex()
            }
            BufferUploader.drawWithShader(builder.end())
        }

        RenderSystem.depthMask(true)
        RenderSystem.colorMask(true, true, true, true)
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)

        // Reticle and readout come in together, so both wait for the zoom. The ocular itself does not:
        // it is the window, and it is what the reticle is drawn through. See DIVISION_MIN_ZOOM.
        val divisionDue = ClientEventHandler.zoomTime >= DIVISION_MIN_ZOOM
        for (i in ocularIndices.indices) {
            if (i > Byte.MAX_VALUE) {
                throw IllegalArgumentException("Index of oculus is out of range for 127")
            }
            if (i >= divisions.size) break

            if (selective && !isScopeOcular[i]) {
                if (!divisionDue) continue
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF)
                renderBoneImmediate(divisions[i], poseStack, bufferSource, quadType, triangleType, light)
            } else {
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF)
                renderBoneImmediate(ocularIndices[i], poseStack, bufferSource, quadType, triangleType, light)

                if (!divisionDue) continue
                val b = (i + 1).inv() and 0xFF
                RenderSystem.stencilFunc(GL11.GL_EQUAL, b, 0xFF)
                renderBoneImmediate(divisions[i], poseStack, bufferSource, quadType, triangleType, light)
                for (text in texts) {
                    if (text.divisionIndex == divisions[i]) {
                        renderAmmoText(text.entry, text.count, text.progress, poseStack, bufferSource)
                    }
                }
            }
        }
    }

    private fun divisionIndices(info: ScopeMode): List<Int> {
        return divisionGroups[info.divisionBone()]
            ?: divisionGroups[DIVISION_NODE]
            ?: emptyList()
    }

    private fun scopeBodyIndices(info: ScopeMode): List<Int> {
        modeScopeBodyGroups[info.index]?.let { group ->
            return if (info.type == ScopeType.SCOPE) {
                group + listOfNotNull(defaultScopeBodyIndex)
            } else {
                group
            }
        }
        return if (modeScopeBodyGroups.isEmpty()) listOfNotNull(defaultScopeBodyIndex) else emptyList()
    }

    private fun ocularIndicesFor(info: ScopeMode): List<Int> {
        return ocularIndicesByGroup[info.index] ?: ocularIndicesByGroup[0] ?: emptyList()
    }

    private fun isScopeOcularFor(info: ScopeMode): List<Boolean> {
        return isScopeOcularByGroup[info.index] ?: isScopeOcularByGroup[0] ?: emptyList()
    }

    private fun ocularRingIndex(info: ScopeMode): Int {
        // Sight optics model their lens without an ocular_ring.
        if (info.type == ScopeType.SIGHT) return -1
        return ocularRingIndices[info.index] ?: ocularRingIndices[0] ?: -1
    }

    private fun renderRemaining(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int,
        info: ScopeMode,
        companion: ScopeMode? = null,
        ammoBarState: AmmoBarState? = null,
        texts: List<AmmoText> = emptyList()
    ) {
        val hidden = mutableListOf<Int>()
        // Other numbered scope parts stay in the model render; only the active optic group is special.
        scopeBodyIndices(info).forEach { addSpecialIndex(hidden, it) }
        if (info.type == ScopeType.SCOPE) {
            addSpecialIndex(hidden, ocularRingIndex(info))
        }
        hidden += ocularIndicesFor(info)
        if (companion?.isSight() == true) {
            hidden += ocularIndicesFor(companion)
            if (ClientEventHandler.zoomTime > 0.15) {
                scopeBodyIndices(companion).forEach { addSpecialIndex(hidden, it) }
            }
        }
        divisionGroups.values.forEach { hidden += it }

        val originalVisible = BooleanArray(hidden.size)
        for (i in hidden.indices) {
            val bone = instance.getBone(hidden[i])
            if (bone != null) {
                originalVisible[i] = bone.visible
                bone.visible = false
            }
        }

        baseModel.renderToBuffer(
            instance,
            poseStack,
            bufferSource,
            quadType,
            triangleType,
            light,
            OverlayTexture.NO_OVERLAY
        )

        for (i in hidden.indices) {
            instance.getBone(hidden[i])?.visible = originalVisible[i]
        }

        // This path renders the whole model through the multi-buffer overload, which passes
        // skipNormalVisibilityCull = false.
        renderAmmoBar(ammoBarState, poseStack, bufferSource, quadType, triangleType, light, false)
        flush(bufferSource, quadType, triangleType)

        // Texts anchored outside a division subtree. Unlike a reticle text they are not inside the
        // ocular opening, so they need the depth buffer rather than a stencil window to stay in front
        // of the housing — and by now the whole body has been drawn into it, on this path and on the
        // aiming path alike. Drawn last so nothing of the model can overwrite them.
        for (text in texts) {
            if (text.divisionIndex < 0) {
                renderAmmoText(text.entry, text.count, text.progress, poseStack, bufferSource)
            }
        }
    }

    private fun renderBoneImmediate(
        boneIndex: Int,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int
    ) {
        if (boneIndex < 0) return
        val bone = instance.getBone(boneIndex) ?: return
        val originalVisible = bone.visible
        bone.visible = true

        poseStack.pushPose()
        val parentIndex = bone.parentIndex()
        if (parentIndex >= 0) {
            instance.mulGlobalTransform(poseStack, parentIndex)
        }

        val quadBuffer: VertexConsumer = bufferSource.getBuffer(quadType)
        baseModel.renderBone(
            instance,
            boneIndex,
            poseStack,
            quadBuffer,
            light,
            OverlayTexture.NO_OVERLAY,
            1f,
            1f,
            1f,
            1f,
            true
        )
        val triangleBuffer: VertexConsumer = bufferSource.getBuffer(triangleType)
        baseModel.renderBone(
            instance,
            boneIndex,
            poseStack,
            triangleBuffer,
            light,
            OverlayTexture.NO_OVERLAY,
            1f,
            1f,
            1f,
            1f,
            false
        )
        flush(bufferSource, quadType, triangleType)

        poseStack.popPose()
        bone.visible = originalVisible
    }

    private fun flush(
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType
    ) {
        if (!com.atsuishio.superbwarfare.compat.oculus.OculusCompat.endBatch(bufferSource)) {
            bufferSource.endBatch(quadType)
            bufferSource.endBatch(triangleType)
        }
    }

    private fun getBoneCenter(poseStack: PoseStack, boneIndex: Int): Vector3f {
        val matrix = Matrix4f(poseStack.last().pose()).mul(instance.getGlobalTransform(boneIndex))
        return matrix.getTranslation(Vector3f())
    }

    private fun setBoneVisible(boneIndex: Int, visible: Boolean) {
        instance.getBone(boneIndex)?.visible = visible
    }

    private fun restoreScopeBodyVisibility() {
        if (defaultScopeBodyIndex >= 0) {
            setBoneVisible(defaultScopeBodyIndex, true)
        }
        modeScopeBodyGroups.values.forEach { group ->
            group.forEach { setBoneVisible(it, true) }
        }
    }

    private fun markIlluminatedBones() {
        for (index in illuminatedBoneIndices) {
            instance.getBone(index)?.illuminated = true
        }
    }

    private fun updateDynamicDivisionScale() {
        if (dynamicDivisionIndex < 0) return
        val bone = instance.getBone(dynamicDivisionIndex) ?: return
        val scale = ClientEventHandler.customZoom.coerceAtLeast(1.0).toFloat()
        bone.xScale = scale
        bone.yScale = scale
    }

    private fun addSpecialIndex(list: MutableList<Int>, index: Int) {
        if (index >= 0) list += index
    }

    private data class OcularEntry(val index: Int, val isScope: Boolean)

    /**
     * One text to draw: the line itself, the count to expand it with, and the index of the division
     * bone it hangs under, which is what the division loops match on.
     *
     * [divisionIndex] is `-1` when the anchor bone is not inside a `division*` subtree. Such a text
     * cannot be drawn next to a reticle, so it is drawn by [renderRemaining] instead — that is how a
     * model that hangs its readout off the scope body (rather than off the reticle) still shows the
     * text while aiming. No division bone ever has index `-1`, so the division guards stay correct
     * without an extra check.
     */
    private class AmmoText(
        val entry: AmmoTextEntry,
        val count: Int,
        /** Remaining magazine ratio, which the entry resolves its tiered color against. */
        val progress: Float,
        val divisionIndex: Int
    )

    /**
     * What [applyAmmoBar] has to give back: the bones it touched (`-1` where the model has no such
     * bone), the scales and visibility they had, and the resolved tint per bone, with [UNTINTED]
     * marking the entries that keep rendering as part of the model.
     */
    private class AmmoBarState(
        val boneIndices: IntArray,
        /** Flattened `x, y, z` scale per entry — [SCALES_PER_BONE] floats each. */
        val savedScales: FloatArray,
        val savedVisible: BooleanArray,
        val tints: IntArray
    )

    companion object {
        // Real tints are opaque ARGB, so the sign bit is free to mark "no tint configured".
        private const val UNTINTED = Int.MIN_VALUE

        /**
         * Floats [AmmoBarState.savedScales] spends per entry: `x`, `y` and `z`.
         *
         * All three are captured even though an entry drives only one of them, because the other two
         * are written to as well — an entry squashing on X has to be sure the bone was not left
         * squashed on Y by something else — and a scale that is set but not restored outlives the
         * frame, since nothing resets the attachment instance's pose.
         */
        private const val SCALES_PER_BONE = 3

        /**
         * Font-space Y offset that puts the centre of a glyph box on the anchor bone.
         *
         * A glyph quad spans `[y, y + height]` around the `y` passed to `drawInBatch`: the sheet
         * builder subtracts its own baseline adjustment, which for the default font's ascent of 7
         * leaves the top edge exactly on `y`. Digits are 7 units tall there, so half of that box —
         * with the sign flipped, because the text is drawn below its origin — is what centres it.
         * Descenders reach 8 units, which is at worst half a unit off on a readout that is nothing
         * but digits.
         */
        private const val GLYPH_BOX_CENTER = -3.5f

        /**
         * Aiming progress below which no `division*` subtree is drawn at all — reticle and readout
         * alike.
         *
         * The reticle is drawn with depth testing off inside the ocular window, so while the scope is
         * still swinging up it would already be floating in front of the scope body. Holding the whole
         * subtree back until the zoom is this far along keeps the readout attached to the thing it
         * labels, and gets the two on screen at the same moment instead of the text trailing the
         * reticle in.
         *
         * Split into two gates rather than one because a bone's `visible` flag cannot express this:
         * a text is drawn from its anchor bone's global transform and ignores that flag entirely,
         * while [renderBoneImmediate] forces the flag on for the bones it draws.
         *
         * A `Double` to match [ClientEventHandler.zoomTime] rather than the font-space floats above,
         * so the comparison is against exactly 0.4 and not against the `Float` it would be widened to.
         */
        private const val DIVISION_MIN_ZOOM = 0.4

        private const val SCOPE_BODY_NODE = "scope_body"
        private const val OCULAR_RING_NODE = "ocular_ring"
        private const val DIVISION_NODE = "division"
        private const val DYNAMIC_DIVISION_NODE = "dynamic_divison"
        private const val OCULAR_NODE = "ocular"
        private const val OCULAR_SIGHT_NODE = "ocular_sight"
        private const val OCULAR_SCOPE_NODE = "ocular_scope"
        private const val ILLUMINATED_SUFFIX = "_illuminated"
        private val OCULAR_PATTERN = Pattern.compile(
            "^($OCULAR_NODE|$OCULAR_SIGHT_NODE|$OCULAR_SCOPE_NODE)(_(\\d+))?$"
        )
    }
}
