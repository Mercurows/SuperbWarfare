package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.VectorTool.lerpGetEntityBoundingBoxCenter
import com.atsuishio.superbwarfare.tools.worldToScreen
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.min

@OnlyIn(Dist.CLIENT)
object IglaHudOverlay : CommonOverlay("igla_9k38_hud") {
    private val FRAME = loc("textures/overlay/frame/frame_diamond.png")
    private val PART_1 = loc("textures/overlay/igla_9k38/part_1.png")
    private val PART_2 = loc("textures/overlay/igla_9k38/part_2.png")
    private val PART_3 = loc("textures/overlay/igla_9k38/part_3.png")
    private val PART_4 = loc("textures/overlay/igla_9k38/part_4.png")
    private val HOLD = loc("textures/overlay/igla_9k38/hold.png")
    private val SHOOT = loc("textures/overlay/igla_9k38/shoot.png")
    private val IGLA_SCOPE = loc("textures/overlay/igla_9k38/igla_scope.png")

    private var scopeScale = 1f
    private var lerpSeeking = 1f

    override fun shouldRender() = super.shouldRender() && !ClientEventHandler.isEditing

    override fun RenderContext.render() {
        val poseStack = guiGraphics.pose()
        val stack = player.mainHandItem

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.banHand(player)) return

        if (stack.item === ModItems.IGLA_9K38.get() && ClientEventHandler.zoomPos > 0.83 && isFirstPerson && ClientEventHandler.zoom) {
            val data = from(stack)

            poseStack.pushPose()

            val moveX =
                (-32 * ClientEventHandler.turnRot[1] - (if (player.isSprinting) 100 else 67) * ClientEventHandler.movePosX + 3 * ClientEventHandler.cameraRot[2]).toFloat()
            val moveY =
                (-32 * ClientEventHandler.turnRot[0] + 100 * ClientEventHandler.velocityY.toFloat() - (if (player.isSprinting) 100 else 67) * ClientEventHandler.movePosY - 12 * ClientEventHandler.boltMove + 3 * ClientEventHandler.cameraRot[1]).toFloat()
            scopeScale = Mth.lerp(
                (0.5f * deltaFrame).toDouble(),
                scopeScale.toDouble(),
                1.35f + (0.2f * ClientEventHandler.boltMove)
            ).toFloat()
            val f = min(screenWidth, screenHeight).toFloat()
            val f1: Float = min(screenWidth.toFloat() / f, screenHeight.toFloat() / f) * scopeScale
            val i = Mth.floor(f * f1).toFloat()
            val j = Mth.floor(f * f1).toFloat()
            val pCross = (camera.position.add(Vec3(camera.lookVector))).worldToScreen()
            val x0 = pCross.x.toFloat() + 4 * moveX
            val y0 = pCross.y.toFloat() + 4 * moveY

            val blockPos = player.blockPosition()
            val combinedLightLevel = player.level().getMaxLocalRawBrightness(blockPos)

            RenderSystem.disableDepthTest()
            RenderSystem.depthMask(false)
            RenderSystem.enableBlend()
            RenderSystem.setShader { GameRenderer.getPositionTexShader() }
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
            )
            RenderSystem.setShaderColor(
                combinedLightLevel.toFloat() / 15,
                combinedLightLevel.toFloat() / 15,
                combinedLightLevel.toFloat() / 15,
                1f
            )

            RenderHelper.preciseBlit(
                guiGraphics,
                IGLA_SCOPE,
                x0 - 1.5f * i,
                y0 - 1.5f * j,
                0f,
                0f,
                3 * i,
                3 * j,
                3 * i,
                3 * j
            )

            RenderSystem.disableDepthTest()
            RenderSystem.depthMask(false)
            RenderSystem.enableBlend()
            RenderSystem.setShader { GameRenderer.getPositionTexShader() }
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
            )
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            val decoy = TraceTool.findLookDecoy(player, cameraPos, player.getViewVector(deltaFrame), 512.0)

            if (decoy == null) {
                val targetEntity =
                    if (ClientEventHandler.lockOn) ClientEventHandler.lockingEntity else ClientEventHandler.seekingEntity
                val seekingTime = ClientEventHandler.seekingTime
                lerpSeeking = Mth.lerp(
                    partialTick,
                    lerpSeeking,
                    Mth.clamp(data.get(GunProp.SEEK_TIME) - seekingTime, 0, data.get(GunProp.SEEK_TIME)) * 0.6f
                )

                if (targetEntity != null) {
                    val pos = lerpGetEntityBoundingBoxCenter(targetEntity, partialTick)
                    val point = pos.worldToScreen()
                    val x = point.x.toFloat()
                    val y = point.y.toFloat()
                    poseStack.pushPose()

                    poseStack.translate(x, y, 0f)
                    //框
                    RenderHelper.blit(poseStack, FRAME, -12f, -12f, 0f, 0f, 24f, 24f, 24f, 24f, 1f)

                    //锁定进度
                    RenderHelper.blit(
                        poseStack,
                        PART_1,
                        -12 - lerpSeeking,
                        -12 - lerpSeeking,
                        0f,
                        0f,
                        24f,
                        24f,
                        24f,
                        24f,
                        1f
                    )
                    RenderHelper.blit(
                        poseStack,
                        PART_2,
                        -12 + lerpSeeking,
                        -12 - lerpSeeking,
                        0f,
                        0f,
                        24f,
                        24f,
                        24f,
                        24f,
                        1f
                    )
                    RenderHelper.blit(
                        poseStack,
                        PART_3,
                        -12 - lerpSeeking,
                        -12 + lerpSeeking,
                        0f,
                        0f,
                        24f,
                        24f,
                        24f,
                        24f,
                        1f
                    )
                    RenderHelper.blit(
                        poseStack,
                        PART_4,
                        -12 + lerpSeeking,
                        -12 + lerpSeeking,
                        0f,
                        0f,
                        24f,
                        24f,
                        24f,
                        24f,
                        1f
                    )

                    //状态
                    if (seekingTime >= data.get(GunProp.SEEK_TIME) && data.ammo.get() > 0) {
                        RenderHelper.blit(poseStack, SHOOT, -12f, -26f, 0f, 0f, 24f, 24f, 24f, 24f, 1f)
                    } else {
                        RenderHelper.blit(poseStack, HOLD, -12f, -26f, 0f, 0f, 24f, 24f, 24f, 24f, 1f)
                    }

                    //测距
                    poseStack.pushPose()
                    val range = format0D(player.distanceTo(targetEntity).toDouble())
                    val width = Minecraft.getInstance().font.width(range)
                    poseStack.scale(0.8f, 0.8f, 1f)
                    poseStack.translate(0.1f, 0f, 0f)
                    guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        Component.literal(range),
                        (-width.toFloat() / 2).toInt(),
                        14,
                        0xFFD6B6,
                        false
                    )
                    poseStack.popPose()

                    poseStack.popPose()
                }
            }
            poseStack.popPose()
        } else {
            scopeScale = 1f
        }
    }
}
