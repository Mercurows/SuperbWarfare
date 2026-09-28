package com.atsuishio.superbwarfare.client.model.attachment

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.charm.CharmRig
import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoDisplayRenderer
import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoDisplayRenderer.AmmoBarState
import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoDisplayRenderer.AmmoText
import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoReadout
import com.atsuishio.superbwarfare.client.renderer.scope.ScopeStencilRenderHelper
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

    /**
     * 把一份姿态应用到附件模型上（**四期新增**，§9.8.7）。
     *
     * 与 `GeoGunModel.applyPose` 是同一套（底层同为 [TreeModelInstance]），
     * 缺的只是这一层薄封装 —— 副武器的换弹动画由**它自己的模型**播，
     * 就是靠这两个方法 + `AttachmentModelReloadListener` 的 `animPath` 才成立的。
     *
     * 调用方（`GeoGunRenderer.renderRegisteredAttachments`）必须在渲染后调用 [resetPose]：
     * 模型实例是**全局共享**的（同一种配件装在多把枪上共用一份实例），
     * 留着上一帧的姿态会串到别的枪上。
     */
    fun applyPose(pose: Pose) {
        instance.applyPose(pose)
    }

    /**
     * 绑定姿势，与 `GeoGunModel.getBindPose` 是同一个东西（同一个底层模型）。
     *
     * ⚠ **不要拿 clip 的原始姿态直接 [applyPose]**：`BoneTreeInstance.applyPose` 是**直接写**
     * `BoneState.x/y/z`，而动画文件里的平移量是"**相对绑定姿势的偏移**"（`0` = 停在骨骼自己的
     * 静止位置）。主武器那边正是靠 `BLENDER.blend(bindPose, pose)` 把绑定垫回去的
     * （见 `GeoGunRenderer` 里应用主武器姿态那一行），所以附件也必须走同一套 ——
     * 否则**绑定不为 0 的骨骼会跑到父节点原点**去。下挂筒的炮弹就是这样错的：
     * `projectile` 绑定 `(0, -1.1743, -6.5436)`（枪管轴线），clip 里写 `[0,0,0]` 时
     * 整棵子树（炮弹**和**手）被抬到模型原点，实测偏上 0.0734、偏后 0.4090 方块。
     */
    fun getBindPose(): Pose = baseModel.bindPose

    /** 复位姿态；与 [applyPose] 成对使用 */
    fun resetPose() {
        instance.resetPose()
        markIlluminatedBones()
    }

    /**
     * Ammo readout engine shared with the gun model. Built lazily because [divisionGroups] is only
     * populated once the constructor's bone scan has run.
     */
    private val ammo: AmmoDisplayRenderer by lazy {
        AmmoDisplayRenderer(baseModel, instance, divisionGroups.values.flatten().toHashSet())
    }

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

    /**
     * 手部骨骼索引，与 `GeoGunModel` 是同一对约定名。
     *
     * 副武器模型是从普通枪模型上裁下来的，`lefthand_pos` 上还留着**一整块手部几何**
     * （`sub_weapon_gp_25.geo.json` 里那块 `4×12×4`）。第一人称下它会和
     * `GeoGunModel.renderHands` 画的真手臂叠在一起，所以要像渲染主武器那样把它藏掉 ——
     * `renderBone` 会跳过隐藏骨骼的**整棵子树**。
     *
     * 模型里没有这两根骨骼时索引是 -1，[setBoneVisible] 会安静地跳过。
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

    /**
     * 吊坠骨架：`string` / `charm` 两根分组骨骼 + 摆点/摆长/静止方向。
     *
     * **懒解析、结果挂在本对象上**：模型对象在数据包重载时会被整个换掉，
     * 所以缓存在这里天然随重载失效 —— 换成"按模型路径缓存"就会一直用旧模型的骨骼下标。
     * 模型里没有这两个分组时返回 `null`（正常配件都会走这条，只有吊坠不是）。
     *
     * 解析结果**打一条日志**（每个模型一次）：吊坠不摆的原因只有两种 —— "没找到分组"和
     * "找到了但几何量离谱"，两种都表现为**画面上什么都不发生**，不留一行字就只能靠猜。
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

        // After the restore, so nothing is drawn while the model is still carrying the squashed
        // scales. A text anchored on the scope body is correct here as-is; one anchored below a
        // division lands inside the housing and gets depth tested away, which is why the aiming path
        // draws those itself.
        for (entry in readout.texts) {
            ammo.renderText(entry, readout.count, readout.progress, poseStack, bufferSource)
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
        val ammoBarState = ammo.applyBars(readout.bars, readout.progress)
        val texts = ammo.buildTexts(readout.texts, readout.count, readout.progress)
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

        ammo.restoreBars(ammoBarState)
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
                    ammo.renderText(text, poseStack, bufferSource)
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

        // Texts anchored outside a division subtree. Unlike a reticle text they are not inside the
        // ocular opening, so they need the depth buffer rather than a stencil window to stay in front
        // of the housing — and by now the whole body has been drawn into it, on this path and on the
        // aiming path alike. Drawn last so nothing of the model can overwrite them.
        for (text in texts) {
            if (text.divisionIndex < 0) {
                ammo.renderText(text, poseStack, bufferSource)
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

    /**
     * 藏掉模型自带的手部几何，与 `GeoGunModel.renderToBuffer` 开头那两句同义。
     *
     * 只在**画之前**置位、不还原：这两根骨骼上没有任何一帧需要它们可见的东西
     * （真手臂由主武器那条链路单独画），而模型实例是同型号附件全局共享的，
     * 每帧重设只是把 `GeoGunModel` 的写法照搬过来。
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

        /** 手部骨骼名，与 `GeoGunModel.LEFT_HAND_BONE` / `RIGHT_HAND_BONE` 保持一致 */
        private const val LEFT_HAND_BONE = "lefthand_pos"
        private const val RIGHT_HAND_BONE = "righthand_pos"
        private val OCULAR_PATTERN = Pattern.compile(
            "^($OCULAR_NODE|$OCULAR_SIGHT_NODE|$OCULAR_SCOPE_NODE)(_(\\d+))?$"
        )
    }
}
