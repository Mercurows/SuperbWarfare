package com.atsuishio.superbwarfare.client.overlay.weapon

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay.renderKillIndicatorDynamic
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.event.ClientMouseHandler
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.FormatTool
import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import com.atsuishio.superbwarfare.tools.MathTool.getGradientColor
import com.atsuishio.superbwarfare.tools.VectorUtil
import com.atsuishio.superbwarfare.tools.mc
import com.atsuishio.superbwarfare.tools.plus
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
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.joml.Math

// TODO 预制通用固定翼飞机HUD，提取准星
@OnlyIn(Dist.CLIENT)
object AircraftHud {
    const val ID: String = "@Aircraft"

    private var lerpVy = 1f
    private var lerpG = 1f
    private var diffY = 0f
    private var diffX = 0f

    var bombHitPosX: Double = 0.0
    var bombHitPosY: Double = 0.0
    var bombHitPosZ: Double = 0.0

    private val BOMB_SCOPE = loc("textures/overlay/vehicle/aircraft/bomb_scope.png")
    private val BOMB_SCOPE_PITCH = loc("textures/overlay/vehicle/aircraft/bomb_scope_pitch.png")
    private val HUD_BASE_MISSILE = loc("textures/overlay/vehicle/aircraft/hud_base_missile.png")
    private val HUD_BASE = loc("textures/overlay/vehicle/aircraft/hud_base.png")
    private val HUD_LINE = loc("textures/overlay/vehicle/aircraft/hud_line.png")
    private val HUD_IND = loc("textures/overlay/vehicle/aircraft/hud_ind.png")
    private val HUD_BOMB = loc("textures/overlay/vehicle/aircraft/bomb.png")
    private val HUD_BASE2 = loc("textures/overlay/vehicle/aircraft/hud_base2.png")
    private val COMPASS_IND = loc("textures/overlay/vehicle/aircraft/compass_ind.png")
    private val HELICOPTER_ROLL_IND = loc("textures/overlay/vehicle/helicopter/roll_ind.png")
    private val HELICOPTER_SPEED_FRAME = loc("textures/overlay/vehicle/helicopter/speed_frame.png")

    private val COMPASS = loc("textures/overlay/vehicle/base/compass.png")
    private val CROSSHAIR_3P = loc("textures/overlay/vehicle/crosshair/third_camera.png")
    private val BOMB_RING = loc("textures/overlay/crosshair/rex_circle.png")

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

        val pos = cameraPos.add(vehicle.getViewVector(partialTick).scale(512.0))
        var posCross = shootPos.add(vehicle.getShootDirectionForHud(player, partialTick).scale(dis))

        if (bomb) {
            bombHitPosX = Mth.lerp(partialTick.toDouble(), bombHitPosX, vehicle.bombHitPos(player).x)
            bombHitPosY = Mth.lerp(partialTick.toDouble(), bombHitPosY, vehicle.bombHitPos(player).y)
            bombHitPosZ = Mth.lerp(partialTick.toDouble(), bombHitPosZ, vehicle.bombHitPos(player).z)
            posCross = Vec3(bombHitPosX, bombHitPosY, bombHitPosZ)
        }

        val p = VectorUtil.worldToScreen(pos)
        val pCross = VectorUtil.worldToScreen(posCross)

