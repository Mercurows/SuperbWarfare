package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.DroneLayer
import com.atsuishio.superbwarfare.client.model.entity.DroneModel
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.tools.NBTTool
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.CameraType
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import software.bernie.geckolib.renderer.GeoEntityRenderer

class DroneRenderer(renderManager: EntityRendererProvider.Context) :
    GeoEntityRenderer<DroneEntity>(renderManager, DroneModel()) {
    override fun getRenderType(
        animatable: DroneEntity,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType? {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }

    override fun render(
        entityIn: DroneEntity,
        entityYaw: Float,
        partialTicks: Float,
        poseStack: PoseStack,
        bufferIn: MultiBufferSource,
        packedLightIn: Int
    ) {
        poseStack.pushPose()
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityIn.getYaw(partialTicks)))
        poseStack.mulPose(Axis.XP.rotationDegrees(entityIn.getBodyPitch(partialTicks)))
        poseStack.mulPose(Axis.ZP.rotationDegrees(entityIn.getRoll(partialTicks)))
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn)

        var flag = true
        val player = localPlayer
        if (player != null) {
            if (mc.options.cameraType == CameraType.FIRST_PERSON || mc.options.cameraType == CameraType.THIRD_PERSON_BACK) {
                val stack = player.mainHandItem
                val tag = NBTTool.getTag(stack)

                if (stack.`is`(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) {
                    if (entityIn.getUUID().toString() == tag.getString("LinkedDrone")) {
                        flag = false
                    }
                }
            }
        }

        if (flag) {
            renderAttachments(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn)
        }

        poseStack.popPose()
    }

    private var entityNameCache = ""
    private var entityCache: Entity? = null
    private var attachedTick = Int.MAX_VALUE

    init {
        this.addRenderLayer(DroneLayer(this))
        this.shadowRadius = 0.2f
    }

    // 统一渲染挂载实体
    private fun renderAttachments(
        entity: DroneEntity,
        entityYaw: Float,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        val data = entity.getEntityData()
        val attached = data.get(DroneEntity.DISPLAY_ENTITY)
        if (attached.isEmpty()) return

        val renderEntity: Entity?

        if (entityNameCache == attached && entityCache != null) {
            renderEntity = entityCache
        } else {
            renderEntity = EntityType.byString(attached)
                .map { type -> type.create(entity.level()) }
                .orElse(null)
            if (renderEntity == null) return

            // 填充tag
            val tag = data.get(DroneEntity.DISPLAY_ENTITY_TAG)
            if (!tag.isEmpty) {
                renderEntity.load(tag)
            }

            entityNameCache = attached
            entityCache = renderEntity
            attachedTick = entity.tickCount
        }
        val displayData = data.get(DroneEntity.DISPLAY_DATA)

        renderEntity!!.tickCount =
            if (displayData[11] >= 0) displayData[11].toInt() else entity.tickCount - attachedTick

        val scale = floatArrayOf(displayData[0], displayData[1], displayData[2])
        val offset = floatArrayOf(displayData[3], displayData[4], displayData[5])
        val rotation = floatArrayOf(displayData[6], displayData[7], displayData[8])
        val xLength = displayData[9]
        val yLength = displayData[10]

        for (i in 0..<entity.ammo) {
            val x: Float
            val z: Float
            if (data.get(DroneEntity.MAX_AMMO) == 1) {
                // 神风或单个挂载
                x = 0f
                z = 0f
            } else {
                // 投弹
                x = xLength / 2 * (if (i % 2 == 0) 1 else -1)

                val rows = data.get(DroneEntity.MAX_AMMO) / 2
                val row = i / 2
                if (rows < 2) {
                    z = 0f
                } else {
                    val rowLength = yLength / rows
                    z = -yLength / 2 + rowLength * row
                }
            }

            poseStack.pushPose()
            poseStack.translate(x + offset[0], offset[1], z + offset[2])
            poseStack.scale(scale[0], scale[1], scale[2])
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation[2]))
            poseStack.mulPose(Axis.XP.rotationDegrees(rotation[0]))
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation[1]))

            entityRenderDispatcher.render(
                renderEntity,
                0.0,
                0.0,
                0.0,
                entityYaw,
                partialTicks,
                poseStack,
                buffer,
                packedLight
            )

            poseStack.popPose()
        }
    }
}
