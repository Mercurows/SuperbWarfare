package com.atsuishio.superbwarfare.client.overlay.weapon

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay.renderEnergyInfo
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import com.atsuishio.superbwarfare.tools.TraceTool
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.math.Axis
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.gui.overlay.ForgeGui
import org.joml.Math

@OnlyIn(Dist.CLIENT)
class Ac130GunnerHud {
    companion object {
        val ROLL_IND = loc("textures/overlay/vehicle/helicopter/roll_ind.png")
        val FRAME = loc("textures/overlay/vehicle/land/tv_frame.png")
        val LINE = loc("textures/overlay/vehicle/land/line.png")
        val COMPASS = loc("textures/overlay/vehicle/base/compass.png")
        val BLOCK = loc("textures/overlay/misc/block_white.png")
    }

    var lerpRecoil: Float = 0f

    // 测距缓存（每 tick 更新一次）
    private var cachedRange: Double = 0.0
    private var cachedBlockPos: BlockPos? = null
    private var cachedIsEntity: Boolean = false
    private var lastRangeTick: Int = -1
    
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
        val data = vehicle.getGunData(index) ?: return

        if (vehicle.hasWeapon(vehicle.getSeatIndex(player))) {
            if (ClientEventHandler.zoomVehicle || mc.options.cameraType == CameraType.FIRST_PERSON) {
                val color = data.get(GunProp.CROSSHAIR_COLOR).get()
                val recoil = Mth.lerp(partialTick, vehicle.recoilShakeO.toFloat(), vehicle.recoilShake.toFloat())
                lerpRecoil = Mth.lerp(0.1f * partialTick, lerpRecoil, recoil * (2 * (Math.random() - 0.5f)).toFloat())
                val pitch = Mth.lerp(partialTick, vehicle.fakePitchO, vehicle.fakePitch)

                poseStack.pushPose()

                poseStack.translate(
                    lerpRecoil * 6 + screenWidth * 0.025f * recoil,
                    recoil * 3 + screenHeight * 0.025f * recoil - pitch,
                    0f
                )
                poseStack.scale(1 - recoil * 0.05f, 1 - recoil * 0.05f, 1f)
                poseStack.rotateAround(
                    Axis.ZP.rotationDegrees(-0.3f * ClientEventHandler.cameraRoll + 2.5f * lerpRecoil),
                    screenWidth / 2f,
                    screenHeight / 2f,
                    0f
                )


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
                    128 - (64f / 45 * VehicleVecUtils.getYRotFromVector(Vec3(mc.gameRenderer.mainCamera.lookVector)).toFloat()),
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
                            vehicle.absoluteSpeed * 72,
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

                // 测距（每 tick 更新一次缓存）
                if (vehicle.tickCount != lastRangeTick) {
                    lastRangeTick = vehicle.tickCount

                    val result = player.level().clip(
                        ClipContext(
                            vehicle.getShootPosForHud(player, 1f),
                            vehicle.getShootPosForHud(player, 1f)
                                .add(vehicle.getShootDirectionForHud(player, 1f).scale(512.0)),
                            ClipContext.Block.VISUAL,
                            ClipContext.Fluid.NONE,
                            player
                        )
                    )
                    val hitPos = result.location
                    cachedBlockPos = BlockPos.containing(hitPos)
                    cachedRange = player.getEyePosition(1f).distanceTo(hitPos)
                    cachedIsEntity = false

                    val lookingEntity = TraceTool.cameraFindLookingEntity(
                        player,
                        vehicle.getShootPosForHud(player, 1f),
                        vehicle.getShootDirectionForHud(player, 1f),
                        512.0
                    )
                    if (lookingEntity != null) {
                        cachedIsEntity = true
                        cachedRange = player.distanceTo(lookingEntity).toDouble()
                        cachedBlockPos = lookingEntity.blockPosition()
                    }
                }

                val displayText = if (cachedIsEntity || cachedRange <= 500) {
                    val bp = cachedBlockPos
                    val coordStr = if (bp != null) " [${bp.x}, ${bp.y}, ${bp.z}]" else ""
                    "${format0D(cachedRange, " m")}$coordStr"
                } else {
                    "---m"
                }

                val width = Minecraft.getInstance().font.width(displayText)
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    Component.literal(displayText),
                    screenWidth / 2 - width / 2,
                    screenHeight - 53,
                    color,
                    false
                )
                
                poseStack.popPose()
            }
        }
    }
}
