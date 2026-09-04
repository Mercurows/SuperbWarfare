package com.atsuishio.superbwarfare.client.model.attachment

import com.atsuishio.superbwarfare.client.renderer.scope.ScopeStencilRenderHelper
import com.atsuishio.superbwarfare.data.attachment.ScopeInfo
import com.atsuishio.superbwarfare.data.attachment.ScopeType
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import java.util.*
import java.util.regex.Pattern

class BedrockAttachmentModel(private val baseModel: TreeBedrockModel) {
    private val instance: TreeModelInstance = baseModel.createInstance()

    private val scopeBodyIndex: Int
    private val ocularRingIndex: Int
    private val ocularIndices = mutableListOf<Int>()
    private val isScopeOcular = mutableListOf<Boolean>()
    private val divisionIndices = mutableListOf<Int>()
    private val illuminatedBoneIndices: IntArray = baseModel.bones()
        .asSequence()
        .filter { it.name().endsWith(ILLUMINATED_SUFFIX) }
        .map { it.index() }
        .toList()
        .toIntArray()

    init {
        markIlluminatedBones()

        val oculars = TreeMap<Int, OcularEntry>()
        for (bone in baseModel.bones()) {
            val matcher = OCULAR_PATTERN.matcher(bone.name())
            if (!matcher.matches()) continue

            val num = matcher.group(3)?.toIntOrNull() ?: 1
            val isScope = OCULAR_SCOPE_NODE == matcher.group(1)
            oculars[num] = OcularEntry(bone.index(), isScope)
        }
        for (entry in oculars.values) {
            ocularIndices += entry.index
            isScopeOcular += entry.isScope
        }

        var divisionIndex = baseModel.getIndex(DIVISION_NODE)
        var divisionSuffix = 2
        while (divisionIndex >= 0) {
            addDivisionGeometry(divisionIndex)
            divisionIndex = baseModel.getIndex("${DIVISION_NODE}_$divisionSuffix")
            divisionSuffix++
        }

        scopeBodyIndex = baseModel.getIndex(SCOPE_BODY_NODE)
        ocularRingIndex = baseModel.getIndex(OCULAR_RING_NODE)
    }

    private fun addDivisionGeometry(divisionIndex: Int) {
        if (divisionIndex < 0) return

        val divisionBone = baseModel.bone(divisionIndex)
        if (divisionBone.hasQuads()) {
            divisionIndices += divisionIndex
            setBoneVisible(divisionIndex, false)
            return
        }

        val children = baseModel.bones()
            .filter { it.parentIndex() == divisionIndex && it.name().startsWith("${DIVISION_NODE}_") }
            .filter { it.hasQuads() }

        if (children.isEmpty()) {
            divisionIndices += divisionIndex
            setBoneVisible(divisionIndex, false)
            return
        }

        for (child in children) {
            divisionIndices += child.index()
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
        packedOverlay: Int
    ) {
        markIlluminatedBones()
        baseModel.renderToBuffer(
            instance,
            poseStack,
            bufferSource,
            RenderType.entityCutout(texture),
            BedrockModelRenderTypes.polyMeshCutout(texture),
            packedLight,
            packedOverlay,
            1f,
            1f,
            1f,
            1f,
            true
        )
    }

    fun needsStencil(info: ScopeInfo?): Boolean = info != null && ocularIndices.isNotEmpty()

    fun renderWithStencil(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        texture: ResourceLocation,
        packedLight: Int,
        partialTicks: Float,
        info: ScopeInfo
    ) {
        markIlluminatedBones()
        val quadType = RenderType.entityCutout(texture)
        val triangleType = BedrockModelRenderTypes.polyMeshCutout(texture)

        when (info.type) {
            ScopeType.SIGHT -> renderSight(poseStack, bufferSource, quadType, triangleType, packedLight)
            ScopeType.SCOPE -> renderScope(
                poseStack,
                bufferSource,
                quadType,
                triangleType,
                packedLight,
                partialTicks,
                info
            )
        }

        renderRemaining(poseStack, bufferSource, quadType, triangleType, packedLight)
    }

    private fun renderSight(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int
    ) {
        ScopeStencilRenderHelper.enableItemEntityStencilTest()
        RenderSystem.clearStencil(0)
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX)

        renderOcularStencil(poseStack, bufferSource, quadType, triangleType, light, false)
        renderDivisionOnly(poseStack, bufferSource, quadType, triangleType, light)

        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF)
        ScopeStencilRenderHelper.disableItemEntityStencilTest()

