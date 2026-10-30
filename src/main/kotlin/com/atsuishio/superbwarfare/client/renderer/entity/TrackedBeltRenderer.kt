package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance
import com.atsuishio.superbwarfare.physics.tracked.TrackedRenderState
import com.atsuishio.superbwarfare.physics.tracked.TrackedSideRenderState
import com.atsuishio.superbwarfare.physics.tracked.render.BeltContactBaseline
import com.atsuishio.superbwarfare.physics.tracked.render.McspTrackPath
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * Inserts visible track plates on the shared wheel envelope.
 *
 * The plates are not Bullet bodies. The old continuous ring geometry is hidden
 * rather than used as the belt.
 */
@OnlyIn(Dist.CLIENT)
internal object TrackedBeltRenderer {
    private val legacyRings = arrayOf("ring_l", "ring_l2", "ring_r", "ring_r2")

    fun prepare(instance: VehicleModelInstance, tracked: TrackedRenderState) {
        hideLegacy(instance)
        placeWheels(instance, tracked)
    }

    fun draw(
        instance: VehicleModelInstance,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        texture: ResourceLocation,
        packedLight: Int,
        tracked: TrackedRenderState,
    ) {
        val consumer = buffer.getBuffer(RenderType.entityTranslucent(texture))
        drawSide(instance, poseStack, consumer, packedLight, tracked, tracked.left)
        drawSide(instance, poseStack, consumer, packedLight, tracked, tracked.right)
    }

    private fun hideLegacy(instance: VehicleModelInstance) {
        for (name in legacyRings) instance.getBone(name)?.visible = false
        val groups = instance.boneGroups
        sequenceOf(
            groups.leftTrackMove, groups.leftTrackRot, groups.rightTrackMove, groups.rightTrackRot,
        ).flatten().forEach { it.visible = false }
    }

    private fun placeWheels(instance: VehicleModelInstance, tracked: TrackedRenderState) {
        placeSideWheels(instance, tracked, tracked.left)
        placeSideWheels(instance, tracked, tracked.right)
    }

    private fun placeSideWheels(
        instance: VehicleModelInstance,
        tracked: TrackedRenderState,
        side: TrackedSideRenderState,
    ) {
        for (wheel in side.wheels) {
            val bone = instance.getBone(wheel.bone) ?: continue
            place(instance, bone, tracked, wheel.x, wheel.y, wheel.z)
            bone.rotation.rotationX(wheel.spin.toFloat())
        }
    }

    private fun drawSide(
        instance: VehicleModelInstance,
        poseStack: PoseStack,
        consumer: com.mojang.blaze3d.vertex.VertexConsumer,
        packedLight: Int,
        tracked: TrackedRenderState,
        side: TrackedSideRenderState,
    ) {
        if (side.visibleCount <= 0 || side.restPitch <= 1.0e-6 || side.templateBone.isBlank() || side.wheels.size < 2) return
        val path = McspTrackPath.solve(side.wheels.map {
            McspTrackPath.Wheel(it.y, it.z, it.radius, it.outside)
        }, side.thickness) ?: return
        val link = instance.getBone(side.templateBone) ?: return
        val move = instance.getBone(link.parentIndex()) ?: link
        val boneIndex = instance.getIndex(side.templateBone)
        if (boneIndex < 0) return
        link.visible = true
        val definition = link.definition()
        // MCSP samples `(phase + index) * currentLength / count`. Phase itself
        // advances by belt speed over the fixed rest pitch. The road-wheel
        // baseline only shifts the plate onto that centre line; it does not
        // move the idler or freeze the scroll.
        val spacing = path.length / side.visibleCount
        val travel = side.phase * spacing
        val seatNormal = seatOnRoadWheels(path, side)
        for (plate in 0 until side.visibleCount) {
            val sample = path.at(travel + plate * spacing)
            val pose = McspTrackPath.linkPose(sample, side.seatAlong, seatNormal, side.seatAngle)
            place(instance, move, tracked, side.planeX, pose.y, pose.z)
            link.x = definition.bindX()
            link.y = definition.bindY()
            link.z = definition.bindZ()
            link.rotation.rotationX(pose.rotationX)
            instance.renderSingleBonePass(
                poseStack,
                boneIndex,
                consumer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                1f, 1f, 1f, 1f,
                true,
                false,
            )
        }
        link.visible = false
    }

