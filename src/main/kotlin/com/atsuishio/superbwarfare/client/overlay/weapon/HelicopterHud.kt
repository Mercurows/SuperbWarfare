package com.atsuishio.superbwarfare.client.overlay.weapon

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay.renderKillIndicatorDynamic
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay.renderEnergyInfo
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay.renderWeaponInfoThird
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getXRotFromVector
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getYRotFromVector
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import com.atsuishio.superbwarfare.tools.MathTool.getGradientColor
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.VectorUtil
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.math.Axis
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.gui.overlay.ForgeGui
import org.joml.Math

@OnlyIn(Dist.CLIENT)
object HelicopterHud {
    const val ID: String = "@Helicopter"

    private val HELI_BASE = loc("textures/overlay/vehicle/helicopter/heli_base.png")
    private val ROLL_IND = loc("textures/overlay/vehicle/helicopter/roll_ind.png")
    private val HELI_LINE = loc("textures/overlay/vehicle/helicopter/heli_line.png")
    private val HELI_POWER_RULER = loc("textures/overlay/vehicle/helicopter/heli_power_ruler.png")
    private val HELI_POWER = loc("textures/overlay/vehicle/helicopter/heli_power.png")
    private val HELI_VY_MOVE = loc("textures/overlay/vehicle/helicopter/heli_vy_move.png")
    private val SPEED_FRAME = loc("textures/overlay/vehicle/helicopter/speed_frame.png")
    private val CROSSHAIR_IND = loc("textures/overlay/vehicle/helicopter/crosshair_ind.png")
    private val HELI_DRIVER_ANGLE = loc("textures/overlay/vehicle/helicopter/heli_driver_angle.png")
    private val FRAME = loc("textures/overlay/vehicle/land/tv_frame.png")
    private val LINE = loc("textures/overlay/vehicle/land/line.png")

    private val COMPASS = loc("textures/overlay/vehicle/base/compass.png")
    private val CROSSHAIR_3P = loc("textures/overlay/vehicle/crosshair/third_camera.png")

    private var scopeScale = 1f
    private var lerpVy = 1f
    private var lerpPower = 1f