        if (scopeBodyIndex >= 0) {
            renderBoneImmediate(scopeBodyIndex, poseStack, bufferSource, quadType, triangleType, light)
        }
    }

    private fun renderScope(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int,
        partialTicks: Float,
        info: ScopeInfo
    ) {
        ScopeStencilRenderHelper.enableItemEntityStencilTest()
        RenderSystem.clearStencil(0)
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX)

        if (ocularRingIndex >= 0) {
            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF)
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
            renderBoneImmediate(ocularRingIndex, poseStack, bufferSource, quadType, triangleType, light)
        }

        renderOcularStencil(poseStack, bufferSource, quadType, triangleType, light, false)

        if (scopeBodyIndex >= 0) {
            RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF)
            renderBoneImmediate(scopeBodyIndex, poseStack, bufferSource, quadType, triangleType, light)
        }

        renderOcularAndDivision(poseStack, bufferSource, quadType, triangleType, light, partialTicks, info, false)

        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF)
        ScopeStencilRenderHelper.disableItemEntityStencilTest()
    }

    private fun renderOcularStencil(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int,
        selectScope: Boolean
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
        light: Int
    ) {
        if (divisionIndices.isEmpty()) return

        RenderSystem.disableDepthTest()
        for (i in divisionIndices.indices) {
            RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF)
            renderBoneImmediate(divisionIndices[i], poseStack, bufferSource, quadType, triangleType, light)
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
        info: ScopeInfo,
        selective: Boolean
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

        for (i in ocularIndices.indices) {
            if (i > Byte.MAX_VALUE) {
                throw IllegalArgumentException("Index of oculus is out of range for 127")
            }
            if (i >= divisionIndices.size) break

            if (selective && !isScopeOcular[i]) {
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF)
                renderBoneImmediate(divisionIndices[i], poseStack, bufferSource, quadType, triangleType, light)
            } else {
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF)
                renderBoneImmediate(ocularIndices[i], poseStack, bufferSource, quadType, triangleType, light)

                val b = (i + 1).inv() and 0xFF
                RenderSystem.stencilFunc(GL11.GL_EQUAL, b, 0xFF)
                renderBoneImmediate(divisionIndices[i], poseStack, bufferSource, quadType, triangleType, light)
            }
        }
    }

    private fun renderRemaining(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int
    ) {
        val hidden = mutableListOf<Int>()
        addSpecialIndex(hidden, scopeBodyIndex)
        addSpecialIndex(hidden, ocularRingIndex)
        hidden += ocularIndices
        hidden += divisionIndices

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
        flush(bufferSource, quadType, triangleType)

        for (i in hidden.indices) {
            instance.getBone(hidden[i])?.visible = originalVisible[i]
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
            val parentTransform = instance.getGlobalTransform(parentIndex)
            poseStack.last().pose().mul(parentTransform)
            poseStack.last().normal().mul(Matrix3f(parentTransform))
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

    private fun markIlluminatedBones() {
        for (index in illuminatedBoneIndices) {
            instance.getBone(index)?.illuminated = true
        }
    }

    private fun addSpecialIndex(list: MutableList<Int>, index: Int) {
        if (index >= 0) list += index
    }

    private data class OcularEntry(val index: Int, val isScope: Boolean)

    companion object {
        private const val SCOPE_BODY_NODE = "scope_body"
        private const val OCULAR_RING_NODE = "ocular_ring"
        private const val DIVISION_NODE = "division"
        private const val OCULAR_NODE = "ocular"
        private const val OCULAR_SIGHT_NODE = "ocular_sight"
        private const val OCULAR_SCOPE_NODE = "ocular_scope"
        private const val ILLUMINATED_SUFFIX = "_illuminated"
        private val OCULAR_PATTERN = Pattern.compile(
            "^($OCULAR_NODE|$OCULAR_SIGHT_NODE|$OCULAR_SCOPE_NODE)(_(\\d+))?$"
        )
    }
}
