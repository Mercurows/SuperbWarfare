package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.CameraStateCache
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.tan

/**
 * Projected screen position of a model point.
 *
 * [x] and [y] are GUI-scaled screen coordinates. [depth] is the NDC depth
 * after perspective division. [visible] is false when the point is behind the
 * camera or outside the supplied screen bounds.
 */
data class BoneScreenPoint(
    val x: Float,
    val y: Float,
    val depth: Float,
    val visible: Boolean
)

/**
 * Shared coordinate conversion helpers for SBM models rendered in first person.
 *
 * The first-person renderer already applies the current gun pose through
 * [PoseStack]. This object therefore expects the same complete pose stack that
 * is passed to `renderModel`/`afterRender`, where model geometry starts at the
 * pose stack origin. Bone-local points additionally require the bone transform
 * returned by SBM's `TreeModelInstance.getGlobalTransform`.
 *
 * Code based on TACZ-RESPAWN.
 */
@OnlyIn(Dist.CLIENT)
object BedrockBoneCoordinateTool {

    private const val CLIP_EPSILON = 1.0e-5f

    /**
     * Transforms a point in SBM model space into first-person view space.
     * View space uses camera position as the origin, +X right, +Y up and +Z back.
     */
    @JvmStatic
    fun firstPersonModelPointToView(
        poseStack: PoseStack,
        modelPoint: Vector3f
    ): Vec3 {
        return transformPosition(poseStack.last().pose(), modelPoint)
    }

    /**
     * Transforms a point in a bone's local space into first-person view space.
     * [boneTransform] must be the animated global transform of that bone.
     */
    @JvmStatic
    fun firstPersonBonePointToView(
        poseStack: PoseStack,
        boneTransform: Matrix4f,
        boneLocalPoint: Vector3f
    ): Vec3 {
        val modelMatrix = Matrix4f(poseStack.last().pose()).mul(boneTransform)
        return transformPosition(modelMatrix, boneLocalPoint)
    }

    /**
     * Resolves a named gun bone and transforms its local point into view space.
     */
    @JvmStatic
    fun firstPersonBonePointToView(
        poseStack: PoseStack,
        model: GeoGunModel,
        boneName: String,
        boneLocalPoint: Vector3f
    ): Vec3? {
        val boneTransform = model.getGlobalTransform(boneName) ?: return null
        return firstPersonBonePointToView(poseStack, boneTransform, boneLocalPoint)
    }

    /**
     * Projects an SBM model point to GUI-scaled screen coordinates using the
     * supplied projection matrix. Pass the projection active during first-person
     * model rendering (usually `RenderSystem.getProjectionMatrix()`).
     */
    @JvmStatic
    fun firstPersonModelPointToScreen(
        poseStack: PoseStack,
        modelPoint: Vector3f,
        projectionMatrix: Matrix4f,
        screenWidth: Int,
        screenHeight: Int
    ): BoneScreenPoint? {
        val viewPoint = firstPersonModelPointToView(poseStack, modelPoint)
        return viewPointToScreen(viewPoint, projectionMatrix, screenWidth, screenHeight)
    }

    /**
     * Projects a bone-local SBM point to GUI-scaled screen coordinates.
     */
    @JvmStatic
    fun firstPersonBonePointToScreen(
        poseStack: PoseStack,
        boneTransform: Matrix4f,
        boneLocalPoint: Vector3f,
        projectionMatrix: Matrix4f,
        screenWidth: Int,
        screenHeight: Int
    ): BoneScreenPoint? {
        val viewPoint = firstPersonBonePointToView(poseStack, boneTransform, boneLocalPoint)
        return viewPointToScreen(viewPoint, projectionMatrix, screenWidth, screenHeight)
    }

    /**
     * Resolves a named gun bone and projects its local point to the screen.
     */
    @JvmStatic
    fun firstPersonBonePointToScreen(
        poseStack: PoseStack,
        model: GeoGunModel,
        boneName: String,
        boneLocalPoint: Vector3f,
        projectionMatrix: Matrix4f,
        screenWidth: Int,
        screenHeight: Int
    ): BoneScreenPoint? {
        val boneTransform = model.getGlobalTransform(boneName) ?: return null
        return firstPersonBonePointToScreen(
            poseStack,
            boneTransform,
            boneLocalPoint,
            projectionMatrix,
            screenWidth,
            screenHeight
        )
    }

