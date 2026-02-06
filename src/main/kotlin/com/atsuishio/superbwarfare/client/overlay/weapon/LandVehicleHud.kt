package com.atsuishio.superbwarfare.client.overlay.weapon

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay.renderEnergyInfo
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import com.atsuishio.superbwarfare.tools.MathTool.getGradientColor
import com.atsuishio.superbwarfare.tools.TraceTool
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.math.Axis
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.joml.Math

@OnlyIn(Dist.CLIENT)
object LandVehicleHud {
    const val ID: String = "@Land"

    private val COMPASS = loc("textures/overlay/vehicle/base/compass.png")
    private val ROLL_IND = loc("textures/overlay/vehicle/helicopter/roll_ind.png")

    // 地面载具车身显示
    private val FRAME = loc("textures/overlay/vehicle/land/tv_frame.png")
    private val LINE = loc("textures/overlay/vehicle/land/line.png")
    private val BARREL = loc("textures/overlay/vehicle/land/line.png")
    private val BODY = loc("textures/overlay/vehicle/land/body.png")
    private val LEFT_WHEEL = loc("textures/overlay/vehicle/land/left_wheel.png")
    private val RIGHT_WHEEL = loc("textures/overlay/vehicle/land/right_wheel.png")
    private val ENGINE = loc("textures/overlay/vehicle/land/engine.png")

    var lerpRecoil: Float = 0f

