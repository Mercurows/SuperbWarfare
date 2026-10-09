package com.atsuishio.superbwarfare.client.model.gun

import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoDisplayRenderer
import com.atsuishio.superbwarfare.client.renderer.ammo.AmmoReadout
import com.atsuishio.superbwarfare.client.renderer.gun.GunEmissiveTextures
import com.atsuishio.superbwarfare.client.renderer.scope.BuiltinGunScopeRenderer
import com.atsuishio.superbwarfare.resource.ModelResource
import com.atsuishio.superbwarfare.resource.model.GunLODModelReloadListener
import com.atsuishio.superbwarfare.resource.model.GunModelReloadListener
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.TreeModelInstance
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel
import com.maydaymemory.mae.basic.Pose
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderDispatcher
import net.minecraft.client.renderer.entity.player.PlayerRenderer
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.HumanoidArm
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3f
import java.util.*

/**
 * Tree SBM gun model wrapper used by the first-person gun renderer.
 *
 * The tree model is immutable and shared between guns of the same type; the
 * runtime instance carries per-render pose state. Hand bones are kept separate
 * from the gun geometry so the renderer can choose to draw real player arms.
 *
 * Code based on TAC-Z-Respawn.
 */
open class GeoGunModel @JvmOverloads constructor(
    val baseModel: TreeBedrockModel,
    var renderHand: Boolean = true
) {
    val instance: TreeModelInstance = baseModel.createInstance()

    /**
     * Ammo readout engine, shared with the attachment models. Carries no division bones: a gun model
     * has no reticle, so every text anchor resolves to "draw it with the rest of the model".
     */
    private val ammo = AmmoDisplayRenderer(baseModel, instance)

    /**
     * 枪自带瞄具的镜筒窗口渲染器。扫一遍骨骼名就定下来了，所以懒加载。
     *
     * 和 [ammo] 不同，它**不**只服务于某一把枪：任何 geo 里带 `ocular` 的枪都能用，
     * 没有这根骨骼时 [BuiltinGunScopeRenderer.available] 为假，整条路径直接关闭。
     */
    val builtinScopeRenderer: BuiltinGunScopeRenderer by lazy { BuiltinGunScopeRenderer(baseModel, instance) }

    private val illuminatedBoneIndices: IntArray = baseModel.bones()
        .asSequence()
        .filter { it.name().endsWith(ILLUMINATED_SUFFIX) }
        .map { it.index() }
        .toList()
        .toIntArray()

    /**
     * 弹链上的子弹：`(第几发, 骨骼下标)`，按名字里的序号排好序 —— 弹链有多长由它决定。
     *
     * 约定 `bullet_N` 的子树里**只有第 N 发**那点几何（见 m_2_hb：`bullet_N` 挂着那一发的两段
     * 模型，链节骨骼 `bulletN` 才是串起整条弹链的父级），所以按剩余弹量逐个 `visible = false`
     * 就是一发一发从弹链上消失。空列表 = 这枪没有弹链，[showBulletChainBones] 什么都不做。
     */
    private val bulletChainBones: List<Pair<Int, Int>> = baseModel.bones()
        .asSequence()
        .mapNotNull { bone ->
            BULLET_CHAIN_PATTERN.matchEntire(bone.name())
                ?.groupValues?.get(1)?.toIntOrNull()
                ?.let { it to bone.index() }
        }
        .sortedBy { it.first }
        .toList()

    private val magazineBones: Map<String, Int> = mapOf(
        MAGAZINE_STANDARD_BONE to baseModel.getIndex(MAGAZINE_STANDARD_BONE),
        MAGAZINE_EXTEND_BONE to baseModel.getIndex(MAGAZINE_EXTEND_BONE),
        MAGAZINE_EXTEND_PRO_BONE to baseModel.getIndex(MAGAZINE_EXTEND_PRO_BONE)
    ).filterValues { it >= 0 }

    private val stockBones: Map<String, Int> = mapOf(
        OEM_STOCK_STANDARD_BONE to baseModel.getIndex(OEM_STOCK_STANDARD_BONE),
        OEM_STOCK_LIGHT_BONE to baseModel.getIndex(OEM_STOCK_LIGHT_BONE),
        OEM_STOCK_HEAVY_BONE to baseModel.getIndex(OEM_STOCK_HEAVY_BONE),
        CUSTOM_STOCK_ADAPTER_BONE to baseModel.getIndex(CUSTOM_STOCK_ADAPTER_BONE)
    ).filterValues { it >= 0 }

    protected val rootBoneIndex: Int = baseModel.getIndex(ROOT_BONE)
    protected val cameraBoneIndex: Int = baseModel.getIndex(CAMERA_BONE)
    protected val leftHandBoneIndex: Int = baseModel.getIndex(LEFT_HAND_BONE)
    protected val rightHandBoneIndex: Int = baseModel.getIndex(RIGHT_HAND_BONE)

    protected val bindGlobalTransformCache = hashMapOf<String, Matrix4f?>()
    protected var modelCenterCache: Vector3f? = null

    init {
        markIlluminatedBones()
    }

    fun getBone(boneName: String): BoneState? = instance.getBone(boneName)

    fun getBone(boneIndex: Int): BoneState? = instance.getBone(boneIndex)

    fun getIndex(boneName: String): Int = baseModel.getIndex(boneName)

    fun getBindPose(): Pose = baseModel.bindPose

    fun applyPose(pose: Pose) {
        instance.applyPose(pose)
    }

    fun resetPose() {
        instance.resetPose()
        markIlluminatedBones()
    }

    fun getGlobalTransform(boneName: String): Matrix4f? {
        val index = baseModel.getIndex(boneName)
        return if (index >= 0) instance.getGlobalTransform(index) else null
    }

    fun getGlobalTransform(boneIndex: Int): Matrix4f = instance.getGlobalTransform(boneIndex)

    fun getRootBone(): BoneState? = instance.getBone(rootBoneIndex)

    fun getCameraBone(): BoneState? = instance.getBone(cameraBoneIndex)

    fun showMagazineBone(visibleBoneName: String) {
        val visibleIndex = magazineBones[visibleBoneName]
            ?: magazineBones[MAGAZINE_STANDARD_BONE]
            ?: return
        for ((_, index) in magazineBones) {
            instance.getBone(index)?.visible = index == visibleIndex
        }
    }

    fun showStockBone(
        visibleBoneName: String,
        fallbackBoneName: String = OEM_STOCK_STANDARD_BONE
    ) {
        val visibleIndex = stockBones[visibleBoneName]
            ?: stockBones[fallbackBoneName]
        for ((_, index) in stockBones) {
            instance.getBone(index)?.visible = index == visibleIndex
        }
    }

    fun hideAllStockBones() {
        for ((_, index) in stockBones) {
            instance.getBone(index)?.visible = false
        }
    }

    /**
     * Draws only the round of the ammo type currently loaded: among [candidateBoneNames] — every
     * bone a gun's ammo types could use — only the ones in [visibleBoneNames] stay visible.
     *
     * Names the model does not contain are skipped, and bones outside [candidateBoneNames] keep
     * their default visibility, so a gun that declares no projectile bone renders unchanged.
     */
    fun showProjectileBone(visibleBoneNames: Collection<String>, candidateBoneNames: Collection<String>) {
        for (name in candidateBoneNames) {
            val index = baseModel.getIndex(name)
            if (index < 0) continue
            instance.getBone(index)?.visible = name in visibleBoneNames
        }
    }

    /**
     * 按剩余弹量画弹链：第 N 发的模型 `bullet_N` 只在子弹还剩 `N` 发（及以上）时显示，于是
     * 打得越少弹链上剩下的子弹越少，最先没的是序号最大的那一发。
     *
     * [keepAllVisible] 为 true 时**不隐藏任何一发** —— 换弹动画走过 `HIDE_BULLET_CHAIN` 之后
     * 弹链整条换新，此时枪里的子弹数还没回满也不会露出空链。
     *
     * 每帧对每根骨骼都显式写一次 `visible`，所以不需要谁去还原。没有 `bullet_N` 命名骨骼的枪
     * 这里一根也扫不到，渲染不变。
     */
    fun showBulletChainBones(ammo: Int, keepAllVisible: Boolean) {
        for ((order, index) in bulletChainBones) {
            instance.getBone(index)?.visible = keepAllVisible || ammo >= order
        }
    }

    /**
     * Global transform for a bone in bind pose, cached by name.
     * This is useful for attachment mounting and other static model-space calculations.
     */
    open fun getBindGlobalTransform(boneName: String): Matrix4f? {
        if (bindGlobalTransformCache.containsKey(boneName)) {
            return bindGlobalTransformCache[boneName]
        }

        val transform = computeBindGlobalTransform(boneName)
        bindGlobalTransformCache[boneName] = transform
        return transform
    }

    private fun computeBindGlobalTransform(boneName: String): Matrix4f? {
        val index = baseModel.getIndex(boneName)
        if (index < 0) return null

        val currentPose = instance.pose
        instance.resetPose()
        val transform = Matrix4f(instance.getGlobalTransform(index))
        instance.applyPose(currentPose)
        return transform
    }

    open fun getConstraintPath(boneIndex: Int): IntArray {
        if (boneIndex < 0) return IntArray(0)

        val path = ArrayList<Int>()
        var current = boneIndex
        while (current >= 0) {
            path.add(0, current)
            current = instance.getBone(current)?.parentIndex() ?: -1
        }
        return path.toIntArray()
    }

    /**
     * Model-space center, preferring the baked render bounds and falling back
     * to visible bone bind positions when bounds are unavailable.
     */
    open fun getModelCenter(): Vector3f {
        modelCenterCache?.let { return it }

        val box = baseModel.renderBoundingBox
        if (box != null) {
            modelCenterCache = Vector3f(box.center.x.toFloat(), box.center.y.toFloat(), box.center.z.toFloat())
            return modelCenterCache!!
        }

        val currentPose = instance.pose
        instance.resetPose()

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE
        var any = false

        for (bone in baseModel.bones()) {
            val name = bone.name().lowercase(Locale.ENGLISH)
            if (name.contains("view") || name.contains("camera") || name.contains("hand")) continue

            val position = instance.getGlobalTransform(bone.index()).getTranslation(Vector3f())
            minX = minOf(minX, position.x)
            minY = minOf(minY, position.y)
            minZ = minOf(minZ, position.z)
            maxX = maxOf(maxX, position.x)
            maxY = maxOf(maxY, position.y)
            maxZ = maxOf(maxZ, position.z)
            any = true
        }

        instance.applyPose(currentPose)

        modelCenterCache = if (any) {
            Vector3f((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f)
        } else {
            Vector3f()
        }
        return modelCenterCache!!
    }

    open fun renderToBuffer(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        texture: ResourceLocation,
        packedLight: Int,
        packedOverlay: Int,
        readout: AmmoReadout = AmmoReadout(),
        handAnchors: Map<HumanoidArm, Matrix4f> = emptyMap(),
        emissiveTexture: ResourceLocation? = null
    ) {
        renderToBuffer(
            poseStack,
            bufferSource,
            RenderType.entityTranslucent(texture),
            BedrockModelRenderTypes.polyMeshCutout(texture),
            packedLight,
            packedOverlay,
            readout,
            handAnchors,
            emissiveTexture
        )
    }

    /**
     * Draws the gun, optionally with its ammo readout.
     *
     * [readout] travels as a parameter rather than living on the instance because one [GeoGunModel] is
     * shared by every stack using the same model file, so two guns of the same type can be drawn in
     * the same frame — two players, or an item frame next to a held gun — and any per-render state
     * kept on the instance would let them read each other's ammo.
     *
     * Bones named by the readout that the model does not contain are skipped, which is what makes this
     * safe for LOD models: those are baked from a separate `gun_lod` file that generally has no ammo
     * bones, so a gun simply loses its readout at LOD distance instead of failing to draw.
     *
     * [handAnchors] replaces the arm anchor bones for this draw only, and travels as a parameter for
     * the same shared-instance reason as [readout]: during a sub-weapon reload the arms have to follow
     * the *attachment* model's `lefthand_pos`/`righthand_pos` instead of the gun's (see
     * `GeoGunRenderer.resolveSubWeaponHandAnchors`). An arm missing from the map keeps following the
     * gun, so callers that do not know about sub-weapons can leave it empty.
     *
     * [emissiveTexture] is the gun's optional `_e` glow mask (see [GunEmissiveTextures]). When present
     * the whole model is drawn a second time with `RenderType.eyes`, so only the texels the mask paints
     * brighten. It is a texture rather than a pair of RenderTypes because the glow layer is always this
     * one eyes pass; `null` (no `_e` file) draws exactly what the gun drew before the feature existed.
     */
    open fun renderToBuffer(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        quadRenderType: RenderType,
        triangleRenderType: RenderType,
        packedLight: Int,
        packedOverlay: Int,
        readout: AmmoReadout = AmmoReadout(),
        handAnchors: Map<HumanoidArm, Matrix4f> = emptyMap(),
        emissiveTexture: ResourceLocation? = null
    ) {
        hideBone(leftHandBoneIndex)
        hideBone(rightHandBoneIndex)
        hideShellGeometry()
        markIlluminatedBones()

        // Applied before the model pass so the model's own draw skips every bar bone that carries a
        // tint; those are drawn separately by renderBars so they can take a color.
        val ammoBarState = ammo.applyBars(readout.bars, readout.progress)

        baseModel.renderToBuffer(
            instance,
            poseStack,
            bufferSource,
            quadRenderType,
            triangleRenderType,
            packedLight,
            packedOverlay,
            1f,
            1f,
            1f,
            1f,
            true
        )

        // 自发光层：同一份几何、同一份骨骼姿态，只是换一套 RenderType 再画一遍，所以手臂遮蔽、弹链、
        // 弹匣之类的可见性判断不必重做——上面那几处 `visible` 还留在骨骼上，`renderBone` 会照样跳过。
        // 位置紧贴基础层是有意的：`BufferSource` 取到新的 RenderType 就会把上一段冲刷掉，
        // 这两遍的四种 RenderType 互不相同，于是发光层一定**后画**、盖在基础层之上
        // （`RenderType.eyes` 只写颜色、不写深度，不会把后面的东西挡掉）。
        // 手臂、弹药条、弹药文字的绘制都排在这之后，仍然压在发光层上面。
        if (emissiveTexture != null) {
            baseModel.renderToBuffer(
                instance,
                poseStack,
                bufferSource,
                RenderType.eyes(emissiveTexture),
                ModRenderTypes.polyMeshEyes(emissiveTexture),
                packedLight,
                packedOverlay,
                1f,
                1f,
                1f,
                1f,
                true
            )
        }

        if (renderHand) {
            renderHands(poseStack, packedLight, bufferSource, handAnchors)
        }

        // skipNormalVisibilityCull = false, matching the multi-buffer model pass above.
        ammo.renderBars(ammoBarState, poseStack, bufferSource, quadRenderType, triangleRenderType, packedLight, false)
        ammo.restoreBars(ammoBarState)

        // After the restore, so nothing is drawn while the model is still carrying the squashed
        // scales. Every anchor resolves to divisionIndex -1 on a gun, since a gun model has no
        // reticle subtree, so all of them are drawn here rather than by a division pass.
        for (entry in readout.texts) {
            ammo.renderText(entry, readout.count, readout.progress, readout.range, readout.heat, poseStack, bufferSource)
        }
    }

    private fun hideBone(boneIndex: Int) {
        instance.getBone(boneIndex)?.visible = false
    }

    private fun hideShellGeometry() {
        baseModel.bones().forEach { bone ->
            if (SHELL_GEOMETRY_PATTERN.matches(bone.name())) {
                instance.getBone(bone.index())?.visible = false
            }
        }
    }

    private fun markIlluminatedBones() {
        for (index in illuminatedBoneIndices) {
            instance.getBone(index)?.illuminated = true
        }
    }

    /**
     * Draws the real player arms at the model's hand anchor bones.
     *
     * [handAnchors] takes over the anchor for the arms it names — used while a sub-weapon plays its own
     * reload animation, where the hands are posed by the *attachment* model and would otherwise stay
     * glued to the gun. Anything missing from it keeps using this model's own bone.
     */
    private fun renderHands(
        poseStack: PoseStack,
        packedLight: Int,
        bufferSource: MultiBufferSource,
        handAnchors: Map<HumanoidArm, Matrix4f>
    ) {
        val player = localPlayer ?: return

        val leftAnchor = handAnchors[HumanoidArm.LEFT]
        if (leftAnchor != null) {
            poseStack.pushPose()
            mulTransformWithNormal(poseStack, leftAnchor)
            renderFirstPersonArm(player, bufferSource, HumanoidArm.LEFT, poseStack, packedLight)
            poseStack.popPose()
        } else if (leftHandBoneIndex >= 0) {
            poseStack.pushPose()
            instance.mulGlobalTransform(poseStack, leftHandBoneIndex)
            renderFirstPersonArm(player, bufferSource, HumanoidArm.LEFT, poseStack, packedLight)
            poseStack.popPose()
        }

        val rightAnchor = handAnchors[HumanoidArm.RIGHT]
        if (rightAnchor != null) {
            poseStack.pushPose()
            mulTransformWithNormal(poseStack, rightAnchor)
            renderFirstPersonArm(player, bufferSource, HumanoidArm.RIGHT, poseStack, packedLight)
            poseStack.popPose()
        } else if (rightHandBoneIndex >= 0) {
            poseStack.pushPose()
            instance.mulGlobalTransform(poseStack, rightHandBoneIndex)
            renderFirstPersonArm(player, bufferSource, HumanoidArm.RIGHT, poseStack, packedLight)
            poseStack.popPose()
        }
    }

    /**
     * Same contract as `GeoGunRenderer.mulPoseWithNormal`: `PoseStack.mulPoseMatrix` only updates the
     * pose, but the arm's lighting reads the normal matrix, so both have to be multiplied.
     */
    private fun mulTransformWithNormal(poseStack: PoseStack, matrix: Matrix4f) {
        poseStack.last().normal().mul(Matrix3f(matrix).invert().transpose())
        poseStack.last().pose().mul(matrix)
    }

    companion object {
        protected const val ROOT_BONE = "root"
        protected const val CAMERA_BONE = "camera"
        protected const val LEFT_HAND_BONE = "lefthand_pos"
        protected const val RIGHT_HAND_BONE = "righthand_pos"

        private const val ILLUMINATED_SUFFIX = "_illuminated"

        const val MAGAZINE_STANDARD_BONE = "magazine_standard"
        const val MAGAZINE_EXTEND_BONE = "magazine_extend"
        const val MAGAZINE_EXTEND_PRO_BONE = "magazine_extend_pro"

        const val OEM_STOCK_STANDARD_BONE = "oem_stock_standard"
        const val OEM_STOCK_LIGHT_BONE = "oem_stock_light"
        const val OEM_STOCK_HEAVY_BONE = "oem_stock_heavy"
        const val CUSTOM_STOCK_ADAPTER_BONE = "custom_stock_adapter"

        @JvmField
        val MAGAZINE_BONE_NAMES: Set<String> = setOf(
            MAGAZINE_STANDARD_BONE,
            MAGAZINE_EXTEND_BONE,
            MAGAZINE_EXTEND_PRO_BONE
        )

        @JvmField
        val STOCK_BONE_NAMES: Set<String> = setOf(
            OEM_STOCK_STANDARD_BONE,
            OEM_STOCK_LIGHT_BONE,
            OEM_STOCK_HEAVY_BONE,
            CUSTOM_STOCK_ADAPTER_BONE
        )

        private val SHELL_GEOMETRY_PATTERN = Regex("^shells$|^shell\\d+$|^bullet_shell$", RegexOption.IGNORE_CASE)

        /** 弹链上第 N 发子弹的骨骼名：`bullet_1`、`bullet_2`……序号从 1 开始。 */
        private val BULLET_CHAIN_PATTERN = Regex("^bullet_(\\d+)$", RegexOption.IGNORE_CASE)

        @JvmStatic
        fun create(modelPath: ResourceLocation): GeoGunModel? {
            return GunModelReloadListener.getModel(modelPath)
        }

        @JvmStatic
        fun createLOD(modelPath: ResourceLocation): GeoGunModel? {
            return GunLODModelReloadListener.getModel(modelPath)
        }

        @JvmStatic
        fun create(modelResource: ModelResource): GeoGunModel? {
            val modelPath = modelResource.model ?: return null
            return create(modelPath)
        }

        @JvmStatic
        fun create(modelResource: ModelResource, lodLevel: Int): GeoGunModel? {
            val modelPath = modelResource.getLODModel(lodLevel)
            return modelPath?.let(::createLOD)
        }

        @JvmStatic
        fun renderFirstPersonArm(
            player: LocalPlayer,
            bufferSource: MultiBufferSource,
            hand: HumanoidArm,
            poseStack: PoseStack,
            packedLight: Int
        ) {
            val renderManager: EntityRenderDispatcher = mc.entityRenderDispatcher
            val renderer = renderManager.getRenderer(player) as PlayerRenderer

            if (hand == HumanoidArm.RIGHT) {
                renderer.renderRightHand(poseStack, bufferSource, packedLight, player)
            } else {
                renderer.renderLeftHand(poseStack, bufferSource, packedLight, player)
            }
        }
    }
}
