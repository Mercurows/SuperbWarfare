package com.atsuishio.superbwarfare.client.overlay.weapon

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay.renderKillIndicatorDynamic
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.event.ClientMouseHandler
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.tools.canBeSeen
import com.atsuishio.superbwarfare.tools.localPlayer
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
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import org.joml.Math

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
object OldAircraftHud {
    const val ID: String = "@OldAircraft"

    private var lerpVy = 1f
    private var diffY = 0f
    private var diffX = 0f

    var bombHitPosX: Double = 0.0
    var bombHitPosY: Double = 0.0

    private val BOMB_SCOPE = loc("textures/overlay/vehicle/aircraft/bomb_scope.png")
    private val BOMB_SCOPE_PITCH = loc("textures/overlay/vehicle/aircraft/bomb_scope_pitch.png")
    private val HUD_BASE = loc("textures/overlay/vehicle/crosshair/old_aircraft_gun.png")
    private val CROSSHAIR_3P = loc("textures/overlay/vehicle/crosshair/third_camera.png")
    private val BOMB_RING = loc("textures/overlay/crosshair/rex_circle.png")

    private var mouseX = 0f
    private var mouseY = 0f

    private var dis = 512.0

    @SubscribeEvent
    fun onOldAircraftHudClientTick(event: ClientTickEvent.Post) {
        val player = localPlayer ?: return
        val vehicle = player.vehicle
        if (vehicle !is VehicleEntity) return
        if (vehicle.computed().hudType != ID) return

        val shootPos = vehicle.getShootPosForHud(player, 1f)

        val result = player.level().clip(
            ClipContext(
                shootPos, shootPos.add(vehicle.getShootDirectionForHud(player, 1f).scale(512.0)),
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player
            )
        )
        val hitPos = result.location

        dis = shootPos.distanceTo(hitPos)

        val lookingEntity = vehicle.getPlayerLookAtEntityOnVehicle(player, 512.0, 1f)

        if (lookingEntity != null) {
            dis = shootPos.distanceTo(lookingEntity.position())
        }
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
        val camera = mc.gameRenderer.mainCamera
        val cameraPos = camera.position
        val poseStack = guiGraphics.pose()
        val gunData = vehicle.getGunData(player) ?: return

        poseStack.pushPose()

        val bomb = gunData.get(GunProp.CROSSHAIR) == "@AirBomb"

        val color = vehicle.hudColor
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

        lerpVy = Mth.lerp((0.021f * partialTick).toDouble(), lerpVy.toDouble(), vehicle.deltaMovement.y() * 20)
            .toFloat()
        diffY = Mth.lerp(partialTick.toDouble(), diffY.toDouble(), ClientMouseHandler.lerpSpeedX).toFloat()
        diffX = Mth.lerp(partialTick.toDouble(), diffX.toDouble(), ClientMouseHandler.lerpSpeedY).toFloat()

        val shootPos = vehicle.getShootPosForHud(player, partialTick)

        val pos = cameraPos.add(vehicle.getViewVector(partialTick).scale(512.0))
        var posCross = shootPos.add(vehicle.getShootDirectionForHud(player, partialTick).scale(dis))

        if (bomb) {
            val bombHitPosO = ClientEventHandler.bombHitPosO
            val bombHitPos = ClientEventHandler.bombHitPos
            val bombHitPosX = Mth.lerp(partialTick.toDouble(), bombHitPosO.x, bombHitPos.x)
            val bombHitPosY = Mth.lerp(partialTick.toDouble(), bombHitPosO.y, bombHitPos.y)
            val bombHitPosZ = Mth.lerp(partialTick.toDouble(), bombHitPosO.z, bombHitPos.z)
            posCross = Vec3(bombHitPosX, bombHitPosY, bombHitPosZ)
        }

        val p = pos.worldToScreen()
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

        poseStack.pushPose()

        if ((mc.options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) && pos.canBeSeen()) {
            val x = p.x.toFloat()
            val y = p.y.toFloat()

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

            RenderHelper.preciseBlitWithColor(guiGraphics, HUD_BASE, x - 24, y - 18, 0f, 0f, 48f, 48f, 48f, 48f, color)

            renderKillIndicatorDynamic(
                guiGraphics,
                x - 7.5f + (2 * (Math.random() - 0.5f)).toFloat(),
                y - 1.5f + (2 * (Math.random() - 0.5f)).toFloat()
            )

        }

        poseStack.pushPose()

        if (posCross.canBeSeen()) {
            val x = pCross.x.toFloat()
            val y = pCross.y.toFloat()

            var xCross = x
            var yCross = y

            if (gunData.get(GunProp.CROSSHAIR) == "@AirBomb") {
                bombHitPosX = Mth.lerp(0.25 * partialTick.toDouble(), bombHitPosX, x.toDouble())
                bombHitPosY = Mth.lerp(0.25 * partialTick.toDouble(), bombHitPosY, y.toDouble())
                xCross = bombHitPosX.toFloat()
                yCross = bombHitPosY.toFloat()
            }

            if (mc.options.cameraType != CameraType.FIRST_PERSON && !ClientEventHandler.zoomVehicle) {
                var cross = CROSSHAIR_3P
                var size = 16f

                if (gunData.get(GunProp.CROSSHAIR) == "@AirBomb") {
                    cross = BOMB_RING
                    size = 24f
                } else {
                    mouseX = Mth.lerp(0.1f * partialTick, mouseX, ClientMouseHandler.lerpSpeedX.toFloat())
                    mouseY = Mth.lerp(0.1f * partialTick, mouseY, ClientMouseHandler.lerpSpeedY.toFloat())
                    RenderHelper.preciseBlit(
                        guiGraphics,
                        BOMB_RING,
                        xCross - 8 + mouseX,
                        yCross - 8 + mouseY,
                        0f,
                        0f,
                        16f,
                        16f,
                        16f,
                        16f
                    )
                }

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

                if (gunData.get(GunProp.CROSSHAIR) == "@AirBomb") {
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
                } else {
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
                }

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
