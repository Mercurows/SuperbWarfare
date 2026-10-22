package com.atsuishio.superbwarfare.client.model.attachment

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.charm.CharmRig
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoDisplayRenderer
import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoDisplayRenderer.AmmoBarState
import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoDisplayRenderer.AmmoText
import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoReadout
import com.atsuishio.superbwarfare.client.renderer.scope.ScopeLensPostProcessor
import com.atsuishio.superbwarfare.client.renderer.scope.ScopeLensShader
import com.atsuishio.superbwarfare.client.renderer.scope.ScopeStencilRenderHelper
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.attachment.ScopeMode
import com.atsuishio.superbwarfare.data.attachment.ScopeType
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel
import com.maydaymemory.mae.basic.Pose
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
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import java.util.regex.Pattern

class BedrockAttachmentModel(internal val baseModel: TreeBedrockModel) {
    internal val instance: TreeModelInstance = baseModel.createInstance()

    fun applyPose(pose: Pose) {
        instance.applyPose(pose)
    }

    fun getBindPose(): Pose = baseModel.bindPose

    fun resetPose() {
        instance.resetPose()
        markIlluminatedBones()
    }

    private val ammo: AmmoDisplayRenderer by lazy {
        AmmoDisplayRenderer(baseModel, instance, divisionGroups.values.flatten().toHashSet())
    }

    private val defaultScopeBodyIndex: Int
    private val dynamicDivisionIndex: Int
    private val ocularRingIndices = mutableMapOf<Int, Int>()
    private val ocularIndicesByGroup = mutableMapOf<Int, List<Int>>()
    private val isScopeOcularByGroup = mutableMapOf<Int, List<Boolean>>()
    private val shaderIndicesByGroup = mutableMapOf<Int, Int>()
    private val surfaceRadiusCache = mutableMapOf<Int, Float>()
    private val modeScopeBodyGroups = mutableMapOf<Int, List<Int>>()
    private val divisionGroups = mutableMapOf<String, List<Int>>()
    private val bindGlobalTransformCache = mutableMapOf<String, Matrix4f?>()
    private val illuminatedBoneIndices: IntArray = baseModel.bones()
        .asSequence()
        .filter { it.name().endsWith(ILLUMINATED_SUFFIX) }
        .map { it.index() }
        .toList()
        .toIntArray()

    /**
     * 手部骨骼索引，与 `GeoGunModel` 是同一对约定名
     */
    private val leftHandBoneIndex: Int = baseModel.getIndex(LEFT_HAND_BONE)
    private val rightHandBoneIndex: Int = baseModel.getIndex(RIGHT_HAND_BONE)

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

