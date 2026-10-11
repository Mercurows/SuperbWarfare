package com.atsuishio.superbwarfare.client.renderer.ammo

import com.atsuishio.superbwarfare.data.attachment.AmmoBarAxis
import com.atsuishio.superbwarfare.data.attachment.AmmoBarEntry
import com.atsuishio.superbwarfare.data.attachment.AmmoTextEntry
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture

/** 驱动一个 bedrock 模型实例上的弹药条与弹药文字 */
internal class AmmoDisplayRenderer(
    private val baseModel: TreeBedrockModel,
    private val instance: TreeModelInstance,
    /** 所有 `division*` 根下的骨骼下标；没有准星的模型传空集 */
    private val divisionBones: Set<Int> = emptySet(),
) {

    /** 按 [progress] 压扁 [entries] 里的每条弹药条，返回要交给 [restoreBars] 的状态 */
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

                // 不写任何缩放，骨骼保持模型给的；上面对它的记录与还原仍然要做，这样它才能被单独上色
                AmmoBarAxis.NONE -> {}
            }

            if (entries[i].isTinted()) {
                tints[i] = entries[i].colorAt(scale)
                bone.visible = false
            }
        }
        return if (anyBone) AmmoBarState(boneIndices, savedScales, savedVisible, tints) else null
    }

    /** 还原 [applyBars] 记录的一切 */
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

    /** 用各自解析出的颜色单独画出每条带颜色的弹药条 */
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

            // 祖先骨骼被隐藏时整棵子树都不该出现（比如装上瞄准镜后枪会隐藏 `oem_scope`，
            // 弹药条在它下面好几层），而骨骼自己的标记答不了这个问题 —— applyBars 刚刚
            // 特意清掉它，好让模型那一遍跳过这条弹药条
            if (!visibleInModel(index, includeSelf = false)) continue

            // applyBars 把它藏了起来，而按骨骼单独画的那条路径遇到不可见的骨骼会直接返回，
            // 所以这一遍得先打开，restoreBars 再把原值放回去
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

    /** 把每条配置的文字解析成骨骼下标与它所属的 division，骨骼不存在的不计入 */
    fun buildTexts(
        entries: List<AmmoTextEntry>,
        count: Int,
        progress: Float,
        range: Int = AmmoTextEntry.NO_RANGE,
        heat: Int = 0
    ): List<AmmoText> {
        if (entries.isEmpty()) return emptyList()

        val texts = mutableListOf<AmmoText>()
        for (entry in entries) {
            val index = baseModel.getIndex(entry.bone)
            if (index < 0) continue
            // -1 保留、不过滤：不在 division 子树下的锚点没法跟着准星画，但仍要由剩下那一遍画出来，
            // 否则它只会在第三人称和物品栏里可见
            texts += AmmoText(entry, count, progress, range, heat, divisionAnchorOf(index))
        }
        return texts
    }

    /** 在 [text] 的锚点骨骼上把它画出来 */
    fun renderText(text: AmmoText, poseStack: PoseStack, bufferSource: MultiBufferSource) {
        renderText(text.entry, text.count, text.progress, text.range, text.heat, poseStack, bufferSource)
    }

    /** 在锚点骨骼上画一行弹药文字，位置与朝向都由该骨骼给出 */
    fun renderText(
        entry: AmmoTextEntry,
        count: Int,
        progress: Float,
        range: Int,
        heat: Int,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource
    ) {
        val index = baseModel.getIndex(entry.bone)
        if (index < 0) return
        if (!visibleInModel(index, includeSelf = true)) return

        val text = entry.resolve(count, range, heat)
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
            entry.colorAt(progress, heat),
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

    /** 模型那一遍会不会画出包含 [boneIndex] 的子树 */
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

    /** [textBoneIndex] 所属的 division 骨骼下标，不在 `division*` 子树下时为 `-1` */
    private fun divisionAnchorOf(textBoneIndex: Int): Int {
        if (divisionBones.isEmpty()) return -1
        var index = baseModel.bone(textBoneIndex).parentIndex()
        while (index >= 0) {
            if (index in divisionBones) return index
            index = baseModel.bone(index).parentIndex()
        }
        return -1
    }

    /** 一条要画的文字：内容、展开用的数值，以及它所属的 division 骨骼下标 */
    internal class AmmoText(
        val entry: AmmoTextEntry,
        val count: Int,
        val progress: Float,
        val range: Int,
        val heat: Int,
        val divisionIndex: Int
    )

    /** [applyBars] 要交还的东西：它动过的骨骼、这些骨骼原来的缩放与可见性、每条解析出的颜色 */
    internal class AmmoBarState(
        val boneIndices: IntArray,
        val savedScales: FloatArray,
        val savedVisible: BooleanArray,
        val tints: IntArray
    )

    companion object {
        // Real tints are opaque ARGB, so the sign bit is free to mark "no tint configured".
        private const val UNTINTED = Int.MIN_VALUE

        /** [AmmoBarState.savedScales] 每条弹药条占用的浮点数：`x`、`y`、`z` */
        private const val SCALES_PER_BONE = 3

        /** 让字形方块的中心落在锚点骨骼上的字体空间 Y 偏移 */
        private const val GLYPH_BOX_CENTER = -3.5f
    }
}
