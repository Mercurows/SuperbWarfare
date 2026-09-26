package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.isBarrelSilenced
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

    /**
     * 画一簇枪口焰。
     *
     * 挂点三选一，**互斥**而不是层层回退：
     * 1. [subWeaponFlareTransform]：副武器开火（`ClientEventHandler.subWeaponFireRotTimer > 0`）——
     *    只画在**副武器模型自己的** `flare` 骨骼上；没解析到就**什么都不画**，
     *    绝不会退回主武器的枪口（榴弹从下挂筒里出去，枪管前端不该喷火）；
     * 2. [attachmentMuzzleTransform]：枪口配件自带枪口（消音器/制退器这类）；
     * 3. 主武器模型的 `flare`（退回 `muzzle_flash`）骨骼，再叠数据里的 `FlarePosition`。
     *
     * 可见窗口来自两个计时器之一 —— 主武器看 `fireRotTimer`，副武器看
     * `subWeaponFireRotTimer`（阈值相同，见各自的 KDoc）；两者都归 0 时不画。
     *
     * @param muzzleFlashScale 枪口配件的火焰缩放（`AttachmentDefinition.MuzzleFlashScale`）
     * @param subWeaponFlareTransform 副武器模型的 `flare` 骨骼在枪姿态空间里的变换
     *   （`GeoGunRenderer.resolveSubWeaponFlareTransform`）
     * @param subWeaponFlashScale 副武器定义里的 `MuzzleFlashScale`
     */
    fun render(
        poseStack: PoseStack,
        model: GeoGunModel,
        stack: ItemStack,
        bufferSource: MultiBufferSource,
        attachmentMuzzleTransform: Matrix4f? = null,
        muzzleFlashScale: Float = 1.0f,
        subWeaponFlareTransform: Matrix4f? = null,
        subWeaponFlashScale: Float = 1.0f,
    ) {
        val subWeaponTimer = ClientEventHandler.subWeaponFireRotTimer
        val subWeapon = subWeaponTimer > 0.0
        val fireRotTimer = if (subWeapon) subWeaponTimer else ClientEventHandler.fireRotTimer
        if (fireRotTimer <= 0.0 || fireRotTimer >= MAX_VISIBLE_TIME) return

        // 消音器只消**它自己那根枪管**的火焰：副武器开火时枪管根本没参与
        if (!subWeapon && GunData.from(stack).isBarrelSilenced()) return

        val resource = GunResource.compute(stack)
        val flareBone = model.getBone(FLARE_BONE) ?: model.getBone(MUZZLE_FLASH_BONE)
        // 副武器开火不叠主武器的 FlarePosition：挂点就是副武器模型里那根 `flare` 骨骼
        val flarePosition = if (subWeapon) null else resource.flarePosition

        val flareTransform = when {
            subWeapon -> subWeaponFlareTransform ?: return
            attachmentMuzzleTransform != null -> attachmentMuzzleTransform
            else -> null
        }
        if (flareTransform == null && flareBone == null && flarePosition == null) return

        poseStack.pushPose()
        if (flareTransform != null) {
            poseStack.last().pose().mul(flareTransform)
        } else if (flareBone != null) {
            model.instance.mulGlobalTransform(poseStack, flareBone.index())
        }
        if (flarePosition != null) {
            poseStack.translate(flarePosition.x, flarePosition.y + 0.02, -flarePosition.z)
        }

        val scaleRandom = Math.random().toFloat()
        val rotationRandom = Math.random().toFloat()
        val flashScale = if (subWeapon) subWeaponFlashScale else muzzleFlashScale
        val size = resource.flareSize * (0.6f + 0.8f * scaleRandom) * flashScale.coerceAtLeast(0f)
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
