package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.model.entity.VehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.TurretWreckEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FastColor
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Quaternionf
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.renderer.GeoEntityRenderer

class TurretWreckRenderer(renderManager: EntityRendererProvider.Context) :
    EntityRenderer<TurretWreckEntity>(renderManager) {
    init {
        this.shadowRadius = 1f
    }

    fun renderWreck(
        poseStack: PoseStack,
        wreckEntity: TurretWreckEntity,
        bufferSource: MultiBufferSource,
        partialTick: Float,
        packedLight: Int,
        packedOverlay: Int,
    ) {
        val type = EntityType.byString(wreckEntity.vehicleName)
        if (type.isEmpty) return
        val entity = type.get().create(wreckEntity.level()) ?: return
        val renderer = mc.entityRenderDispatcher.getRenderer(entity)
        if (entity is VehicleEntity && entity is GeoAnimatable && renderer is GeoEntityRenderer) {
            val model = renderer.getGeoModel()
            if (model !is VehicleModel) return

            val modelResource = model.getPreciseModelResource(entity) ?: return
            val textureResource = model.getPreciseTextureResource(entity) ?: return

            val bakedModel = model.getBakedModel(modelResource)
            val optionalBone = bakedModel.getBone("turret")
            if (optionalBone.isEmpty) return

            val barrelBone = bakedModel.getBone("barrel")
            barrelBone.ifPresent { it.setRotX(-wreckEntity.xRotO * Mth.DEG_TO_RAD) }

            val passerWeaponPitch = bakedModel.getBone("passengerWeaponStationPitch")
            passerWeaponPitch.ifPresent { it.setRotX(0f) }

            val passerWeaponYaw = bakedModel.getBone("passengerWeaponStationYaw")
            passerWeaponYaw.ifPresent { it.setRotY(0f) }

            optionalBone.get().setHidden(false)

            val turretPos = entity.turretPos

            poseStack.pushPose()

            if (turretPos != null) {
                poseStack.translate(turretPos.x, -turretPos.y, turretPos.z)
            }

            val tBone = optionalBone.get()
            val renderType = RenderType.entityTranslucent(textureResource)
            val source = bufferSource.getBuffer(renderType)

            renderer.renderCubesOfBone(
                poseStack,
                tBone,
                source,
                packedLight,
                packedOverlay,
                FastColor.ARGB32.colorFromFloat(1.0f, 0.3f, 0.3f, 0.3f)
            )
            renderer.renderChildBones(
                poseStack,
                entity,
                tBone,
                renderType,
                bufferSource,
                source,
                false,
                partialTick,
                packedLight,
                packedOverlay,
                FastColor.ARGB32.colorFromFloat(1.0f, 0.3f, 0.3f, 0.3f)
            )
            poseStack.popPose()
        }
    }

    override fun render(
        entityIn: TurretWreckEntity,
        entityYaw: Float,
        partialTicks: Float,
        poseStack: PoseStack,
        bufferIn: MultiBufferSource,
        packedLightIn: Int
    ) {
        poseStack.pushPose()
        poseStack.rotateAround(Quaternionf(entityIn.getQuaternion(partialTicks)), 0f, 0.6f, 0f)

        this.renderWreck(
            poseStack,
            entityIn,
            bufferIn,
            partialTicks,
            packedLightIn,
            OverlayTexture.NO_OVERLAY
        )

        poseStack.popPose()

        if (this.entityRenderDispatcher.shouldRenderHitBoxes()
            && !entityIn.isInvisible
            && !mc.showOnlyReducedInfo()
        ) {
            val matrix4f = poseStack.last().pose()
            val pose = poseStack.last()
            val buffer = bufferIn.getBuffer(RenderType.lines())

            val frontVec = entityIn.getFrontVec(partialTicks)
            renderAxis(entityIn, pose, matrix4f, frontVec, buffer, 0, 0, 255)

            val upVec = entityIn.getUpVec(partialTicks)
            renderAxis(entityIn, pose, matrix4f, upVec, buffer, 0, 255, 0)

            val rightVec = entityIn.getRightVec(partialTicks)
            renderAxis(entityIn, pose, matrix4f, rightVec, buffer, 255, 0, 0)
        }
    }

    override fun getTextureLocation(pEntity: TurretWreckEntity): ResourceLocation {
        return TEXTURE
    }

    fun renderAxis(
        entityIn: TurretWreckEntity,
        pose: PoseStack.Pose,
        matrix4f: Matrix4f,
        vec3: Vec3,
        buffer: VertexConsumer,
        r: Int,
        g: Int,
        b: Int
    ) {
        buffer.addVertex(matrix4f, 0.0f, 0.6f, 0.0f)
            .setColor(r, g, b, 255)
            .setNormal(pose, vec3.x.toFloat(), vec3.y.toFloat(), vec3.z.toFloat())

        buffer.addVertex(
            matrix4f,
            (vec3.x * 4.0).toFloat(),
            (entityIn.eyeHeight.toDouble() + vec3.y * 4.0).toFloat(),
            (vec3.z * 4.0).toFloat()
        ).setColor(r, g, b, 255)
            .setNormal(pose, vec3.x.toFloat(), vec3.y.toFloat(), vec3.z.toFloat())
    }

    companion object {
        val TEXTURE = loc("textures/entity/empty.png")
    }
}