        val shaderPattern = Pattern.compile("^${SHADER_NODE}(_(\\d+))?$")
        for (bone in baseModel.bones()) {
            val matcher = shaderPattern.matcher(bone.name())
            if (!matcher.matches()) continue
            val num = matcher.group(2)?.toIntOrNull() ?: 0
            shaderIndicesByGroup[num] = bone.index()
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

    fun getBindGlobalTransform(boneName: String): Matrix4f? {
        bindGlobalTransformCache[boneName]?.let { return it }
        val index = baseModel.getIndex(boneName)
        if (index < 0) return null

        val currentPose = instance.pose
        instance.resetPose()
        val transform = Matrix4f(instance.getGlobalTransform(index))
        instance.applyPose(currentPose)
        bindGlobalTransformCache[boneName] = transform
        return transform
    }

    fun getBonePivot(boneName: String): Vector3f? {
        val definition = instance.getBone(boneName)?.definition() ?: return null
        return Vector3f(definition.pivotX(), definition.pivotY(), definition.pivotZ())
    }

    /**
     * 骨骼上某个 locator 的全局变换（模型空间，单位是**方块**）
     */
    fun getLocatorTransform(locatorName: String): Matrix4f? = instance.getLocatorTransform(locatorName)

    /**
     * 吊坠骨架：`string` / `charm` 两根分组骨骼 + 摆点/摆长/静止方向
     */
    fun charmRig(): CharmRig? {
        if (!charmRigResolved) {
            charmRigResolved = true
            charmRig = CharmRig.resolve(this)
            val rig = charmRig
            if (rig == null) {
                Mod.LOGGER.warn("Charm model has no usable 'string'/'charm' bone groups, it will not swing")
            } else {
                Mod.LOGGER.info(
                    "Charm rig resolved: pivot=({}, {}, {}) length={} restDir=({}, {}, {})",
                    rig.pivot.x, rig.pivot.y, rig.pivot.z, rig.length,
                    rig.restDir.x, rig.restDir.y, rig.restDir.z
                )
            }
        }
        return charmRig
    }

    private var charmRigResolved = false
    private var charmRig: CharmRig? = null

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

        hideHandBones()
        restoreScopeBodyVisibility()
        markIlluminatedBones()
        val ammoBarState = ammo.applyBars(readout.bars, readout.progress)
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
        ammo.renderBars(ammoBarState, poseStack, bufferSource, quadType, triangleType, packedLight, true)
        ammo.restoreBars(ammoBarState)

        for (entry in readout.texts) {
            ammo.renderText(
                entry,
                readout.count,
                readout.progress,
                readout.range,
                readout.heat,
                poseStack,
                bufferSource
            )
        }

        for (i in hiddenOculars.indices) {
            instance.getBone(hiddenOculars[i])?.visible = originalOcularVisibility[i]
        }
    }

    fun needsStencil(info: ScopeMode?): Boolean =
        info != null && ocularIndicesFor(info).isNotEmpty()

    fun hasLensSurface(info: ScopeMode): Boolean {
        val index = shaderBoneIndex(info)
        return index >= 0 && surfaceRadius(index) > 1.0e-4f
    }

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
        val ammoBarState = ammo.applyBars(readout.bars, readout.progress)
        val texts = ammo.buildTexts(
            readout.texts, readout.count, readout.progress, readout.range, readout.heat
        )
        val quadType = RenderType.entityTranslucent(texture)
        val triangleType = BedrockModelRenderTypes.polyMeshCutout(texture)

        when (info.type) {
            ScopeType.SIGHT -> renderSight(
                poseStack,
                bufferSource,
                texture,
                quadType,
                triangleType,
                packedLight,
                info,
                texts
            )

            ScopeType.SCOPE -> renderScope(
                poseStack,
                bufferSource,
                texture,
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

        ammo.restoreBars(ammoBarState)
    }

    private fun renderSight(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        texture: ResourceLocation,
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
            texture,
            quadType,
            triangleType,
            light,
            info,
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
        texture: ResourceLocation,
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
            texture,
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
        texture: ResourceLocation,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int,
        info: ScopeMode,
        divisions: List<Int>,
        texts: List<AmmoText> = emptyList()
    ) {
        val count = if (info.thermal) ocularIndicesFor(info).size else divisions.size
        if (count == 0) return
        if (ClientEventHandler.zoomTime < DIVISION_MIN_ZOOM) return

        RenderSystem.disableDepthTest()
        withShaderBonesHidden {
            for (i in 0 until count) {
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF)
                val division = divisions.getOrNull(i)
                if (info.thermal && ScopeLensPostProcessor.deferThermalSight(
                        i + 1, info.thermalBackgroundColor?.get(), info.thermalEntityColor?.get()
                    ) { deferred ->
                        if (division != null) {
                            renderDivisionVertices(division, poseStack, deferred, texture, quadType, triangleType, light)
                            for (text in texts) {
                                if (text.divisionIndex == division) ammo.renderText(text, poseStack, deferred)
                            }
                        }
                    }
                ) continue
                if (division == null) continue
                renderDivisionImmediate(division, poseStack, bufferSource, texture, quadType, triangleType, light)
                // Same stencil value and depth state as the reticle it sits next to.
                for (text in texts) {
                    if (text.divisionIndex == division) {
                        ammo.renderText(text, poseStack, bufferSource)
                    }
                }
            }
        }
        RenderSystem.enableDepthTest()
    }

