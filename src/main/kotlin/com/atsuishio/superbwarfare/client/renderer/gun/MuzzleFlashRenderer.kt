package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.client.renderer.gun.MuzzleFlashRenderer.MAX_VISIBLE_TIME
import com.atsuishio.superbwarfare.client.renderer.gun.MuzzleFlashRenderer.isFiring
import com.atsuishio.superbwarfare.client.renderer.gun.MuzzleFlashRenderer.resolveFlareTransform
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
     * 开火窗口（枪口焰可见的那一小段）：主武器看 `fireRotTimer`，副武器看
     * `subWeaponFireRotTimer`，阈值共用 [MAX_VISIBLE_TIME]（见各自的 KDoc），两者都归 0 时不在窗口里。
     *
     * 子弹的"虚拟出膛点"也按这个窗口采样 —— 只有刚开过枪的那一小段里，枪口才代表弹体该出现的位置。
     */
    fun isFiring(): Boolean {
        val timer = if (ClientEventHandler.subWeaponFireRotTimer > 0.0) {
            ClientEventHandler.subWeaponFireRotTimer
        } else {
            ClientEventHandler.fireRotTimer
        }
        return timer > 0.0 && timer < MAX_VISIBLE_TIME
    }

    /**
     * 枪口挂点在**枪模型空间**里的变换。挂点三选一，**互斥**而不是层层回退：
     * 1. [subWeaponFlareTransform]：副武器开火（`ClientEventHandler.subWeaponFireRotTimer > 0`）——
     *    只取**副武器模型自己的** `flare` 骨骼；没解析到就返回 `null`（调用方什么都别画），
     *    绝不会退回主武器的枪口（榴弹从下挂筒里出去，枪管前端不该喷火）；
     * 2. [attachmentMuzzleTransform]：枪口配件自带枪口（消音器/制退器这类）；
     * 3. 主武器模型的 `flare`（退回 `muzzle_flash`）骨骼，再叠数据里的 `FlarePosition`。
     *
     * 枪口焰和子弹的虚拟出膛点共用它 —— 弹体必须从火焰钻出来的那一格钻出来，
     * 这份解析只该有一个出处。
     *
     * @return `null` = 这一帧没有可用的枪口
     */
    fun resolveFlareTransform(
        model: GeoGunModel,
        stack: ItemStack,
        attachmentMuzzleTransform: Matrix4f? = null,
        subWeaponFlareTransform: Matrix4f? = null,
    ): Matrix4f? {
        // 副武器开火：挂点只能是**副武器模型自己的** `flare`。解析不到就返回 `null`（调用方什么都别画），
        // 绝不退回主武器的枪口（榴弹从下挂筒里出去，枪管前端不该喷火），也不叠 `FlarePosition`
        if (ClientEventHandler.subWeaponFireRotTimer > 0.0) {
            return subWeaponFlareTransform?.let { Matrix4f(it) }
        }

        val flarePosition = GunResource.compute(stack).flarePosition
        val flareBone = model.getBone(FLARE_BONE) ?: model.getBone(MUZZLE_FLASH_BONE)
        if (attachmentMuzzleTransform == null && flareBone == null && flarePosition == null) return null

        // 拷贝一份再叠 `FlarePosition`：SBM 目前每次调用都新建矩阵，但别赌它以后不改
        val transform = attachmentMuzzleTransform?.let { Matrix4f(it) }
            ?: flareBone?.let { Matrix4f(model.getGlobalTransform(it.index())) }
            ?: Matrix4f()
        flarePosition?.let {
            transform.translate(it.x.toFloat(), (it.y + 0.02).toFloat(), (-it.z).toFloat())
        }
        return transform
    }

    /**
     * 画一簇枪口焰。
     *
     * 挂点由 [resolveFlareTransform] 解析（副武器 `flare` / 枪口配件的枪口 / 主武器 `flare`），
     * 可见窗口由 [isFiring] 判定；两者都按各自的 KDoc。
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
        val subWeapon = ClientEventHandler.subWeaponFireRotTimer > 0.0
        if (!isFiring()) return

        // 消音器只消**它自己那根枪管**的火焰：副武器开火时枪管根本没参与
        if (!subWeapon && GunData.from(stack).isBarrelSilenced()) return

        val flareTransform = resolveFlareTransform(model, stack, attachmentMuzzleTransform, subWeaponFlareTransform)
            ?: return

        val resource = GunResource.compute(stack)

        poseStack.pushPose()
        poseStack.last().pose().mul(flareTransform)

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
