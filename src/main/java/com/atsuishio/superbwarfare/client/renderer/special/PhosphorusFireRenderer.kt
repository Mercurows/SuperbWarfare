package com.atsuishio.superbwarfare.client.renderer.special

import com.atsuishio.superbwarfare.capability.living.PhosphorusFireCapability
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.resources.model.Material
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import org.joml.Quaternionf

@EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object PhosphorusFireRenderer {
    @Suppress("DEPRECATION")
    @SubscribeEvent
    fun onRenderCurseFlame(event: RenderLivingEvent<LivingEntity, out EntityModel<LivingEntity>>) {
        val entity = event.entity
        if (!PhosphorusFireCapability.of(entity).isOnFire) return

        val stack = event.poseStack

        val sprite1 = Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation("block/soul_fire_0")).sprite()
        val sprite2 = Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation("block/soul_fire_1")).sprite()

        stack.pushPose()
        val size = entity.bbWidth * 1.6f
        stack.scale(size, size, size)

        var hwRatio = entity.bbHeight / size
        var xOffset = 0.5f
        var yOffset = (entity.y - entity.boundingBox.minY).toFloat()
        var zOffset = 0.0f

        val camera = mc.entityRenderDispatcher.cameraOrientation()
        stack.mulPose(Quaternionf(0f, camera.y, 0f, camera.w))

        stack.translate(0.0f, 0.0f, hwRatio * 0.02f)

        var i = 0
        val vertexConsumer = event.multiBufferSource.getBuffer(Sheets.cutoutBlockSheet())

        val pose = stack.last()
        while (hwRatio > 0.0f) {
            val atlasSprite = if (i % 2 == 0) sprite1 else sprite2
            var u0 = atlasSprite.u0
            val v0 = atlasSprite.v0
            var u1 = atlasSprite.u1
            val v1 = atlasSprite.v1
            if (i / 2 % 2 == 0) {
                val temp = u1
                u1 = u0
                u0 = temp
            }

            fireVertex(pose, vertexConsumer, xOffset - 0.0f, 0.0f - yOffset, zOffset, u1, v1)
            fireVertex(pose, vertexConsumer, -xOffset - 0.0f, 0.0f - yOffset, zOffset, u0, v1)
            fireVertex(pose, vertexConsumer, -xOffset - 0.0f, 1.4f - yOffset, zOffset, u0, v0)
            fireVertex(pose, vertexConsumer, xOffset - 0.0f, 1.4f - yOffset, zOffset, u1, v0)

            hwRatio -= 0.45f
            yOffset -= 0.45f
            xOffset *= 0.9f
            zOffset += 0.03f
            ++i
        }

        stack.popPose()
    }

    private fun fireVertex(
        pMatrixEntry: PoseStack.Pose,
        pBuffer: VertexConsumer,
        pX: Float,
        pY: Float,
        pZ: Float,
        pTexU: Float,
        pTexV: Float
    ) {
        pBuffer.vertex(pMatrixEntry.pose(), pX, pY, pZ).color(150, 150, 255, 255).uv(pTexU, pTexV).overlayCoords(0, 10)
            .uv2(240).normal(pMatrixEntry.normal(), 0.0f, 1.0f, 0.0f).endVertex()
    }
}