    private fun renderOcularAndDivision(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        texture: ResourceLocation,
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
            texture,
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
        texture: ResourceLocation,
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

        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INVERT)
        RenderSystem.colorMask(false, false, false, false)
        RenderSystem.depthMask(false)

        val progress = ClientEventHandler.aimingProgress(ClientEventHandler.zoomTime)
        val rad = 80f * info.viewRadiusModifier * progress
        val window = Minecraft.getInstance().window
        val projectionMatrix = Matrix4f(RenderSystem.getProjectionMatrix())
        val modelViewMatrix = Matrix4f(RenderSystem.getModelViewMatrix())

        val modelViewStack = RenderSystem.getModelViewStack()
        modelViewStack.pushMatrix()
        modelViewStack.identity()
        RenderSystem.applyModelViewMatrix()
        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        for (i in ocularIndices.indices) {
            if (selective && !isScopeOcular[i]) continue

            RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF)
            val ocularCenter = getBoneCenter(poseStack, ocularIndices[i], modelViewMatrix)
            val depth = -ocularCenter.z()
            if (!depth.isFinite() || depth <= 0f) continue

            val radiusX = rad * depth * 2f /
                    (projectionMatrix.m00() * window.guiScaledWidth)
            val radiusY = rad * depth * 2f /
                    (projectionMatrix.m11() * window.guiScaledHeight)