    /**
     * Seat the template so a plate under a road wheel lies on the belt centre line.
     *
     * The idler is not a reference: it is fixed and is not a suspension station.
     * One probe step resolves the sign of the path-frame normal, then the mean
     * radial error of the sprung wheels is applied to every plate.
     */
    private fun seatOnRoadWheels(path: McspTrackPath.Path, side: TrackedSideRenderState): Double {
        val roads = side.wheels.filter { it.suspended && it.radius > 0.0 }
        if (roads.isEmpty()) return side.seatNormal
        val probe = roads[roads.size / 2]
        val nearest = BeltContactBaseline.nearest(path, probe.y, probe.z) ?: return side.seatNormal
        val sample = path.at(nearest.distance)
        fun radiusAt(normal: Double): Double? {
            val pose = McspTrackPath.linkPose(sample, side.seatAlong, normal, side.seatAngle)
            val gap = kotlin.math.hypot(pose.y - probe.y, pose.z - probe.z)
            return gap.takeIf { it.isFinite() }
        }
        val current = radiusAt(side.seatNormal) ?: return side.seatNormal
        val nudged = radiusAt(side.seatNormal + 0.01) ?: return side.seatNormal
        val sign = if (nudged >= current) 1.0 else -1.0
        var error = 0.0
        var counted = 0
        for (wheel in roads) {
            val point = BeltContactBaseline.nearest(path, wheel.y, wheel.z) ?: continue
            val pose = McspTrackPath.linkPose(path.at(point.distance), side.seatAlong, side.seatNormal, side.seatAngle)
            val radial = BeltContactBaseline.radialError(
                wheel.y, wheel.z, wheel.radius, side.thickness, pose.y, pose.z,
            ) ?: continue
            error += radial
            counted++
        }
        if (counted == 0) return side.seatNormal
        return side.seatNormal + sign * (error / counted)
    }

    private fun place(
        instance: VehicleModelInstance,
        bone: BoneState,
        tracked: TrackedRenderState,
        x: Double,
        y: Double,
        z: Double,
    ) {
        val desired = chassisToPixels(tracked, x, y, z)
        val definition = bone.definition()
        val delta = Vector3f(
            desired.x - definition.pivotX() * 16f,
            desired.y - definition.pivotY() * 16f,
            desired.z - definition.pivotZ() * 16f,
        )
        val parent = instance.getBone(bone.parentIndex())
        if (parent != null) {
            val rotation = Quaternionf()
            parent.getGlobalTransform(instance).getNormalizedRotation(rotation)
            if (rotation.lengthSquared() > 1.0e-8f) rotation.normalize().invert().transform(delta)
        }
        bone.x = definition.bindX() + delta.x
        bone.y = definition.bindY() + delta.y
        bone.z = definition.bindZ() + delta.z
    }

    /**
     * Chassis metres to baker pixels.
     *
     * The baker stores `pivotX = -rawX / 16` and `pivotZ = rawZ / 16`. Physics
     * local Z is already `-rawZ / 16`, so only X needs another negation here.
     */
    private fun chassisToPixels(tracked: TrackedRenderState, x: Double, y: Double, z: Double): Vector3f {
        val scale = if (tracked.renderScale == 0.0) 1.0 else tracked.renderScale
        val toPixels = 16.0 / scale
        return Vector3f(
            ((-(x + tracked.originX)) * toPixels).toFloat(),
            ((y + tracked.originY) * toPixels).toFloat(),
            ((-(z + tracked.originZ)) * toPixels).toFloat(),
        )
    }
}
