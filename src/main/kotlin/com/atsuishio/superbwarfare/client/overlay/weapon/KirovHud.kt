package com.atsuishio.superbwarfare.client.overlay.weapon

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.overlay.CompassHud
import com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay.renderKillIndicatorDynamic
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.tools.canBeSeen
import com.atsuishio.superbwarfare.tools.mc
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
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.fml.common.EventBusSubscriber
import org.joml.Math

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
object KirovHud {
    const val ID: String = "@Kirov"
    private val BOMB_SCOPE = loc("textures/overlay/vehicle/aircraft/bomb_scope.png")
    private val BOMB_SCOPE_PITCH = loc("textures/overlay/vehicle/aircraft/bomb_scope_pitch.png")
    private val BOMB_RING = loc("textures/overlay/crosshair/rex_circle.png")

    private val compassHud = CompassHud().apply {
        x = 130f
        y = -76f  // 距底部 72+4 = 76 像素
        size = 72f
    }

    fun render(
        vehicle: VehicleEntity,
        player: Player,
        guiGraphics: GuiGraphics,
        partialTick: Float,
        screenWidth: Int,
        screenHeight: Int
    ) {
        if (player !== vehicle.getFirstPassenger()) return
        val poseStack = guiGraphics.pose()
        val gunData = vehicle.getGunData(player) ?: return

        poseStack.pushPose()

        val bomb = gunData.get(GunProp.CROSSHAIR) == "@AirBomb"

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

        val bombHitPosO = ClientEventHandler.bombHitPosO
        val bombHitPos = ClientEventHandler.bombHitPos
        var bombHitPosX = Mth.lerp(partialTick.toDouble(), bombHitPosO.x, bombHitPos.x)
        var bombHitPosY = Mth.lerp(partialTick.toDouble(), bombHitPosO.y, bombHitPos.y)
        val bombHitPosZ = Mth.lerp(partialTick.toDouble(), bombHitPosO.z, bombHitPos.z)
        val posCross = Vec3(bombHitPosX, bombHitPosY, bombHitPosZ)
        val pCross = posCross.worldToScreen()

        // 投弹准星
        if (bomb && ClientEventHandler.zoomVehicle) {
            if (posCross.canBeSeen()) {
                val f = Math.min(screenWidth, screenHeight).toFloat()
                val f1 = Math.min(screenWidth.toFloat() / f, screenHeight.toFloat() / f)
                val i = Mth.floor(f * f1)
                val j = Mth.floor(f * f1)

                val x = screenWidth.toFloat() / 2
                val y = screenHeight.toFloat() / 2

                poseStack.pushPose()
                poseStack.translate(x, y, 0f)
                val component = vehicle.thirdPersonAmmoComponent(gunData, player)
                guiGraphics.drawString(mc.font, component, 25, -11, 1, false)
                poseStack.popPose()

                RenderHelper.preciseBlit(
                    guiGraphics,
                    BOMB_SCOPE,
                    x - 1.5f * i,
                    y - 1.5f * j,
                    0f,
                    0f,
                    (3 * i).toFloat(),
                    (3 * j).toFloat(),
                    (3 * i).toFloat(),
                    (3 * j).toFloat()
                )

                poseStack.pushPose()
                poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), x, y, 0f)
                RenderHelper.preciseBlit(
                    guiGraphics,
                    BOMB_SCOPE_PITCH,
                    x - 1.5f * i,
                    y - 1.5f * j - 4 * vehicle.getPitch(partialTick),
                    0f,
                    0f,
                    (3 * i).toFloat(),
                    (3 * j).toFloat(),
                    (3 * i).toFloat(),
                    (3 * j).toFloat()
                )
                renderKillIndicatorDynamic(
                    guiGraphics,
                    x - 7.5f + (2 * (Math.random() - 0.5f)).toFloat(),
                    y - 7.5f + (2 * (Math.random() - 0.5f)).toFloat()
                )
                poseStack.popPose()
                return
            }
        }

        // 指南针
        compassHud.render(guiGraphics, vehicle, screenWidth, screenHeight, partialTick)

        poseStack.pushPose()
        poseStack.pushPose()

        if (posCross.canBeSeen()) {
            val x = pCross.x.toFloat()
            val y = pCross.y.toFloat()

            var xCross = x
            var yCross = y

            if (gunData.get(GunProp.CROSSHAIR) == "@AirBomb") {
                OldAircraftHud.bombHitPosX = Mth.lerp(0.25 * partialTick.toDouble(), OldAircraftHud.bombHitPosX, x.toDouble())
                OldAircraftHud.bombHitPosY = Mth.lerp(0.25 * partialTick.toDouble(), OldAircraftHud.bombHitPosY, y.toDouble())
                xCross = OldAircraftHud.bombHitPosX.toFloat()
                yCross = OldAircraftHud.bombHitPosY.toFloat()
            }

            if (mc.options.cameraType != CameraType.FIRST_PERSON && !ClientEventHandler.zoomVehicle) {
                val cross = BOMB_RING
                val size = 24f

                poseStack.pushPose()
                poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), xCross, yCross, 0f)
                poseStack.pushPose()
                poseStack.translate(xCross, yCross, 0f)
                poseStack.scale(0.75f, 0.75f, 1f)

                val heat = vehicle.getWeaponHeat(player) / 100f
                val component = vehicle.thirdPersonAmmoComponent(gunData, player)

                guiGraphics.drawString(mc.font, component, 25, -9, Mth.hsvToRgb(0f, heat, 1f), false)
                if (vehicle.hasDecoy()) {
                    if (vehicle.decoyReady) {
                        guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            Component.translatable("tips.superbwarfare.flare.ready").append(
                                Component.literal(
                                    " [" + ModKeyMappings.RELEASE_DECOY.key.displayName.string + "]"
                                )
                            ),
                            25,
                            1,
                            -1,
                            false
                        )
                    } else {
                        guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            Component.translatable("tips.superbwarfare.flare.reloading"),
                            25,
                            1,
                            0xFF0000,
                            false
                        )
                    }
                }

                poseStack.popPose()

                RenderHelper.preciseBlit(
                    guiGraphics,
                    cross,
                    xCross - 0.5f * size,
                    yCross - 0.5f * size,
                    0f,
                    0f,
                    size,
                    size,
                    size,
                    size
                )

                renderKillIndicatorDynamic(
                    guiGraphics,
                    xCross - 7.5f + (2 * (Math.random() - 0.5f)).toFloat(),
                    yCross - 7.5f + (2 * (Math.random() - 0.5f)).toFloat()
                )
                poseStack.popPose()
            }
        }
        poseStack.popPose()
        poseStack.popPose()
    }
}