    fun render(
        vehicle: VehicleEntity,
        player: Player,
        gui: ForgeGui,
        guiGraphics: GuiGraphics,
        partialTick: Float,
        screenWidth: Int,
        screenHeight: Int
    ) {
        val mc = gui.getMinecraft()
        val poseStack = guiGraphics.pose()
        val index = vehicle.getSeatIndex(player)
        val data = vehicle.getGunData(index)
        if (data == null) {
            scopeScale = 0.7f
            return
        }
        val color = vehicle.hudColor

        if (vehicle.getSeatIndex(player) == vehicle.computed().turretControllerIndex && vehicle.hasTurret()) {
            if (ClientEventHandler.zoomVehicle) {
                // 武器名

                VehicleMainWeaponHudOverlay.renderWeaponInfoFirst(
                    guiGraphics,
                    vehicle,
                    player,
                    vehicle.getGunData(player)!!,
                    mc.font,
                    screenWidth,
                    screenHeight,
                    color
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

                // 指南针
                RenderHelper.blit(
                    poseStack,
                    COMPASS,
                    screenWidth.toFloat() / 2 - 128,
                    10f,
                    (128 - (64f / 45 * getYRotFromVector(vehicle.getBarrelVector(partialTick)))).toFloat(),
                    0f,
                    256f,
                    16f,
                    512f,
                    16f,
                    color
                )
                RenderHelper.blit(poseStack, ROLL_IND, screenWidth / 2f - 8, 30f, 0f, 0f, 16f, 16f, 16f, 16f, color)

                // 电视
                val addW = (screenWidth / screenHeight) * 48
                val addH = (screenWidth / screenHeight) * 27
                RenderHelper.preciseBlit(
                    guiGraphics,
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
                RenderHelper.blit(
                    poseStack,
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

                // 时速
                guiGraphics.drawString(
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
                guiGraphics.drawString(
                    Minecraft.getInstance().font, Component.literal(format0D(vehicle.y, " m")),
                    screenWidth / 2 + 160, screenHeight / 2 - 39, color, false
                )

                // 低电量警告
                renderEnergyInfo(vehicle, guiGraphics, screenWidth, screenHeight, mc.font)

                // 测距
                var lookAtEntity = false

                val result = player.level().clip(
                    ClipContext(
                        vehicle.getShootPosForHud(player, partialTick),
                        vehicle.getShootPosForHud(player, partialTick)
                            .add(vehicle.getShootDirectionForHud(player, partialTick).scale(512.0)),
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.NONE,
                        player
                    )
                )
                val hitPos = result.getLocation()

                val blockRange = player.getEyePosition(1f).distanceTo(hitPos)
                var entityRange = 0.0

                val lookingEntity = TraceTool.camerafFindLookingEntity(
                    player,
                    vehicle.getShootPosForHud(player, partialTick),
                    vehicle.getShootDirectionForHud(player, partialTick),
                    512.0
                )
                if (lookingEntity != null) {
                    lookAtEntity = true
                    entityRange = player.distanceTo(lookingEntity).toDouble()
                }

                if (lookAtEntity) {
                    val width = Minecraft.getInstance().font.width(format0D(entityRange, " m"))
                    guiGraphics.drawString(
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
                        guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            Component.literal("---m"),
                            screenWidth / 2 - width / 2,
                            screenHeight - 53,
                            color,
                            false
                        )
                    } else {
                        val width = Minecraft.getInstance().font.width(format0D(blockRange, " m"))
                        guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            Component.literal(format0D(blockRange, " m")),
                            screenWidth / 2 - width / 2,
                            screenHeight - 53,
                            color,
                            false
                        )
                    }
                }
            }
        } else {
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

            scopeScale = Mth.lerp(partialTick, scopeScale, 1f)
            val f = Math.min(screenWidth, screenHeight).toFloat()
            val f1 = Math.min(screenWidth.toFloat() / f, screenHeight.toFloat() / f) * scopeScale
            val i = Mth.floor(f * f1).toFloat()
            val j = Mth.floor(f * f1).toFloat()
            val k = (screenWidth - i) / 2f
            val l = (screenHeight - j) / 2f

            val shootPos = vehicle.getShootPosForHud(player, partialTick)

            val result = player.level().clip(
                ClipContext(
                    shootPos, shootPos.add(vehicle.getShootDirectionForHud(player, partialTick).scale(512.0)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
                )
            )
            val hitPos = result.getLocation()

            var dis = shootPos.distanceTo(hitPos)

            val lookingEntity = vehicle.getPlayerLookAtEntityOnVehicle(player, 512.0, partialTick)

            if (lookingEntity != null) {
                dis = shootPos.distanceTo(lookingEntity.position())
            }

            val pos = shootPos.add(vehicle.getShootDirectionForHud(player, partialTick).scale(dis))
            val screenPos = VectorUtil.worldToScreen(pos)
            val speed = vehicle.deltaMovement.length() * 72
            val height = vehicle.position().distanceTo(
                (Vec3.atLowerCornerOf(
                    vehicle.level().clip(
                        ClipContext(
                            vehicle.position(), vehicle.position().add(Vec3(0.0, -1.0, 0.0).scale(100.0)),
                            ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, vehicle
                        )
                    ).blockPos
                ))
            )
            val blockInWay = vehicle.position().distanceTo(
                (Vec3.atLowerCornerOf(
                    vehicle.level().clip(
                        ClipContext(
                            vehicle.position(),
                            vehicle.position()
                                .add(vehicle.deltaMovement.add(0.0, 0.06, 0.0).normalize().scale(100.0)),
                            ClipContext.Block.OUTLINE,
                            ClipContext.Fluid.ANY,
                            vehicle
                        )
                    ).blockPos
                ))
            )

            val x = screenPos.x.toFloat()
            val y = screenPos.y.toFloat()

            if (Minecraft.getInstance().options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) {
                RenderHelper.blit(poseStack, HELI_BASE, k, l, 0f, 0f, i, j, i, j, color)

                val diffY = -Mth.lerp(partialTick, vehicle.turretYRotO, vehicle.turretYRot) * 0.3f
                val diffX = (Mth.wrapDegrees(
                    -getXRotFromVector(vehicle.getBarrelVector(partialTick)) - Mth.lerp(
                        partialTick,
                        vehicle.xRotO,
                        vehicle.xRot
                    )
                ) * 0.072f).toFloat()
                RenderHelper.blit(poseStack, HELI_DRIVER_ANGLE, k + diffY, l + diffX, 0f, 0f, i, j, i, j, color)

                RenderHelper.blit(
                    poseStack,
                    COMPASS,
                    screenWidth.toFloat() / 2 - 128,
                    6f,
                    128 + (64f / 45 * vehicle.yRot),
                    0f,
                    256f,
                    16f,
                    512f,
                    16f,
                    color
                )

                poseStack.pushPose()
                poseStack.rotateAround(
                    Axis.ZP.rotationDegrees(-vehicle.getRoll(partialTick)),
                    screenWidth / 2f,
                    screenHeight / 2f,
                    0f
                )
                val pitch = vehicle.getPitch(partialTick)
                RenderHelper.blit(
                    poseStack,
                    HELI_LINE,
                    screenWidth.toFloat() / 2 - 128,
                    screenHeight.toFloat() / 2 - 512 - 5.475f * pitch,
                    0f,
                    0f,
                    256f,
                    1024f,
                    256f,
                    1024f,
                    color
                )
                poseStack.popPose()

                poseStack.pushPose()
                poseStack.rotateAround(
                    Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)),
                    screenWidth / 2f,
                    screenHeight / 2f - 56,
                    0f
                )
                RenderHelper.blit(
                    poseStack,
                    ROLL_IND,
                    screenWidth.toFloat() / 2 - 8,
                    screenHeight.toFloat() / 2 - 88,
                    0f,
                    0f,
                    16f,
                    16f,
                    16f,
                    16f,
                    color
                )
                poseStack.popPose()

                RenderHelper.blit(
                    poseStack,
                    HELI_POWER_RULER,
                    screenWidth.toFloat() / 2 + 100,
                    screenHeight.toFloat() / 2 - 64,
                    0f,
                    0f,
                    64f,
                    128f,
                    64f,
                    128f,
                    color
                )

                val power = vehicle.power
                lerpPower = Mth.lerp(0.001f * partialTick, lerpPower, power)
                RenderHelper.blit(
                    poseStack,
                    HELI_POWER,
                    screenWidth.toFloat() / 2 + 130f,
                    (screenHeight.toFloat() / 2 - 64 + 124 - power * 980),
                    0f,
                    0f,
                    4f,
                    power * 980,
                    4f,
                    power * 980,
                    color
                )

                lerpVy =
                    Mth.lerp((0.021f * partialTick).toDouble(), lerpVy.toDouble(), vehicle.deltaMovement.y() * 20)
                        .toFloat()
                RenderHelper.blit(
                    poseStack,
                    HELI_VY_MOVE,
                    screenWidth.toFloat() / 2 + 138,
                    (screenHeight.toFloat() / 2 - 3 - Math.max(lerpVy, -24f) * 2.5f),
                    0f,
                    0f,
                    8f,
                    8f,
                    8f,
                    8f,
                    color
                )

                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    Component.literal(format0D(lerpVy.toDouble(), "m/s")),
                    screenWidth / 2 + 146,
                    (screenHeight / 2f - 3 - Math.max(lerpVy, -24f) * 2.5).toInt(),
                    (if (lerpVy < -24 || ((lerpVy < -10 || (lerpVy < -1 && speed > 100)) && height < 36) || (speed > 40 && blockInWay < 72)) -65536 else color),
                    false
                )
                guiGraphics.drawString(
                    Minecraft.getInstance().font, Component.literal(format0D(vehicle.y)),
                    screenWidth / 2 + 104, screenHeight / 2, color, false
                )
                RenderHelper.blit(
                    poseStack,
                    SPEED_FRAME,
                    screenWidth.toFloat() / 2 - 144,
                    screenHeight.toFloat() / 2 - 6,
                    0f,
                    0f,
                    50f,
                    18f,
                    50f,
                    18f,
                    color
                )
                guiGraphics.drawString(
                    Minecraft.getInstance().font, Component.literal(format0D(speed, "km/h")),
                    screenWidth / 2 - 140, screenHeight / 2, color, false
                )

                if (vehicle.hasDecoy()) {
                    if (vehicle.decoyReady) {
                        guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            Component.translatable("tips.superbwarfare.flare.ready").append(
                                Component.literal(
                                    " [" + ModKeyMappings.RELEASE_DECOY.key.displayName.string + "]"
                                )
                            ),
                            screenWidth / 2 - 160,
                            screenHeight / 2 - 50,
                            color,
                            false
                        )
                    } else {
                        guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            Component.translatable("tips.superbwarfare.flare.reloading"),
                            screenWidth / 2 - 160,
                            screenHeight / 2 - 50,
                            0xFF0000,
                            false
                        )
                    }
                }
                val component = vehicle.firstPersonAmmoComponent(data, player)

                val heat = vehicle.getWeaponHeat(player)
                guiGraphics.drawString(
                    mc.font, component, screenWidth / 2 - 160, screenHeight / 2 - 59,
                    getGradientColor(color, 0xFF0000, heat, 2), false
                )

                renderEnergyInfo(vehicle, guiGraphics, screenWidth, screenHeight, mc.font)

                RenderHelper.blit(poseStack, CROSSHAIR_IND, x - 8, y - 8, 0f, 0f, 16f, 16f, 16f, 16f, color)
                renderKillIndicatorDynamic(
                    guiGraphics,
                    x - 7.5f + (2 * (Math.random() - 0.5f)).toFloat(),
                    y - 7.5f + (2 * (Math.random() - 0.5f)).toFloat()
                )
            } else if (VectorUtil.canSee(pos)) {
                poseStack.pushPose()
                poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), x, y, 0f)
                RenderHelper.preciseBlit(guiGraphics, CROSSHAIR_3P, x - 8, y - 8, 0f, 0f, 16f, 16f, 16f, 16f)
                renderKillIndicatorDynamic(
                    guiGraphics,
                    x - 7.5f + (2 * (Math.random() - 0.5f)).toFloat(),
                    y - 7.5f + (2 * (Math.random() - 0.5f)).toFloat()
                )

                poseStack.pushPose()

                poseStack.translate(x, y, 0f)
                poseStack.scale(0.75f, 0.75f, 1f)

                renderWeaponInfoThird(guiGraphics, vehicle, player, data, mc.font)

                if (vehicle.hasDecoy()) {
                    if (vehicle.decoyReady) {
                        guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            Component.translatable("tips.superbwarfare.flare.ready").append(
                                Component.literal(
                                    " [" + ModKeyMappings.RELEASE_DECOY.key.displayName.string + "]"
                                )
                            ),
                            30,
                            1,
                            -1,
                            false
                        )
                    } else {
                        guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            Component.translatable("tips.superbwarfare.flare.reloading"),
                            30,
                            1,
                            0xFF0000,
                            false
                        )
                    }
                }

                poseStack.popPose()
                poseStack.popPose()
            }

            if (lerpVy < -16) {
                guiGraphics.drawString(
                    Minecraft.getInstance().font, Component.literal("SINK RATE，PULL UP!"),
                    screenWidth / 2 - 53, screenHeight / 2 + 24, -65536, false
                )
                if (player.tickCount % 30 == 0) {
                    player.level()
                        .playLocalSound(player.onPos, ModSounds.PULL_UP.get(), SoundSource.PLAYERS, 3f, 1f, false)
                }
            } else if (((lerpVy < -10 || (lerpVy < -3 && speed > 100)) && height < 36) || (speed > 72 && blockInWay < 72)) {
                guiGraphics.drawString(
                    Minecraft.getInstance().font, Component.literal("TERRAIN TERRAIN"),
                    screenWidth / 2 - 42, screenHeight / 2 + 24, -65536, false
                )
                if (player.tickCount % 30 == 0) {
                    player.level()
                        .playLocalSound(player.onPos, ModSounds.TERRAIN.get(), SoundSource.PLAYERS, 3f, 1f, false)
                }
            }
            poseStack.popPose()
        }
    }
}
