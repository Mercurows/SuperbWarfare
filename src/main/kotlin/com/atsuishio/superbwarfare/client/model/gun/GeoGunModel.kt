package com.atsuishio.superbwarfare.client.model.gun

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

    private val illuminatedBoneIndices: IntArray = baseModel.bones()
        .asSequence()
        .filter { it.name().endsWith(ILLUMINATED_SUFFIX) }
        .map { it.index() }
        .toList()
        .toIntArray()

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
        packedOverlay: Int
    ) {
        renderToBuffer(
            poseStack,
            bufferSource,
            RenderType.entityCutout(texture),
            BedrockModelRenderTypes.polyMeshCutout(texture),
            packedLight,
            packedOverlay
        )
    }

    open fun renderToBuffer(
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        quadRenderType: RenderType,
        triangleRenderType: RenderType,
        packedLight: Int,
        packedOverlay: Int
    ) {
        hideBone(leftHandBoneIndex)
        hideBone(rightHandBoneIndex)
        hideShellGeometry()
        markIlluminatedBones()

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

        if (renderHand) {
            renderHands(poseStack, packedLight, bufferSource)
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

    private fun renderHands(poseStack: PoseStack, packedLight: Int, bufferSource: MultiBufferSource) {
        val player = localPlayer ?: return

        if (leftHandBoneIndex >= 0) {
            poseStack.pushPose()
            instance.mulGlobalTransform(poseStack, leftHandBoneIndex)
            renderFirstPersonArm(player, bufferSource, HumanoidArm.LEFT, poseStack, packedLight)
            poseStack.popPose()
        }

        if (rightHandBoneIndex >= 0) {
            poseStack.pushPose()
            instance.mulGlobalTransform(poseStack, rightHandBoneIndex)
            renderFirstPersonArm(player, bufferSource, HumanoidArm.RIGHT, poseStack, packedLight)
            poseStack.popPose()
        }
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