    /**
     * Convenience overload for first-person render callbacks. It reads the
     * current projection and Minecraft's GUI-scaled window dimensions.
     */
    @JvmStatic
    fun firstPersonModelPointToScreen(
        poseStack: PoseStack,
        modelPoint: Vector3f
    ): BoneScreenPoint? {
        val window = mc.window
        return firstPersonModelPointToScreen(
            poseStack,
            modelPoint,
            RenderSystem.getProjectionMatrix(),
            window.guiScaledWidth,
            window.guiScaledHeight
        )
    }

    /**
     * Convenience overload for named gun bones during first-person rendering.
     */
    @JvmStatic
    fun firstPersonBonePointToScreen(
        poseStack: PoseStack,
        model: GeoGunModel,
        boneName: String,
        boneLocalPoint: Vector3f
    ): BoneScreenPoint? {
        val window = mc.window
        return firstPersonBonePointToScreen(
            poseStack,
            model,
            boneName,
            boneLocalPoint,
            RenderSystem.getProjectionMatrix(),
            window.guiScaledWidth,
            window.guiScaledHeight
        )
    }

    /**
     * Converts a view-space point into a true world-space position.
     */
    @JvmStatic
    fun viewPointToWorld(
        viewPoint: Vec3,
        camera: Camera
    ): Vec3 {
        return camera.position.add(viewPointToWorldOffset(viewPoint, camera))
    }

    /**
     * Converts a view-space point into world space when the first-person model
     * and the world scene use different FOV values.
     */
    @JvmStatic
    fun viewPointToWorld(
        viewPoint: Vec3,
        camera: Camera,
        modelFovDegrees: Float,
        worldFovDegrees: Float
    ): Vec3 {
        val adjusted = adjustViewDepthForFov(viewPoint, modelFovDegrees, worldFovDegrees)
        return viewPointToWorld(adjusted, camera)
    }

    /**
     * Converts a view-space point into a camera-relative world-aligned offset.
     * This is the same transform used by SBM first-person world particles, so
     * callers that need a `Matrix4f` can build it from this offset as well.
     */
    @JvmStatic
    fun viewPointToWorldOffset(
        viewPoint: Vec3,
        camera: Camera
    ): Vec3 {
        val transform = cameraRotationInverse(camera)
        val worldOffset = transform.transformPosition(
            Vector3f(
                viewPoint.x.toFloat(),
                viewPoint.y.toFloat(),
                viewPoint.z.toFloat()
            )
        )
        return Vec3(
            worldOffset.x.toDouble(),
            worldOffset.y.toDouble(),
            worldOffset.z.toDouble()
        )
    }

    /**
     * Converts a first-person SBM model point into world coordinates.
     */
    @JvmStatic
    fun firstPersonModelPointToWorld(
        poseStack: PoseStack,
        modelPoint: Vector3f,
        camera: Camera
    ): Vec3 {
        return viewPointToWorld(firstPersonModelPointToView(poseStack, modelPoint), camera)
    }

    /**
     * Convenience overload using the current first-person camera.
     */
    @JvmStatic
    fun firstPersonModelPointToWorld(
        poseStack: PoseStack,
        modelPoint: Vector3f
    ): Vec3 {
        return firstPersonModelPointToWorld(
            poseStack,
            modelPoint,
            mc.gameRenderer.mainCamera
        )
    }

    /**
     * Converts a first-person bone-local SBM point into world coordinates.
     */
    @JvmStatic
    fun firstPersonBonePointToWorld(
        poseStack: PoseStack,
        boneTransform: Matrix4f,
        boneLocalPoint: Vector3f,
        camera: Camera
    ): Vec3 {
        return viewPointToWorld(
            firstPersonBonePointToView(poseStack, boneTransform, boneLocalPoint),
            camera
        )
    }

