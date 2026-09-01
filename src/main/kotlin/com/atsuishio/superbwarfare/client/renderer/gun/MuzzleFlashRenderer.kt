package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f

object MuzzleFlashRenderer {

    private val FLARE_TEXTURE = Mod.loc("textures/particle/flare.png")

    private const val FLARE_BONE = "flare"
    private const val MUZZLE_FLASH_BONE = "muzzle_flash"
    private const val MAX_VISIBLE_TIME = 0.3

    fun render(
        poseStack: PoseStack,
        model: GeoGunModel,
        stack: ItemStack,
        bufferSource: MultiBufferSource,
        attachmentMuzzleTransform: Matrix4f? = null,
        muzzleFlashScale: Float = 1.0f
    ) {
        val fireRotTimer = ClientEventHandler.fireRotTimer
        if (fireRotTimer <= 0.0 || fireRotTimer >= MAX_VISIBLE_TIME) return
        if (GunData.from(stack).attachment.get(AttachmentType.BARREL) == 2) return

        val resource = GunResource.compute(stack)
        val flareBone = model.getBone(FLARE_BONE) ?: model.getBone(MUZZLE_FLASH_BONE)
        val flarePosition = resource.flarePosition
        if (flareBone == null && flarePosition == null && attachmentMuzzleTransform == null) return

        poseStack.pushPose()
        if (attachmentMuzzleTransform != null) {
            poseStack.last().pose().mul(attachmentMuzzleTransform)
        } else if (flareBone != null) {
            model.instance.mulGlobalTransform(poseStack, flareBone.index())
        }
        if (flarePosition != null) {
            poseStack.translate(flarePosition.x, flarePosition.y + 0.02, -flarePosition.z)
        }

        val scaleRandom = Math.random().toFloat()
        val rotationRandom = Math.random().toFloat()
        val size = resource.flareSize * (0.6f + 0.8f * scaleRandom) * muzzleFlashScale.coerceAtLeast(0f)
        poseStack.mulPose(Axis.ZP.rotation(0.5f * (rotationRandom - 0.5f)))
        poseStack.scale(size, size, 1f)

        val consumer = bufferSource.getBuffer(ModRenderTypes.MUZZLE_FLASH_TYPE.apply(FLARE_TEXTURE))
        val pose = poseStack.last().pose()
        vertex(consumer, pose, 0f, 0f, 0, 1)
        vertex(consumer, pose, 1f, 0f, 1, 1)
        vertex(consumer, pose, 1f, 1f, 1, 0)
        vertex(consumer, pose, 0f, 1f, 0, 0)
        poseStack.popPose()
    }

    private fun vertex(
        consumer: VertexConsumer,
        pose: Matrix4f,
        x: Float,
        y: Float,
        u: Int,
        v: Int
    ) {
        consumer.addVertex(pose, x - 0.5f, y - 0.5f, 0f)
            .setColor(255, 255, 255, 255)
            .setUv(u.toFloat(), v.toFloat())
    }
}
