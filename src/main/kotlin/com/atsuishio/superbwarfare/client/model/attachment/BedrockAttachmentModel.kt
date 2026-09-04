package com.atsuishio.superbwarfare.client.model.attachment

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.LightTexture
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

/**
 * Code based on TACZ-RESPAWN
 */
class BedrockAttachmentModel(private val baseModel: TreeBedrockModel) {
    private val instance: TreeModelInstance = baseModel.createInstance()

    private val ocularEntries: List<OcularEntry>
    private val divisionIndices: MutableList<Int>
    private val divisionIlluminatedIndices: List<Int>
    private val scopeBodyIndex: Int
    private val ocularRingIndex: Int

    var isScope = false
    var isSight = false
    var scopeViewRadiusModifier = 1f
    var renderDivisionGeometry = true

    init {
        val oculars = TreeMap<Int, OcularEntry>()
        for (bone in baseModel.bones()) {
            val ocular = parseOcularBone(bone.name())
            if (ocular != null) {
                val (number, scopeOcular) = ocular
                oculars[number] = OcularEntry(bone.index(), scopeOcular)
            }
        }
        ocularEntries = oculars.values.toList()

        divisionIndices = mutableListOf()
        for (base in DIVISION_BASES) {
            var number = 1
            while (true) {
                val boneName = if (number == 1) base else "${base}_$number"
                val index = baseModel.getIndex(boneName)
                if (index < 0) break
                divisionIndices += index
                instance.getBone(index)?.visible = false
                number++
            }
        }
        divisionIlluminatedIndices = divisionIndices.mapNotNull { divisionIndex ->
            baseModel.bone(divisionIndex)
                ?.childBones()
                ?.firstOrNull { it.name().endsWith(ILLUMINATED_SUFFIX) }
                ?.index()
        }

        scopeBodyIndex = baseModel.getIndex(SCOPE_BODY_NODE)
        ocularRingIndex = baseModel.getIndex(OCULAR_RING_NODE)
    }

    fun getGlobalTransform(boneName: String): Matrix4f? {
        val index = baseModel.getIndex(boneName)
        return if (index >= 0) instance.getGlobalTransform(index) else null
    }

    fun needsStencil(): Boolean = (isScope || isSight) && ocularEntries.isNotEmpty()

    fun renderToBuffer(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        texture: ResourceLocation,
        packedLight: Int,
        packedOverlay: Int
    ) {
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

    fun renderWithStencil(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        texture: ResourceLocation,
        packedLight: Int,
        aimingProgress: Float
    ) {
        val quadType = RenderType.entityCutout(texture)
        val triangleType = BedrockModelRenderTypes.polyMeshCutout(texture)

        when {
            isScope && isSight -> renderBoth(
                poseStack,
                bufferSource,
                quadType,
                triangleType,
                packedLight,
                aimingProgress
            )

            isScope -> renderScope(poseStack, bufferSource, quadType, triangleType, packedLight, aimingProgress)
            isSight -> renderSight(poseStack, bufferSource, quadType, triangleType, packedLight)
        }
        renderRemaining(poseStack, bufferSource, quadType, triangleType, packedLight)
    }

    fun renderCrosshairIcon(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        crosshairTexture: ResourceLocation,
        colorArgb: Int,
        packedLight: Int
    ) {
        if (divisionIlluminatedIndices.isEmpty()) return

        val renderType = RenderType.eyes(crosshairTexture)
        val red = ((colorArgb shr 16) and 0xFF) / 255f
        val green = ((colorArgb shr 8) and 0xFF) / 255f
        val blue = (colorArgb and 0xFF) / 255f

        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        for (boneIndex in divisionIlluminatedIndices) {
            val boneDefinition = baseModel.bone(boneIndex) ?: continue
            val bone = instance.getBone(boneIndex) ?: continue
            poseStack.pushPose()
            instance.mulParentGlobalTransform(poseStack, boneIndex)
            bone.translateAndRotateAndScale(poseStack)

            val consumer = bufferSource.getBuffer(renderType)
            val cube = boneDefinition.cubes().firstOrNull()
            if (cube != null) {
                val x = cube.x()
                val y = cube.y()
                val z = cube.z() + cube.depth()
                emitCrosshairQuad(
                    consumer,
                    poseStack.last().pose(),
                    x,
                    y,
                    x + cube.width(),
                    y + cube.height(),
                    z,
                    red,
                    green,
                    blue
                )
            } else {
                emitCrosshairQuad(
                    consumer,
                    poseStack.last().pose(),
                    -0.25f,
                    -0.25f,
                    0.25f,
                    0.25f,
                    0f,
                    red,
                    green,
                    blue
                )
            }
            poseStack.popPose()
        }
        bufferSource.endBatch(renderType)
        RenderSystem.depthMask(true)
        RenderSystem.enableDepthTest()
    }

    private fun emitCrosshairQuad(
        consumer: VertexConsumer,
        matrix: Matrix4f,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        z: Float,
        red: Float,
        green: Float,
        blue: Float
    ) {
        consumer.vertex(matrix, x0, y0, z)
            .color(red, green, blue, 1f)
            .uv(0f, 0f)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(0f, 0f, 1f)
            .endVertex()
        consumer.vertex(matrix, x1, y0, z)
            .color(red, green, blue, 1f)
            .uv(1f, 0f)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(0f, 0f, 1f)
            .endVertex()
        consumer.vertex(matrix, x1, y1, z)
            .color(red, green, blue, 1f)
            .uv(1f, 1f)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(0f, 0f, 1f)
            .endVertex()
        consumer.vertex(matrix, x0, y1, z)
            .color(red, green, blue, 1f)
            .uv(0f, 1f)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(0f, 0f, 1f)
            .endVertex()
    }

    private fun renderScope(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        packedLight: Int,
        aimingProgress: Float
    ) {
        enableStencil()
        clearStencil()

        if (ocularRingIndex >= 0) {
            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF)
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
            renderBoneImmediate(ocularRingIndex, poseStack, bufferSource, quadType, triangleType, packedLight)
        }

        renderOcularStencil(poseStack, bufferSource, quadType, triangleType, packedLight, false)

        if (scopeBodyIndex >= 0) {
            RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF)
            renderBoneImmediate(scopeBodyIndex, poseStack, bufferSource, quadType, triangleType, packedLight)
        }