        // 投弹准星
        if (bomb && ClientEventHandler.zoomVehicle) {
            if (VectorUtil.canSee(posCross)) {
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

        if ((mc.options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) && VectorUtil.canSee(
                pos
            )
        ) {
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

            if (gunData.get(GunProp.CROSSHAIR) == "@AirCraftMissile") {
                RenderHelper.preciseBlitWithColor(
                    guiGraphics,
                    HUD_BASE_MISSILE,
                    x - 160,
                    y - 160,
                    0f,
                    0f,
                    320f,
                    320f,
                    320f,
                    320f,
                    color
                )
            } else {
                RenderHelper.preciseBlitWithColor(
                    guiGraphics,
                    HUD_BASE,
                    x - 160,
                    y - 160,
                    0f,
                    0f,
                    320f,
                    320f,
                    320f,
                    320f,
                    color
                )
            }

            //指南针
            RenderHelper.preciseBlitWithColor(
                guiGraphics,
                COMPASS,
                x - 128,
                y - 122,
                128 + (64f / 45 * vehicle.getYaw(partialTick)),
                0f,
                256f,
                16f,
                512f,
                16f,
                color
            )
            RenderHelper.preciseBlitWithColor(guiGraphics, COMPASS_IND, x - 4, y - 130, 0f, 0f, 8f, 8f, 8f, 8f, color)

            //滚转指示
            poseStack.pushPose()
            poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), x, y + 48, 0f)
            RenderHelper.preciseBlitWithColor(
                guiGraphics,
                HELICOPTER_ROLL_IND,
                x - 4,
                y + 144,
                0f,
                0f,
                8f,
                8f,
                8f,
                8f,
                color
            )
            poseStack.popPose()

            //一些文本

            poseStack.pushPose()
            poseStack.translate(x.toDouble(), y.toDouble(), 0.0)
            //时速
            guiGraphics.drawString(
                    mc.font, Component.literal(format0D(vehicle.deltaMovement.length() * 72)),
                    -105, -61, color, false
            )

            //高度
            guiGraphics.drawString(
                    mc.font, Component.literal(format0D(vehicle.y)),
                    75, -61, color, false
            )

            //垂直速度
            guiGraphics.drawString(
                    mc.font,
                    Component.literal(FormatTool.DECIMAL_FORMAT_1ZZ.format(lerpVy.toDouble())),
                    -96,
                    60,
                    color,
                    false
            )
            //加速度
            lerpG =
                    Mth.lerp((0.1f * partialTick).toDouble(), lerpG.toDouble(), vehicle.getAcceleration() / 9.8).toFloat()
            guiGraphics.drawString(mc.font, Component.literal("M"), -105, 70, color, false)
            guiGraphics.drawString(mc.font, Component.literal("0.2"), -96, 70, color, false)
            guiGraphics.drawString(mc.font, Component.literal("G"), -105, 78, color, false)
            guiGraphics.drawString(
                    mc.font,
                    Component.literal(FormatTool.DECIMAL_FORMAT_1ZZ.format(lerpG.toDouble())),
                    -96,
                    78,
                    color,
                    false
            )

            // 热诱弹
            if (vehicle.hasDecoy()) {
                if (vehicle.decoyReady) {
                    guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            Component.translatable("tips.superbwarfare.flare.ready").append(
                                    Component.literal(
                                            " [" + ModKeyMappings.RELEASE_DECOY.key.displayName.string + "]"
                                    )
                            ),
                            72,
                            0,
                            color,
                            false
                    )
                } else {
                    guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            Component.translatable("tips.superbwarfare.flare.reloading"),
                            72,
                            0,
                            0xFF0000,
                            false
                    )
                }
            }
            guiGraphics.drawString(mc.font, Component.literal("TGT"), 76, 78, color, false)

            // 武器名
            val heat = vehicle.getWeaponHeat(player)
            val component = vehicle.firstPersonAmmoComponent(gunData, player)

            guiGraphics.drawString(
                    mc.font, component, -mc.font.width(component) / 2, 91,
                    getGradientColor(color, 0xFF0000, heat, 2), false
            )

            // 能量警告
            if (vehicle.hasEnergyStorage()) {
                if (vehicle.energy < 0.02 * vehicle.maxEnergy) {
                    guiGraphics.drawString(
                            mc.font, Component.literal("NO POWER!"),
                            -144, 14, -65536, false
                    )
                } else if (vehicle.energy < 0.2 * vehicle.maxEnergy) {
                    guiGraphics.drawString(
                            mc.font, Component.literal("LOW POWER"),
                            -144, 14, 0xFF6B00, false
                    )
                }
            }

            poseStack.popPose()

            //框
            RenderHelper.preciseBlitWithColor(
                guiGraphics,
                HELICOPTER_SPEED_FRAME,
                x - 108,
                y - 64,
                0f,
                0f,
                36f,
                12f,
                36f,
                12f,
                color
            )
            RenderHelper.preciseBlitWithColor(
                guiGraphics,
                HELICOPTER_SPEED_FRAME,
                x + 108 - 36,
                y - 64,
                0f,
                0f,
                36f,
                12f,
                36f,
                12f,
                color
            )

            //角度
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

            poseStack.rotateAround(Axis.ZP.rotationDegrees(-vehicle.getRoll(partialTick)), x, y, 0f)
            val pitch = vehicle.getPitch(partialTick)
            RenderHelper.preciseBlitWithColor(
                guiGraphics,
                HUD_LINE,
                x - 96 + diffY,
                y - 128,
                0f,
                448 + 4.10625f * pitch,
                192f,
                256f,
                192f,
                1152f,
                color
            )
            RenderHelper.preciseBlitWithColor(
                guiGraphics,
                HUD_IND,
                x - 18 + diffY,
                y - 12,
                0f,
                0f,
                36f,
                24f,
                36f,
                24f,
                color
            )
            RenderHelper.preciseBlitWithColor(
                guiGraphics,
                HUD_LINE,
                x - 96 + diffY,
                y - 128,
                0f,
                448 + 4.10625f * pitch,
                192f,
                256f,
                192f,
                1152f,
                color
            )

            if (bomb) {
                RenderHelper.preciseBlitWithColor(
                    guiGraphics,
                    HUD_BOMB,
                    x - 64 + diffY,
                    y - 64,
                    0f,
                    0f,
                    128f,
                    128f,
                    128f,
                    128f,
                    color
                )
            } else {
                RenderHelper.preciseBlitWithColor(
                    guiGraphics,
                    HUD_IND,
                    x - 18 + diffY,
                    y - 12,
                    0f,
                    0f,
                    36f,
                    24f,
                    36f,
                    24f,
                    color
                )
            }

            poseStack.popPose()
        }

        poseStack.pushPose()

        if (VectorUtil.canSee(posCross)) {
            val x = pCross.x.toFloat()
            val y = pCross.y.toFloat()

            if ((mc.options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) && (gunData.get(
                    GunProp.CROSSHAIR
                ) != "@AirBomb") && (gunData.get(GunProp.CROSSHAIR) != "@AirCraftMissile")
            ) {
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
                RenderHelper.preciseBlitWithColor(
                    guiGraphics,
                    HUD_BASE2,
                    x - 72 + diffY,
                    y - 72 + diffX,
                    0f,
                    0f,
                    144f,
                    144f,
                    144f,
                    144f,
                    color
                )
            } else if (mc.options.cameraType != CameraType.FIRST_PERSON && !ClientEventHandler.zoomVehicle) {
                poseStack.pushPose()
                poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), x, y, 0f)
                poseStack.pushPose()
                poseStack.translate(x, y, 0f)
                poseStack.scale(0.75f, 0.75f, 1f)

                var cross = CROSSHAIR_3P
                var size = 16f

                if (gunData.get(GunProp.CROSSHAIR) == "@AirBomb") {
                    cross = BOMB_RING
                    size = 24f
                }

                val heat = vehicle.getWeaponHeat(player) / 100f
                val component = vehicle.thirdPersonAmmoComponent(gunData, player)

                guiGraphics.drawString(mc.font, component, 25, -9, Mth.hsvToRgb(0f, heat, 1f), false)
                if (vehicle.hasDecoy()) {
                    if (vehicle.decoyReady) {
                        guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            Component.translatable("tips.superbwarfare.flare.ready") + Component.literal(
                                " [" + ModKeyMappings.RELEASE_DECOY.key.displayName.string + "]"
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
                    x - 0.5f * size,
                    y - 0.5f * size,
                    0f,
                    0f,
                    size,
                    size,
                    size,
                    size
                )
                renderKillIndicatorDynamic(
                    guiGraphics,
                    x - 7.5f + (2 * (Math.random() - 0.5f)).toFloat(),
                    y - 7.5f + (2 * (Math.random() - 0.5f)).toFloat()
                )
                poseStack.popPose()
            }
        }

        val speed = vehicle.deltaMovement.length() * 72
        val height = vehicle.position().distanceTo(
            (Vec3.atLowerCornerOf(
                vehicle.level().clip(
                    ClipContext(
                        vehicle.position(), vehicle.position().add(Vec3(0.0, -1.0, 0.0).scale(160.0)),
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
                        vehicle.position().add(vehicle.deltaMovement.add(0.0, 0.06, 0.0).normalize().scale(160.0)),
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.ANY,
                        vehicle
                    )
                ).blockPos
            ))
        )

        if (lerpVy < -42) {
            guiGraphics.drawString(
                Minecraft.getInstance().font, Component.literal("SINK RATE, PULL UP!"),
                screenWidth / 2 - 53, screenHeight / 2 + 24, -65536, false
            )
            if (player.tickCount % 30 == 0) {
                player.level()
                    .playLocalSound(player.onPos, ModSounds.PULL_UP.get(), SoundSource.PLAYERS, 3f, 1f, false)
            }
        } else if (((lerpVy < -10 || (lerpVy < -3 && speed > 170)) && height < 30) || (speed > 100 && blockInWay < 144)) {
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
        poseStack.popPose()
    }
}