    /**
     * Convenience overload using the current first-person camera.
     */
    @JvmStatic
    fun firstPersonBonePointToWorld(
        poseStack: PoseStack,
        boneTransform: Matrix4f,
        boneLocalPoint: Vector3f
    ): Vec3 {
        return firstPersonBonePointToWorld(
            poseStack,
            boneTransform,
            boneLocalPoint,
            mc.gameRenderer.mainCamera
        )
    }

    /**
     * Resolves a named gun bone and converts its local point to world coordinates.
     */
    @JvmStatic
    fun firstPersonBonePointToWorld(
        poseStack: PoseStack,
        model: GeoGunModel,
        boneName: String,
        boneLocalPoint: Vector3f,
        camera: Camera
    ): Vec3? {
        val boneTransform = model.getGlobalTransform(boneName) ?: return null
        return firstPersonBonePointToWorld(poseStack, boneTransform, boneLocalPoint, camera)
    }

    /**
     * Convenience overload using the current first-person camera.
     */
    @JvmStatic
    fun firstPersonBonePointToWorld(
        poseStack: PoseStack,
        model: GeoGunModel,
        boneName: String,
        boneLocalPoint: Vector3f
    ): Vec3? {
        val boneTransform = model.getGlobalTransform(boneName) ?: return null
        return firstPersonBonePointToWorld(
            poseStack,
            boneTransform,
            boneLocalPoint,
            mc.gameRenderer.mainCamera
        )
    }

    /**
     * Returns the camera rotation inverse used to lift SBM first-person view
     * coordinates into world-aligned coordinates around the camera.
     */
    @JvmStatic
    fun cameraRotationInverse(camera: Camera): Matrix4f {
        return Matrix4f()
            .rotationX(Mth.DEG_TO_RAD * camera.xRot)
            .rotateY(Mth.DEG_TO_RAD * (camera.yRot + 180f))
            .rotateZ(CameraStateCache.getCameraRollRadians())
            .invert()
    }

    /**
     * Corrects a first-person view point when the held-item render FOV differs
     * from the world render FOV. This matches the TAC-Z Respawn conversion used
     * before lifting first-person muzzle/laser points into world space.
     */
    @JvmStatic
    fun adjustViewDepthForFov(
        viewPoint: Vec3,
        modelFovDegrees: Float,
        worldFovDegrees: Float
    ): Vec3 {
        if (modelFovDegrees <= 0f || worldFovDegrees <= 0f) {
            return viewPoint
        }
        val ratio = tan(Math.toRadians(modelFovDegrees / 2.0)) /
                tan(Math.toRadians(worldFovDegrees / 2.0))
        return Vec3(
            viewPoint.x,
            viewPoint.y,
            viewPoint.z * ratio
        )
    }

    @JvmStatic
    fun viewPointToScreen(
        viewPoint: Vec3,
        projectionMatrix: Matrix4f,
        screenWidth: Int,
        screenHeight: Int
    ): BoneScreenPoint? {
        val clip = projectionMatrix.transform(
            Vector4f(
                viewPoint.x.toFloat(),
                viewPoint.y.toFloat(),
                viewPoint.z.toFloat(),
                1.0f
            )
        )
        val w = clip.w()
        if (!w.isFinite() || w <= CLIP_EPSILON) {
            return null
        }

        val ndcX = clip.x() / w
        val ndcY = clip.y() / w
        val depth = clip.z() / w
        val x = (ndcX * 0.5f + 0.5f) * screenWidth
        val y = (1.0f - (ndcY * 0.5f + 0.5f)) * screenHeight
        val visible = x >= 0.0f && x <= screenWidth &&
                y >= 0.0f && y <= screenHeight

        return BoneScreenPoint(x, y, depth, visible)
    }

    private fun transformPosition(
        matrix: Matrix4f,
        point: Vector3f
    ): Vec3 {
        val transformed = matrix.transformPosition(Vector3f(point))
        return Vec3(
            transformed.x.toDouble(),
            transformed.y.toDouble(),
            transformed.z.toDouble()
        )
    }
}
