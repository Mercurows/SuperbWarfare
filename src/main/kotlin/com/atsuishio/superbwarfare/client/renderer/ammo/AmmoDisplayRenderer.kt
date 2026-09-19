package com.atsuishio.superbwarfare.client.renderer.ammo

import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoDisplayRenderer.Companion.SCALES_PER_BONE
import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoDisplayRenderer.Companion.UNTINTED
import com.atsuishio.superbwarfare.data.attachment.AmmoBarAxis
import com.atsuishio.superbwarfare.data.attachment.AmmoBarEntry
import com.atsuishio.superbwarfare.data.attachment.AmmoTextEntry
import com.atsuishio.superbwarfare.tools.mulPoseMatrix
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture

/**
 * Drives the ammo bar and ammo text readouts of a single bedrock model instance.
 *
 * Shared by every model that can carry a readout — [com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel]
 * for scopes and the other attachment slots, and [com.atsuishio.superbwarfare.client.model.gun.GeoGunModel]
 * for the gun body itself. Both wrap the same [TreeBedrockModel] + [TreeModelInstance] pair, so the
 * whole mechanism is model-agnostic: it only ever looks bones up by name and reads the
 * [AmmoBarEntry] / [AmmoTextEntry] configuration handed to it.
 *
 * [divisionBones] is the one piece of model knowledge this class needs up front. It holds every bone
 * under a `division*` root, i.e. the reticle subtree of a scope. A text anchored below one of those
 * is only drawn where the reticle is drawn separately, which is what makes a reticle readout appear
 * only while aiming down the sights. Models without a reticle pass an empty set and every text
 * resolves to `-1`, meaning "draw it with the rest of the model" — which is exactly what a gun body
 * or a grip/stock/barrel attachment wants.
 *
 * Nothing here caches per-frame state on the instance: [applyBars] hands back an [AmmoBarState] that
 * the caller must pass to [restoreBars]. That matters because a single model instance is shared by
 * every stack using the same model file and can be rendered more than once per frame.
 */
