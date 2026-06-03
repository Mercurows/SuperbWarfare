package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.client.animation.AnimationCurves
import com.atsuishio.superbwarfare.client.animation.AnimationTimer
import com.atsuishio.superbwarfare.client.animation.ValueAnimator
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.ammo.AmmoSupplierItem
import com.atsuishio.superbwarfare.item.ammo.ammoBoxData
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.util.FastColor
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.roundToInt

@OnlyIn(Dist.CLIENT)
object AmmoCountOverlay : CommonOverlay("ammo_count") {

    private val ammoInfoTimer: AnimationTimer = AnimationTimer(500, 2000)
        .forwardAnimation(AnimationCurves.EASE_OUT_EXPO)
        .backwardAnimation(AnimationCurves.EASE_IN_EXPO)
    private val ammoBoxTimer: AnimationTimer = AnimationTimer(500)
        .forwardAnimation(AnimationCurves.EASE_OUT_EXPO)
        .backwardAnimation(AnimationCurves.EASE_IN_EXPO)

    private val ammoCountAnimators = ValueAnimator.create<Int?>(
        Ammo.entries.size, 800, 0
    )
    private val ammoBoxAnimators = ValueAnimator.create(
        Ammo.entries.size, 800, 0
    )

    /**
     * 在手持弹药或弹药盒时，渲染玩家弹药总量信息
     */
    override fun RenderContext.render() {
        var startRenderingAmmoInfo = false
        var isAmmoBox = false

        // 动画计算
        val currentTime = System.currentTimeMillis()
        val stack = player.mainHandItem
        val vehicle = player.vehicle
        if ((stack.item is AmmoSupplierItem || stack.item === ModItems.AMMO_BOX.get())
            && !(vehicle is VehicleEntity && vehicle.banHand(player))
        ) {
            // 刚拿出弹药物品时，视为开始弹药信息渲染
            startRenderingAmmoInfo = ammoInfoTimer.getProgress(currentTime) == 0f
            ammoInfoTimer.forward(currentTime)

            if (stack.item === ModItems.AMMO_BOX.get()) {
                isAmmoBox = true
                ammoBoxTimer.forward(currentTime)
            } else {
                ammoBoxTimer.backward(currentTime)
            }
        } else {
            ammoInfoTimer.backward(currentTime)
            ammoBoxTimer.backward(currentTime)
        }
        if (!ammoInfoTimer.isForward && ammoInfoTimer.finished(currentTime)) return

        val poseStack = guiGraphics.pose()
        poseStack.pushPose()

        val ammoX: Float = ammoInfoTimer.lerp((w + 120).toFloat(), w.toFloat() / 2 + 40, currentTime)
        val fontHeight = 15
        var yOffset = (-screenHeight - Ammo.entries.size * fontHeight) / 2f

        // 渲染总弹药数量
        val font = Minecraft.getInstance().font

        for (type in Ammo.entries) {
            val index = type.ordinal
            val ammoCount = type.get(player)
            val animator: ValueAnimator<Int?> = ammoCountAnimators[index]

            val boxAnimator: ValueAnimator<Int> = ammoBoxAnimators[index]
            var boxAmmoCount = boxAnimator.newValue()
            var boxAmmoSelected = false

            if (isAmmoBox) {
                val ammoBoxType = stack.ammoBoxData.type

                boxAmmoCount = type.get(stack)
                if (ammoBoxType == null || ammoBoxType == type) {
                    boxAnimator.forward(currentTime)
                    boxAmmoSelected = true
                } else {
                    boxAnimator.reset(boxAmmoCount)
                }
            }

            // 首次开始渲染弹药信息时，记录弹药数量，便于后续播放动画
            if (startRenderingAmmoInfo) {
                animator.reset(ammoCount)
                animator.endForward(currentTime)
                if (isAmmoBox) {
                    boxAnimator.reset(type.get(stack))
                    boxAnimator.endForward(currentTime)
                }
            }

            val ammoAdd = ammoCount.compareTo(animator.oldValue()!!)
            // 弹药数量变化时，更新并开始播放弹药数量更改动画
            animator.compareAndUpdate(ammoCount) {
                // 弹药数量变化时，开始播放弹药数量更改动画
                animator.beginForward(currentTime)
            }

            val progress = animator.getProgress(currentTime)
            val ammoCountStr =
                animator.lerp(animator.oldValue()!!.toFloat(), ammoCount.toFloat(), currentTime).roundToInt().toString()

            // 弹药增加时，颜色由绿变白，否则由红变白
            val fontColor = FastColor.ARGB32.lerp(
                progress, when (ammoAdd) {
                    1 -> -0xff0100
                    -1 -> -0x10000
                    else -> -0x1
                }, -0x1
            )

            RenderSystem.setShaderColor(1f, 1f, 1f, ammoInfoTimer.lerp(0f, 1f, currentTime))

            // 弹药数量
            guiGraphics.drawString(
                font,
                ammoCountStr,
                ammoX + (30 - font.width(ammoCountStr)),
                screenHeight + yOffset,
                fontColor,
                true
            )

            // 弹药类型
            guiGraphics.drawString(
                font,
                Component.translatable(type.translationKey).string,
                ammoX + 35,
                screenHeight + yOffset,
                fontColor,
                true
            )

            // 弹药盒信息渲染
            RenderSystem.setShaderColor(1f, 1f, 1f, ammoBoxTimer.lerp(0f, 1f, currentTime))
            val ammoBoxX: Float = ammoBoxTimer.lerp(-30f, screenWidth.toFloat() / 2, currentTime)

            val ammoBoxAdd = boxAmmoCount.compareTo(boxAnimator.oldValue())
            boxAnimator.compareAndUpdate(boxAmmoCount) { boxAnimator.beginForward(currentTime) }

            // 选中时显示为黄色，否则为白色
            val targetColor = if (boxAmmoSelected) -0x100 else -0x1

            val boxFontColor = FastColor.ARGB32.lerp(
                boxAnimator.getProgress(currentTime),
                when (ammoBoxAdd) {
                    1 -> -0xff0100
                    -1 -> -0x10000
                    else -> targetColor
                },
                targetColor
            )

            // 弹药盒内弹药数量
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                boxAnimator.lerp(boxAnimator.oldValue().toFloat(), boxAmmoCount.toFloat(), currentTime).roundToInt()
                    .toString(),
                ammoBoxX - 70,
                screenHeight + yOffset,
                boxFontColor,
                true
            )

            yOffset += fontHeight.toFloat()
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        poseStack.popPose()
    }
}