    fun render(
        vehicle: VehicleEntity,
        player: LocalPlayer,
        gui: GuiGraphics,
        partialTick: Float,
        screenWidth: Int,
        screenHeight: Int
    ) {
        val mc = Minecraft.getInstance()

        if (vehicle.getSeatIndex(player) != vehicle.computed().turretControllerIndex) return

        val poseStack = gui.pose()

        val camera = mc.gameRenderer.mainCamera
        val cameraPos = camera.position
        val viewVec = Vec3(camera.lookVector)

        val color = vehicle.hudColor

        poseStack.pushPose()

        val recoil = Mth.lerp(partialTick, vehicle.recoilShakeO.toFloat(), vehicle.recoilShake.toFloat())
        lerpRecoil = Mth.lerp(0.1f * partialTick, lerpRecoil, recoil * (2 * (Math.random() - 0.5f)).toFloat())
        poseStack.translate(
            lerpRecoil * 6 + screenWidth * 0.025f * recoil,
            recoil * 3 + screenHeight * 0.025f * recoil,
            0f
        )
        poseStack.scale(1 - recoil * 0.05f, 1 - recoil * 0.05f, 1f)
        poseStack.rotateAround(
            Axis.ZP.rotationDegrees(-0.3f * ClientEventHandler.cameraRoll + 2.5f * lerpRecoil),
            screenWidth / 2f,
            screenHeight / 2f,
            0f
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

        if (Minecraft.getInstance().options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) {
            val addW = (screenWidth / screenHeight) * 48
            val addH = (screenWidth / screenHeight) * 27
            RenderHelper.preciseBlit(
                gui,
                FRAME,
                -addW.toFloat() / 2,
                -addH.toFloat() / 2,
                10f,
                0f,
                0f,
                (screenWidth + addW).toFloat(),
                (screenHeight + addH).toFloat(),
                (screenWidth + addW).toFloat(),
                (screenHeight + addH).toFloat()
            )
            RenderHelper.preciseBlitWithColor(
                gui,
                LINE,
                screenWidth / 2f - 64,
                (screenHeight - 56).toFloat(),
                0f,
                0f,
                128f,
                1f,
                128f,
                1f,
                color
            )

            // 指南针
            RenderHelper.preciseBlitWithColor(
                gui,
                COMPASS,
                screenWidth.toFloat() / 2 - 128,
                10f,
                128 + (64f / 45 * player.yRot),
                0f,
                256f,
                16f,
                512f,
                16f,
                color
            )
            RenderHelper.preciseBlitWithColor(
                gui,
                ROLL_IND,
                screenWidth / 2f - 8,
                30f,
                0f,
                0f,
                16f,
                16f,
                16f,
                16f,
                color
            )

            val turretHeal = (100 - (100 * vehicle.turretHealth / vehicle.getTurretMaxHealth())).toInt()
            RenderHelper.preciseBlitWithColor(
                gui,
                BARREL,
                screenWidth / 2f + 112,
                (screenHeight - 71).toFloat(),
                0f,
                0f,
                1f,
                16f,
                1f,
                16f,
                getGradientColor(color, 0xFF0000, turretHeal, 2)
            )

            // 车身方向
            poseStack.pushPose()
            poseStack.rotateAround(
                Axis.ZP.rotationDegrees(
                    Mth.lerp(
                        partialTick,
                        vehicle.turretYRotO,
                        vehicle.turretYRot
                    )
                ), screenWidth / 2f + 112, (screenHeight - 56).toFloat(), 0f
            )
            val bodyHeal = (100 - (100 * vehicle.health / vehicle.getMaxHealth())).toInt()
            RenderHelper.preciseBlitWithColor(
                gui,
                BODY,
                screenWidth / 2f + 96,
                (screenHeight - 72).toFloat(),
                0f,
                0f,
                32f,
                32f,
                32f,
                32f,
                getGradientColor(color, 0xFF0000, bodyHeal, 2)
            )
            val leftWheelHeal = (100 - (100 * vehicle.leftWheelHealth / vehicle.getWheelMaxHealth())).toInt()
            RenderHelper.preciseBlitWithColor(
                gui,
                LEFT_WHEEL,
                screenWidth / 2f + 96,
                (screenHeight - 72).toFloat(),
                0f,
                0f,
                32f,
                32f,
                32f,
                32f,
                getGradientColor(color, 0xFF0000, leftWheelHeal, 2)
            )
            val rightWheelHeal = (100 - (100 * vehicle.rightWheelHealth / vehicle.getWheelMaxHealth())).toInt()
            RenderHelper.preciseBlitWithColor(
                gui,
                RIGHT_WHEEL,
                screenWidth / 2f + 96,
                (screenHeight - 72).toFloat(),
                0f,
                0f,
                32f,
                32f,
                32f,
                32f,
                getGradientColor(color, 0xFF0000, rightWheelHeal, 2)
            )
            val engineHeal = (100 - (100 * vehicle.mainEngineHealth / vehicle.getEngineMaxHealth())).toInt()
            RenderHelper.preciseBlitWithColor(
                gui,
                ENGINE,
                screenWidth / 2f + 96,
                (screenHeight - 72).toFloat(),
                0f,
                0f,
                32f,
                32f,
                32f,
                32f,
                getGradientColor(color, 0xFF0000, engineHeal, 2)
            )
            poseStack.popPose()

            // 时速
            gui.drawString(
                mc.font,
                Component.literal(
                    format0D(
                        vehicle.deltaMovement.dot(vehicle.getViewVector(partialTick)) * 72,
                        " km/h"
                    )
                ),
                screenWidth / 2 + 160,
                screenHeight / 2 - 48,
                color,
                false
            )

            // 低电量警告
            renderEnergyInfo(vehicle, gui, screenWidth, screenHeight, mc.font)

            // 测距
            var lookAtEntity = false

            val result = player.level().clip(
                ClipContext(
                    player.eyePosition, player.eyePosition.add(player.getViewVector(1f).scale(512.0)),
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player
                )
            )
            val hitPos = result.getLocation()

            val blockRange = player.getEyePosition(1f).distanceTo(hitPos)
            var entityRange = 0.0

            val lookingEntity = TraceTool.camerafFindLookingEntity(player, cameraPos, viewVec, 512.0)
            if (lookingEntity != null) {
                lookAtEntity = true
                entityRange = player.distanceTo(lookingEntity).toDouble()
            }

            if (lookAtEntity) {
                val width = Minecraft.getInstance().font.width(format0D(entityRange, " m"))
                gui.drawString(
                    Minecraft.getInstance().font,
                    Component.literal(format0D(entityRange, " m")),
                    screenWidth / 2 - width / 2,
                    screenHeight - 53,
                    color,
                    false
                )
            } else {
                if (blockRange > 500) {
                    val width = Minecraft.getInstance().font.width("---m")
                    gui.drawString(
                        Minecraft.getInstance().font,
                        Component.literal("---m"),
                        screenWidth / 2 - width / 2,
                        screenHeight - 53,
                        color,
                        false
                    )
                } else {
                    val width = Minecraft.getInstance().font.width(format0D(blockRange, " m"))
                    gui.drawString(
                        Minecraft.getInstance().font,
                        Component.literal(format0D(blockRange, " m")),
                        screenWidth / 2 - width / 2,
                        screenHeight - 53,
                        color,
                        false
                    )
                }
            }

            // 血量
            val heal = (100 - (100 * vehicle.health / vehicle.getMaxHealth())).toInt()
            gui.drawString(
                Minecraft.getInstance().font,
                Component.literal(format0D((100 - heal).toDouble())),
                screenWidth / 2 - 165,
                screenHeight / 2 - 46,
                getGradientColor(color, 0xFF0000, bodyHeal, 2),
                false
            )

            // 诱饵
            if (vehicle.hasDecoy() && player === vehicle.getFirstPassenger()) {
                if (vehicle.decoyReady) {
                    gui.drawString(
                        Minecraft.getInstance().font,
                        Component.translatable("tips.superbwarfare.smoke.ready").append(
                            Component.literal(
                                " [" + ModKeyMappings.RELEASE_DECOY.key.displayName.string + "]"
                            )
                        ),
                        screenWidth / 2 - 165,
                        screenHeight / 2 - 36,
                        color,
                        false
                    )
                } else {
                    gui.drawString(
                        Minecraft.getInstance().font,
                        Component.translatable("tips.superbwarfare.smoke.reloading"),
                        screenWidth / 2 - 165,
                        screenHeight / 2 - 36,
                        0xFF0000,
                        false
                    )
                }
            }

            VehicleMainWeaponHudOverlay.renderWeaponInfoFirst(
                gui,
                vehicle,
                player,
                vehicle.getGunData(player)!!,
                mc.font,
                screenWidth,
                screenHeight,
                color
            )
        }
        poseStack.popPose()
    }
}
