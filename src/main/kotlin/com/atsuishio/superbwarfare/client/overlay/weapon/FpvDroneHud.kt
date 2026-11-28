package com.atsuishio.superbwarfare.client.overlay.weapon

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.overlay.DecoyOverlayHelper
import com.atsuishio.superbwarfare.client.overlay.OverlayTraceHandler
import com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay.renderKillIndicatorDynamic
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay.renderEnergyInfo
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getXRotFromVector
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.event.ClientMouseHandler
import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import com.atsuishio.superbwarfare.tools.MathTool.getGradientColor
import com.atsuishio.superbwarfare.tools.canBeSeen
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.worldToScreen
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.math.Axis
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.gui.overlay.ForgeGui
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Math

/**
 * FPV 无人机 HUD：复制自 [HelicopterHud]，并适配了无人机没有座舱武器（getGunData 为 null）的情况。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(Dist.CLIENT)
object FpvDroneHud {
    const val ID: String = "@FPVDrone"

    val DRONE_BASE = loc("textures/overlay/vehicle/drone/drone_base.png")
    val HUD_LINE = loc("textures/overlay/vehicle/drone/hud_line.png")
    val COMPASS = loc("textures/overlay/vehicle/drone/compass.png")
    val BLOCK = loc("textures/overlay/misc/block_white.png")


    private var lerpVy = 1f


    fun render(
        vehicle: VehicleEntity,
        player: Player,
        gui: ForgeGui,
        guiGraphics: GuiGraphics,
        partialTick: Float,
        screenWidth: Int,
        screenHeight: Int
    ) {
        val mc = gui.minecraft
        val poseStack = guiGraphics.pose()
        val index = vehicle.getSeatIndex(player)
        val data = vehicle.getGunData(index)
        val color = vehicle.hudColor

        poseStack.pushPose()
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


        val speed = vehicle.deltaMovement.length() * 72
        lerpVy = Mth.lerp((0.021f * partialTick).toDouble(), lerpVy.toDouble(), vehicle.deltaMovement.y() * 20).toFloat()

        val x = screenWidth / 2f
        val y = screenHeight / 2f

        RenderHelper.blit(poseStack, DRONE_BASE,
            screenWidth / 2f - 128,
            screenHeight / 2f - 128,
            256f,
            256f,
            0f,
            0f,
            256f,
            256f,
            color)

        RenderHelper.blit(
            poseStack,
            COMPASS,
            screenWidth.toFloat() / 2 - 128,
            6f,
            256f,
            17f,
            128 + (64f / 45 * VehicleHudQuat.yaw(vehicle, partialTick)),
            0f,
            512f,
            17f,
            color
        )

        poseStack.pushPose()
        poseStack.rotateAround(
            Axis.ZP.rotationDegrees(-VehicleHudQuat.roll(vehicle, partialTick)),
            screenWidth / 2f,
            screenHeight / 2f,
            0f
        )
        val pitch = VehicleHudQuat.pitch(vehicle, partialTick)
        RenderHelper.blit(
            poseStack,
            HUD_LINE,
            screenWidth / 2f - 66.5f,
            screenHeight / 2f - 1 - 0.5f * pitch,
            137f,
            2f,
            0f,
            0f,
            137f,
            2f,
            color
        )
        poseStack.popPose()

        poseStack.pushPose()
        poseStack.rotateAround(
            Axis.ZP.rotationDegrees(VehicleHudQuat.roll(vehicle, partialTick)),
            screenWidth / 2f,
            screenHeight / 2f - 56,
            0f
        )

        poseStack.popPose()

//        RenderHelper.blit(
//            poseStack,
//            HELI_VY_MOVE,
//            screenWidth.toFloat() / 2 + 138,
//            (screenHeight.toFloat() / 2 - 3 - Mth.clamp(lerpVy * 3, -24f, 24f) * 2.5f),
//            8f,
//            8f,
//            0f,
//            0f,
//            8f,
//            8f,
//            color
//        )
//
//        guiGraphics.drawString(
//            Minecraft.getInstance().font,
//            Component.literal(format0D(lerpVy.toDouble(), "m/s")),
//            screenWidth / 2 + 146,
//            (screenHeight / 2f - 3 - Mth.clamp(lerpVy * 3, -24f, 24f) * 2.5).toInt(),
//            (if (lerpVy < -12) -65536 else color),
//            false
//        )
//        guiGraphics.drawString(
//            Minecraft.getInstance().font, Component.literal(format0D(vehicle.y)),
//            screenWidth / 2 + 104, screenHeight / 2, color, false
//        )
//
//        guiGraphics.drawString(
//            Minecraft.getInstance().font, Component.literal(format0D(speed, "km/h")),
//            screenWidth / 2 - 140, screenHeight / 2, color, false
//        )

        renderEnergyInfo(vehicle, guiGraphics, screenWidth, screenHeight, mc.font)

        renderKillIndicatorDynamic(
            guiGraphics,
            x - 8f,
            y - 8f
        )
        poseStack.popPose()
    }
}