        renderOcularAndDivision(
            poseStack,
            bufferSource,
            quadType,
            triangleType,
            packedLight,
            aimingProgress,
            false
        )
        disableStencil()
    }

    private fun renderSight(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        packedLight: Int
    ) {
        enableStencil()
        clearStencil()
        renderOcularStencil(poseStack, bufferSource, quadType, triangleType, packedLight, false)
        renderDivisionOnly(poseStack, bufferSource, quadType, triangleType, packedLight)
        disableStencil()

        if (scopeBodyIndex >= 0) {
            renderBoneImmediate(scopeBodyIndex, poseStack, bufferSource, quadType, triangleType, packedLight)
        }
    }

    private fun renderBoth(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        packedLight: Int,
        aimingProgress: Float
    ) {
        enableStencil()
        clearStencil()

        if (ocularRingIndex >= 0) {
            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF)
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
            renderBoneImmediate(ocularRingIndex, poseStack, bufferSource, quadType, triangleType, packedLight)
        }

        renderOcularStencil(poseStack, bufferSource, quadType, triangleType, packedLight, true)

        if (scopeBodyIndex >= 0) {
            RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xFF)
            renderBoneImmediate(scopeBodyIndex, poseStack, bufferSource, quadType, triangleType, packedLight)
        }

        renderOcularStencil(poseStack, bufferSource, quadType, triangleType, packedLight, false)
        renderOcularAndDivision(
            poseStack,
            bufferSource,
            quadType,
            triangleType,
            packedLight,
            aimingProgress,
            true
        )
        disableStencil()
    }

    private fun renderRemaining(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        packedLight: Int
    ) {
        val hidden = mutableListOf<Int>()
        addSpecialIndex(hidden, scopeBodyIndex)
        addSpecialIndex(hidden, ocularRingIndex)
        hidden += ocularEntries.map { it.index }
        hidden += divisionIndices

        val originalVisibility = BooleanArray(hidden.size)
        for (i in hidden.indices) {
            val bone = instance.getBone(hidden[i]) ?: continue
            originalVisibility[i] = bone.visible
            bone.visible = false
        }

        baseModel.renderToBuffer(
            instance,
            poseStack,
            bufferSource,
            quadType,
            triangleType,
            packedLight,
            OverlayTexture.NO_OVERLAY
        )
        flush(bufferSource, quadType, triangleType)

        for (i in hidden.indices) {
            val bone = instance.getBone(hidden[i]) ?: continue
            bone.visible = originalVisibility[i]
        }
    }

    private fun addSpecialIndex(target: MutableList<Int>, index: Int) {
        if (index >= 0) {
            target += index
        }
    }

    private fun renderOcularStencil(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        packedLight: Int,
        selectScope: Boolean
    ) {
        if (ocularEntries.isEmpty()) return

        RenderSystem.colorMask(false, false, false, false)
        RenderSystem.depthMask(false)
        RenderSystem.stencilMask(0xFF)
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE)

        for (i in ocularEntries.indices.reversed()) {
            if (selectScope == ocularEntries[i].isScopeOcular) {
                RenderSystem.stencilFunc(GL11.GL_GREATER, i + 1, 0xFF)
                renderBoneImmediate(
                    ocularEntries[i].index,
                    poseStack,
                    bufferSource,
                    quadType,
                    triangleType,
                    packedLight
                )
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
        packedLight: Int
    ) {
        if (!renderDivisionGeometry) return
        if (divisionIndices.isEmpty()) return

        RenderSystem.disableDepthTest()
        for (i in divisionIndices.indices) {
            RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF)
            renderBoneImmediate(divisionIndices[i], poseStack, bufferSource, quadType, triangleType, packedLight)
        }
        RenderSystem.enableDepthTest()
    }

    private fun renderOcularAndDivision(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        packedLight: Int,
        aimingProgress: Float,
        selective: Boolean
    ) {
        if (ocularEntries.isEmpty()) return

        val builder = Tesselator.getInstance().builder
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INVERT)
        RenderSystem.colorMask(false, false, false, false)
        RenderSystem.depthMask(false)

        val radius = 80f * scopeViewRadiusModifier * aimingProgress.coerceIn(0f, 1f)
        RenderSystem.setShader(GameRenderer::getPositionColorShader)

        for (i in ocularEntries.indices) {
            if (selective && !ocularEntries[i].isScopeOcular) {
                continue
            }

            RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF)
            val center = getBoneCenter(poseStack, ocularEntries[i].index)
            val centerX = center.x * 16f * 90f
            val centerY = center.y * 16f * 90f

            builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR)
            builder.vertex(centerX.toDouble(), centerY.toDouble(), -90.0).color(255, 255, 255, 255).endVertex()
            for (j in 0..90) {
                val angle = j * (Math.PI.toFloat() * 2f) / 90f
                val sin = Mth.sin(angle)
                val cos = Mth.cos(angle)
                builder.vertex((centerX + cos * radius).toDouble(), (centerY + sin * radius).toDouble(), -90.0)
                    .color(255, 255, 255, 255)
                    .endVertex()
            }
            BufferUploader.drawWithShader(builder.end())
        }

        RenderSystem.depthMask(true)
        RenderSystem.colorMask(true, true, true, true)
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)

        for (i in ocularEntries.indices) {
            if (i > 127) {
                throw IllegalArgumentException("Index of oculus is out of range for 127")
            }
            if (selective && !ocularEntries[i].isScopeOcular) {
                if (renderDivisionGeometry && i < divisionIndices.size) {
                    RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF)
                    renderBoneImmediate(
                        divisionIndices[i],
                        poseStack,
                        bufferSource,
                        quadType,
                        triangleType,
                        packedLight
                    )
                }
            } else {
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF)
                renderBoneImmediate(
                    ocularEntries[i].index,
                    poseStack,
                    bufferSource,
                    quadType,
                    triangleType,
                    packedLight
                )

                if (renderDivisionGeometry && i < divisionIndices.size) {
                    val inverted = (i + 1).inv() and 0xFF
                    RenderSystem.stencilFunc(GL11.GL_EQUAL, inverted, 0xFF)
                    renderBoneImmediate(
                        divisionIndices[i],
                        poseStack,
                        bufferSource,
                        quadType,
                        triangleType,
                        packedLight
                    )
                }
            }
        }
    }

    private fun renderBoneImmediate(
        boneIndex: Int,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType,
        packedLight: Int
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

        val quadBuffer = bufferSource.getBuffer(quadType)
        baseModel.renderBone(
            instance,
            boneIndex,
            poseStack,
            quadBuffer,
            packedLight,
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
            packedLight,
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

    private fun getBoneCenter(poseStack: PoseStack, boneIndex: Int): Vector3f {
        val matrix = Matrix4f(poseStack.last().pose()).mul(instance.getGlobalTransform(boneIndex))
        return matrix.getTranslation(Vector3f())
    }

    private fun flush(
        bufferSource: MultiBufferSource.BufferSource,
        quadType: RenderType,
        triangleType: RenderType
    ) {
        bufferSource.endBatch(quadType)
        bufferSource.endBatch(triangleType)
    }

    private fun enableStencil() {
        RenderSystem.assertOnRenderThread()
        Minecraft.getInstance().mainRenderTarget.enableStencil()
        GL11.glEnable(GL11.GL_STENCIL_TEST)
    }

    private fun disableStencil() {
        RenderSystem.assertOnRenderThread()
        GL11.glDisable(GL11.GL_STENCIL_TEST)
    }

    private fun clearStencil() {
        RenderSystem.clearStencil(0)
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX)
    }

    private data class OcularEntry(
        val index: Int,
        val isScopeOcular: Boolean
    )

    companion object {
        private const val SCOPE_BODY_NODE = "scope_body"
        private const val OCULAR_RING_NODE = "ocular_ring"
        private const val ILLUMINATED_SUFFIX = "_illuminated"
        private val OCULAR_BASES = listOf("ocular", "ocular_sight", "ocular_scope")
        private val DIVISION_BASES = listOf("division")

        private fun parseOcularBone(boneName: String): Pair<Int, Boolean>? {
            for (base in OCULAR_BASES) {
                if (boneName == base) {
                    return 1 to (base == "ocular_scope")
                }
                if (boneName.startsWith("${base}_")) {
                    val number = boneName.substringAfterLast('_').toIntOrNull() ?: continue
                    return number to (base == "ocular_scope")
                }
            }
            return null
        }
    }
}