            val builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLE_FAN,
                DefaultVertexFormat.POSITION_COLOR
            )
            builder
                .addVertex(ocularCenter.x(), ocularCenter.y(), ocularCenter.z())
                .setColor(255, 255, 255, 255)
            for (j in 0..90) {
                val angle = j * ((Math.PI * 2.0) / 90.0)
                val sin = Mth.sin(angle.toFloat())
                val cos = Mth.cos(angle.toFloat())
                builder.addVertex(
                    ocularCenter.x() + cos * radiusX,
                    ocularCenter.y() + sin * radiusY,
                    ocularCenter.z()
                )
                    .setColor(255, 255, 255, 255)
            }
            BufferUploader.drawWithShader(builder.build()!!)
        }
        modelViewStack.popMatrix()
        RenderSystem.applyModelViewMatrix()

        val ringIndex = ocularRingIndex(info)
        if (ringIndex >= 0 && ClientEventHandler.zoomTime > 0.8) {
            RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF)
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE)
            renderBoneImmediate(ringIndex, poseStack, bufferSource, quadType, triangleType, light)
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
                withShaderBonesHidden {
                    renderDivisionImmediate(divisions[i], poseStack, bufferSource, texture, quadType, triangleType, light)
                }
            } else {
                RenderSystem.stencilFunc(GL11.GL_EQUAL, i + 1, 0xFF)
                renderBoneImmediate(ocularIndices[i], poseStack, bufferSource, quadType, triangleType, light)

                val b = (i + 1).inv() and 0xFF
                val shaderBone = shaderBoneIndex(info)
                val lensSurface = if (shaderBone >= 0) getLensSurface(poseStack, shaderBone) else null
                RenderSystem.stencilFunc(GL11.GL_EQUAL, b, 0xFF)
                if (info.thermal && lensSurface == null) {
                    if (ScopeLensPostProcessor.deferThermalSight(
                            b, info.thermalBackgroundColor?.get(), info.thermalEntityColor?.get()
                        ) { deferred ->
                            if (divisionDue) {
                                withShaderBonesHidden {
                                    renderDivisionVertices(divisions[i], poseStack, deferred, texture, quadType, triangleType, light)
                                }
                                for (text in texts) {
                                    if (text.divisionIndex == divisions[i]) {
                                        ammo.renderText(text, poseStack, deferred)
                                    }
                                }
                            }
                        }
                    ) continue
                }
                if (shaderBone >= 0 && lensSurface != null) {
                    val magnification = if (DisplayConfig.LENS_ZOOM_RATIO.get() > 0.0) {
                        val progress = ClientEventHandler.aimingProgress(ClientEventHandler.zoomTime)
                        1f + progress * (ClientEventHandler.customZoom.coerceAtLeast(1.0) - 1.0).toFloat()
                    } else {
                        1f
                    }
                    val tint = info.lensTint
                    if (ShaderCompat.isShaderPackActive() || info.thermal) {
                        if (ScopeLensPostProcessor.defer(
                                b, lensSurface, info.lensEdgeWidth, magnification,
                                tint?.get() ?: 0xFFFFFF,
                                if (tint == null) 0f else info.lensTintStrength,
                                { surfaceBuffer -> buildLensSurface(poseStack, shaderBone, light, surfaceBuffer) },
                                { deferredBuffers ->
                                    if (divisionDue) {
                                        withShaderBonesHidden {
                                            renderDivisionVertices(divisions[i], poseStack, deferredBuffers, texture, quadType, triangleType, light)
                                        }
                                        for (text in texts) {
                                            if (text.divisionIndex == divisions[i]) {
                                                ammo.renderText(text, poseStack, deferredBuffers)
                                            }
                                        }
                                    }
                                },
                                thermal = info.thermal,
                                thermalBackgroundColor = info.thermalBackgroundColor?.get(),
                                thermalEntityColor = info.thermalEntityColor?.get()
                            )
                        ) continue
                    } else if (ScopeLensShader.beginSurface(
                            lensSurface.x(), lensSurface.y(), lensSurface.z(),
                            info.lensEdgeWidth, magnification,
                            tint?.get() ?: 0xFFFFFF,
                            if (tint == null) 0f else info.lensTintStrength
                        )
                    ) {
                        renderLensSurface(poseStack, shaderBone, light)
                        ScopeLensShader.endSurface()
                    } else {
                        renderLensFallback(poseStack, shaderBone, info.lensEdgeWidth)
                    }
                }

                if (!divisionDue) continue
                RenderSystem.stencilFunc(GL11.GL_EQUAL, b, 0xFF)
                withShaderBonesHidden {
                    renderDivisionImmediate(divisions[i], poseStack, bufferSource, texture, quadType, triangleType, light)
                }
                for (text in texts) {
                    if (text.divisionIndex == divisions[i]) {
                        ammo.renderText(text, poseStack, bufferSource)
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

    private fun shaderBoneIndex(info: ScopeMode): Int {
        return shaderIndicesByGroup[info.index] ?: shaderIndicesByGroup[0] ?: -1
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
        ammo.renderBars(ammoBarState, poseStack, bufferSource, quadType, triangleType, light, false)
        flush(bufferSource, quadType, triangleType)

        for (text in texts) {
            if (text.divisionIndex < 0) {
                ammo.renderText(text, poseStack, bufferSource)
            }
        }
    }

    private fun surfaceRadius(boneIndex: Int): Float {
        return surfaceRadiusCache.getOrPut(boneIndex) {
            val cubeTransform = Matrix4f()
            val point = Vector3f()
            var maxSquared = 0f
            for (cube in baseModel.bone(boneIndex).cubes()) {
                cubeTransform.identity()
                val pivot = cube.pivot()
                val rotation = cube.rotation()
                if (pivot != null && rotation != null) {
                    cubeTransform.translate(pivot[0], pivot[1], pivot[2])
                    cubeTransform.rotate(rotation)
                    cubeTransform.translate(-pivot[0], -pivot[1], -pivot[2])
                }
                for (corner in 0 until 8) {
                    point.set(
                        cube.x() + if (corner and 1 != 0) cube.width() else 0f,
                        cube.y() + if (corner and 2 != 0) cube.height() else 0f,
                        cube.z() + if (corner and 4 != 0) cube.depth() else 0f
                    ).mulPosition(cubeTransform)
                    val squared = point.x * point.x + point.y * point.y
                    if (squared > maxSquared) maxSquared = squared
                }
            }
            Mth.sqrt(maxSquared)
        }
    }

    private fun getLensSurface(poseStack: PoseStack, boneIndex: Int): Vector3f? {
        val radius = surfaceRadius(boneIndex)
        if (radius <= 1.0e-4f) return null

        val matrix = Matrix4f(poseStack.last().pose()).mul(instance.getGlobalTransform(boneIndex))
        val center = matrix.getTranslation(Vector3f())
        val rim = Vector3f(radius, 0f, 0f).mulPosition(matrix)
        val modelView = RenderSystem.getModelViewMatrix()
        center.mulPosition(modelView)
        rim.mulPosition(modelView)

        val dx = rim.x - center.x
        val dy = rim.y - center.y
        val viewRadius = Mth.sqrt(dx * dx + dy * dy)
        if (viewRadius <= 1.0e-4f) return null

        return Vector3f(center.x, center.y, viewRadius)
    }

    // The lens is one flat disc in the shader bone's own frame. Rendering the bone's cubes instead
    // would shade every overlapping cube separately, so the vignette alpha compounds into visible
    // seams wherever two bars cross.
    private fun renderLensSurface(poseStack: PoseStack, boneIndex: Int, light: Int) {
        val radius = surfaceRadius(boneIndex)
        if (radius <= 1.0e-4f) return
        val builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.NEW_ENTITY)
        BufferUploader.drawWithShader(buildLensSurface(poseStack, boneIndex, light, builder))
    }

    private fun buildLensSurface(
        poseStack: PoseStack, boneIndex: Int, light: Int, buffer: ByteBufferBuilder
    ): MeshData = buildLensSurface(
        poseStack, boneIndex, light,
        BufferBuilder(buffer, VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.NEW_ENTITY)
    )

    /**
     * 只往传进来的 [builder] 写顶点，不自己上传：1.21 的 [BufferBuilder] 是一次性的，
     * 由调用方决定立刻 [BufferUploader.drawWithShader] 还是交给 [ScopeLensPostProcessor] 延后绘制。
     */
    private fun buildLensSurface(
        poseStack: PoseStack, boneIndex: Int, light: Int, builder: BufferBuilder
    ): MeshData {
        val radius = surfaceRadius(boneIndex)

        poseStack.pushPose()
        instance.mulGlobalTransform(poseStack, boneIndex)
        val matrix = poseStack.last().pose()

        lensVertex(builder, matrix, 0f, 0f, light)
        for (i in 0..LENS_SEGMENTS) {
            val angle = i * ((Math.PI * 2.0) / LENS_SEGMENTS)
            lensVertex(
                builder, matrix,
                Mth.cos(angle.toFloat()) * radius,
                Mth.sin(angle.toFloat()) * radius,
                light
            )
        }
        val mesh = builder.buildOrThrow()
        poseStack.popPose()
        return mesh
    }

    // Fallback when the lens shader is unavailable: bake the vignette into vertex colours.
    // Rings are spaced quadratically to put more geometry where the falloff is steepest.
    private fun renderLensFallback(poseStack: PoseStack, boneIndex: Int, edgeWidth: Float) {
        val radius = surfaceRadius(boneIndex)
        if (radius <= 1.0e-4f) return

        RenderSystem.setShader(GameRenderer::getPositionColorShader)
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)

        poseStack.pushPose()
        instance.mulGlobalTransform(poseStack, boneIndex)
        val matrix = poseStack.last().pose()

        val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
        var innerFraction = 0f
        var innerAlpha = ScopeLensShader.edgeAlpha(0f, edgeWidth)
        for (ring in 1..LENS_FALLBACK_RINGS) {
            val t = ring.toFloat() / LENS_FALLBACK_RINGS
            val outerFraction = 1f - (1f - t) * (1f - t)
            val outerAlpha = ScopeLensShader.edgeAlpha(outerFraction, edgeWidth)
            val innerRadius = radius * innerFraction
            val outerRadius = radius * outerFraction
            for (segment in 0 until LENS_SEGMENTS) {
                val angle0 = segment * LENS_STEP
                val angle1 = angle0 + LENS_STEP
                val cos0 = Mth.cos(angle0)
                val sin0 = Mth.sin(angle0)
                val cos1 = Mth.cos(angle1)
                val sin1 = Mth.sin(angle1)
                lensFallbackVertex(builder, matrix, innerRadius * cos0, innerRadius * sin0, innerAlpha)
                lensFallbackVertex(builder, matrix, outerRadius * cos0, outerRadius * sin0, outerAlpha)
                lensFallbackVertex(builder, matrix, outerRadius * cos1, outerRadius * sin1, outerAlpha)
                lensFallbackVertex(builder, matrix, innerRadius * cos1, innerRadius * sin1, innerAlpha)
            }
            innerFraction = outerFraction
            innerAlpha = outerAlpha
        }
        BufferUploader.drawWithShader(builder.buildOrThrow())
        poseStack.popPose()

        RenderSystem.depthMask(true)
        RenderSystem.enableDepthTest()
    }

    private fun lensFallbackVertex(
        builder: VertexConsumer,
        matrix: Matrix4f,
        x: Float,
        y: Float,
        alpha: Float
    ) {
        builder.addVertex(matrix, x, y, 0f)
            .setColor(0f, 0f, 0f, alpha)
    }

    private fun lensVertex(builder: VertexConsumer, matrix: Matrix4f, x: Float, y: Float, light: Int) {
        builder.addVertex(matrix, x, y, 0f)
            .setColor(255, 255, 255, 255)
            .setUv(0f, 0f)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(0f, 0f, 1f)
    }

    private fun withShaderBonesHidden(action: () -> Unit) {
        if (shaderIndicesByGroup.isEmpty()) {
            action()
            return
        }

        val bones = shaderIndicesByGroup.values.mapNotNull { instance.getBone(it) }
        val originalVisibility = BooleanArray(bones.size)
        for (i in bones.indices) {
            originalVisibility[i] = bones[i].visible
            bones[i].visible = false
        }

        try {
            action()
        } finally {
            for (i in bones.indices) {
                bones[i].visible = originalVisibility[i]
            }
        }
    }

    private fun renderDivisionImmediate(
        boneIndex: Int,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource.BufferSource,
        texture: ResourceLocation,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int
    ) {
        renderDivisionVertices(boneIndex, poseStack, bufferSource, texture, quadType, triangleType, light)
        // Every material must be drawn before the caller changes the stencil test.
        flush(bufferSource, quadType, triangleType,
            ModRenderTypes.scopeReticle(texture), ModRenderTypes.scopeReticleMesh(texture))
    }

    private fun renderDivisionVertices(
        boneIndex: Int,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        texture: ResourceLocation,
        quadType: RenderType,
        triangleType: RenderType,
        light: Int
    ) {
        if (boneIndex < 0) return
        val illuminated = mutableListOf<Int>()
        fun collect(index: Int) {
            val bone = instance.getBone(index) ?: return
            // Like renderBoneVertices, only the requested root bypasses its visibility flag.
            if (index != boneIndex && !bone.visible) return
            val definition = baseModel.bone(index)
            if (definition.name().endsWith(ILLUMINATED_SUFFIX)) {
                illuminated += index
                return // Illumination already propagates to this bone's descendants in SBM.
            }
            for (child in definition.children()) collect(child)
        }
        collect(boneIndex)

        if (boneIndex !in illuminated) {
            // Exclude the emissive subtrees from the lit pass, rather than drawing them twice.
            for (index in illuminated) setBoneVisible(index, false)
            try {
                renderBoneVertices(boneIndex, poseStack, bufferSource, quadType, triangleType, light)
            } finally {
                // collect() only includes visible descendants; the root is handled separately.
                for (index in illuminated) setBoneVisible(index, true)
            }
        }
        for (index in illuminated) {
            // Use the instance's global parent transform, including animated reticle scaling.
            renderBoneVertices(index, poseStack, bufferSource,
                ModRenderTypes.scopeReticle(texture), ModRenderTypes.scopeReticleMesh(texture),
                LightTexture.FULL_BRIGHT)
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
        renderBoneVertices(boneIndex, poseStack, bufferSource, quadType, triangleType, light)
        flush(bufferSource, quadType, triangleType)
    }

    private fun renderBoneVertices(
        boneIndex: Int,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
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

        baseModel.renderBone(
            instance, boneIndex, poseStack, bufferSource.getBuffer(quadType),
            light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f, true
        )
        baseModel.renderBone(
            instance, boneIndex, poseStack, bufferSource.getBuffer(triangleType),
            light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f, false
        )
        poseStack.popPose()
        bone.visible = originalVisible
    }

    private fun flush(
        bufferSource: MultiBufferSource.BufferSource,
        vararg types: RenderType
    ) {
        if (!ShaderCompat.endBatch(bufferSource)) {
            for (type in types) bufferSource.endBatch(type)
        }
    }

    private fun getBoneCenter(
        poseStack: PoseStack,
        boneIndex: Int,
        modelViewMatrix: Matrix4f
    ): Vector3f {
        val matrix = Matrix4f(modelViewMatrix)
            .mul(poseStack.last().pose())
            .mul(instance.getGlobalTransform(boneIndex))
        return matrix.getTranslation(Vector3f())
    }

    private fun setBoneVisible(boneIndex: Int, visible: Boolean) {
        instance.getBone(boneIndex)?.visible = visible
    }

    /** 按名字设置骨骼可见性，找不到这根骨骼时静默跳过 */
    fun setBoneVisible(boneName: String, visible: Boolean) {
        setBoneVisible(baseModel.getIndex(boneName), visible)
    }

    /** 按名字读骨骼可见性，找不到这根骨骼时返回 `true` */
    fun isBoneVisible(boneName: String): Boolean {
        val index = baseModel.getIndex(boneName)
        if (index < 0) return true
        return instance.getBone(index)?.visible ?: true
    }

    /**
     * 藏掉模型自带的手部几何。
     */
    private fun hideHandBones() {
        setBoneVisible(leftHandBoneIndex, false)
        setBoneVisible(rightHandBoneIndex, false)
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

    companion object {
        internal const val DIVISION_MIN_ZOOM = 0.4

        private const val LENS_SEGMENTS = 90
        private const val LENS_FALLBACK_RINGS = 20
        private const val LENS_STEP = (Math.PI * 2.0 / LENS_SEGMENTS).toFloat()

        private const val SCOPE_BODY_NODE = "scope_body"
        private const val OCULAR_RING_NODE = "ocular_ring"
        private const val DIVISION_NODE = "division"
        private const val DYNAMIC_DIVISION_NODE = "dynamic_divison"
        private const val OCULAR_NODE = "ocular"
        private const val OCULAR_SIGHT_NODE = "ocular_sight"
        private const val OCULAR_SCOPE_NODE = "ocular_scope"
        private const val SHADER_NODE = "shader"
        private const val ILLUMINATED_SUFFIX = "_illuminated"
        private const val LEFT_HAND_BONE = "lefthand_pos"
        private const val RIGHT_HAND_BONE = "righthand_pos"
        private val OCULAR_PATTERN = Pattern.compile(
            "^($OCULAR_NODE|$OCULAR_SIGHT_NODE|$OCULAR_SCOPE_NODE)(_(\\d+))?$"
        )
    }
}