internal class AmmoDisplayRenderer(
    private val baseModel: TreeBedrockModel,
    private val instance: TreeModelInstance,
    /** Indices of every bone under a `division*` root; empty for models without a reticle. */
    private val divisionBones: Set<Int> = emptySet(),
) {

    /**
     * Squashes every bone in [entries] along its configured axis to [progress] (0-1) and returns the
     * state to hand back to [restoreBars] and [renderBars] once rendering is done.
     *
     * Returning the state instead of stashing it in a field keeps this reentrant, which matters
     * because a single model instance is shared by every stack using the same model file and can be
     * rendered more than once per frame.
     *
     * Entries that carry a color are also hidden here, because they are drawn separately by
     * [renderBars] so they can take a tint, and a hidden bone drops its whole subtree from the model
     * pass. Visibility deliberately has this single owner: a scope draws `scope_body*` / `ocular*`
     * through its own immediate-bone helper long before the remaining pass runs, so hiding only
     * inside that pass would let those paths draw an untinted bar.
     *
     * Unlike the gun model, the attachment instances are never reset through `resetPose`, so a caller
     * that skips [restoreBars] would leak the squashed scale into every later render.
     */
    fun applyBars(entries: List<AmmoBarEntry>, progress: Float): AmmoBarState? {
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

    /** Restores everything [applyBars] captured. */
    fun restoreBars(state: AmmoBarState?) {
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
     * This deliberately does not flush, unlike the per-bone immediate path a scope uses. The model
     * pass has already queued geometry into the same buffers as part of one batch, so flushing here
     * would split that batch, and on the Oculus path it would drain everything else the caller had
     * buffered up.
     */
    fun renderBars(
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

            // The readout has to come off screen with the rest of its subtree when an ancestor is
            // hidden — a gun hides `oem_scope` once a scope attachment is fitted, and the bar is
            // several levels below it. The bone's own flag cannot answer that, because applyBars
            // cleared it a moment ago on purpose so the model pass would leave the bar out.
            if (!visibleInModel(index, includeSelf = false)) continue

            // applyBars hid this bone so the model pass would skip it, and the per-bone render bails
            // out on an invisible bone, so it has to be turned back on for this draw. restoreBars puts
            // the original value back.
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
     * Recomputed per render instead of cached: the readout is handed in by the caller each frame, and
     * a single instance of the owning model class is shared by every stack using the same model file.
     *
     * Nothing is filtered out here beyond a missing bone: which texts end up on screen is decided
     * where they are drawn. A text under a `division*` root goes out with the reticle, so the zoom
     * gate in the scope's division loops covers it, and one anchored anywhere else is drawn by the
     * scope's remaining pass — or, for a model with no reticle at all, by its only pass — regardless.
     *
     * On the scope aiming path this same list is what the division loops match on; the whole-model
     * pass draws its texts straight from the entries. That has no bearing on the gate above: third
     * person and the inventory draw the entire model, housing included, and the housing is what hides
     * a text mounted inside the tube from every angle it can be seen from.
     */
    fun buildTexts(
        entries: List<AmmoTextEntry>,
        count: Int,
        progress: Float
    ): List<AmmoText> {
        if (entries.isEmpty()) return emptyList()

        val texts = mutableListOf<AmmoText>()
        for (entry in entries) {
            val index = baseModel.getIndex(entry.bone)
            if (index < 0) continue
            // -1 is kept rather than filtered out: an anchor outside a division subtree cannot be
            // drawn alongside a reticle, but it still has to be drawn by the remaining pass, otherwise
            // it would be visible only in third person and the inventory.
            texts += AmmoText(entry, count, progress, divisionAnchorOf(index))
        }
        return texts
    }

    /** Draws [text] at its anchor bone. See the entry overload for the transform details. */
    fun renderText(text: AmmoText, poseStack: PoseStack, bufferSource: MultiBufferSource) {
        renderText(text.entry, text.count, text.progress, poseStack, bufferSource)
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
    fun renderText(
        entry: AmmoTextEntry,
        count: Int,
        progress: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource
    ) {
        val index = baseModel.getIndex(entry.bone)
        if (index < 0) return
        if (!visibleInModel(index, includeSelf = true)) return

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

    /**
     * Whether the model pass would draw the subtree that contains [boneIndex].
     *
     * [TreeBedrockModel.renderBone] bails out on an invisible bone *before* recursing into children,
     * so hiding an ancestor takes the whole subtree off the model. That is exactly how a gun removes
     * its built-in readout once a scope attachment is fitted — `GeoGunRenderer.renderOemScope` hides
     * `oem_scope` and every ammo bone the devotion declares sits below it. Neither of the two draw
     * helpers here inherits the rule, though: [renderText] builds its glyphs straight from the
     * anchor's transform without asking anything, and [renderBars] goes through
     * [TreeModelInstance.renderSingleBone], which tests the target bone's own flag and stops there.
     * Both therefore have to ask.
     *
     * The walk *stops* at a `division*` bone rather than testing it. Those are hidden the moment the
     * model is parsed, deliberately, so that the model pass leaves the reticle out — it is drawn later
     * through an immediate path that force-shows it. Reading that flag as "hidden" here would drop
     * every reticle-anchored readout, and on a scope the reticle is the thing that is supposed to be
     * visible while aiming. What gates those is the `divisionIndex` match plus the zoom check on the
     * aiming path, not this.
     *
     * [includeSelf] is `false` for a bar, whose own flag [applyBars] clears on purpose so the model
     * pass skips it and it can be drawn separately with a tint. Only its ancestors mean anything
     * there.
     */
    private fun visibleInModel(boneIndex: Int, includeSelf: Boolean): Boolean {
        var index = if (includeSelf) boneIndex else baseModel.bone(boneIndex).parentIndex()
        while (index >= 0) {
            if (index in divisionBones) return true
            val bone = instance.getBone(index) ?: return false
            if (!bone.visible) return false
            index = baseModel.bone(index).parentIndex()
        }
        return true
    }

    /**
     * Index of the division bone [textBoneIndex] hangs under, or `-1` when it is not inside a
     * `division*` subtree.
     *
     * This decides how the text becomes visible: a division bone is hidden in the whole-model pass
     * and only drawn where the reticle is drawn separately, so an anchor below one only shows up
     * while aiming down the sights, and only once the zoom has reached the scope's minimum. `-1`
     * means the anchor sits elsewhere in the model — usually on the scope body, or anywhere at all in
     * a model that has no reticle — and is drawn by the remaining pass instead, depth tested against
     * the body.
     */
    private fun divisionAnchorOf(textBoneIndex: Int): Int {
        if (divisionBones.isEmpty()) return -1
        var index = baseModel.bone(textBoneIndex).parentIndex()
        while (index >= 0) {
            if (index in divisionBones) return index
            index = baseModel.bone(index).parentIndex()
        }
        return -1
    }

    /**
     * One text to draw: the line itself, the count to expand it with, and the index of the division
     * bone it hangs under, which is what the scope's division loops match on.
     *
     * [divisionIndex] is `-1` when the anchor bone is not inside a `division*` subtree. Such a text
     * cannot be drawn next to a reticle, so it is drawn by the remaining pass instead — that is how a
     * model that hangs its readout off the scope body (rather than off the reticle) still shows the
     * text while aiming, and how a model with no reticle at all shows it everywhere. No division bone
     * ever has index `-1`, so the division guards stay correct without an extra check.
     */
    internal class AmmoText(
        val entry: AmmoTextEntry,
        val count: Int,
        /** Remaining magazine ratio, which the entry resolves its tiered color against. */
        val progress: Float,
        val divisionIndex: Int
    )

    /**
     * What [applyBars] has to give back: the bones it touched (`-1` where the model has no such
     * bone), the scales and visibility they had, and the resolved tint per bone, with [UNTINTED]
     * marking the entries that keep rendering as part of the model.
     */
    internal class AmmoBarState(
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
         * frame, since nothing resets an attachment instance's pose.
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
    }
}
