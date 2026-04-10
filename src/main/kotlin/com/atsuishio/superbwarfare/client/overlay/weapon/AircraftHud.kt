package com.atsuishio.superbwarfare.client.overlay.weapon

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay.renderKillIndicatorDynamic
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.event.ClientMouseHandler
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import com.atsuishio.superbwarfare.tools.MathTool.getGradientColor
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
object AircraftHud {
    const val ID: String = "@Aircraft"

    private var lerpVy = 1f
    private var lerpG = 1f
    private var diffY = 0f
    private var diffX = 0f

    var bombHitPosX: Double = 0.0
    var bombHitPosY: Double = 0.0

    private val BOMB_SCOPE = loc("textures/overlay/vehicle/aircraft/bomb_scope.png")
    private val BOMB_SCOPE_PITCH = loc("textures/overlay/vehicle/aircraft/bomb_scope_pitch.png")
    private val HUD_BASE_MISSILE = loc("textures/overlay/vehicle/aircraft/hud_base_missile.png")
    private val HUD_BASE = loc("textures/overlay/vehicle/aircraft/hud_base.png")
    val HUD_LINE = loc("textures/overlay/vehicle/aircraft/hud_line.png")
    val HUD_LINE_3P = loc("textures/overlay/vehicle/aircraft/hud_line_3p.png")
    val ROLL_HUD_3P = loc("textures/overlay/vehicle/aircraft/roll_hud_3p.png")
    private val HUD_IND = loc("textures/overlay/vehicle/aircraft/hud_ind.png")
    private val HUD_BOMB = loc("textures/overlay/vehicle/aircraft/bomb.png")
    private val HUD_BASE2 = loc("textures/overlay/vehicle/aircraft/hud_base2.png")
    private val COMPASS_IND = loc("textures/overlay/vehicle/aircraft/compass_ind.png")
    private val HELICOPTER_ROLL_IND = loc("textures/overlay/vehicle/helicopter/roll_ind.png")
    private val HELICOPTER_SPEED_FRAME = loc("textures/overlay/vehicle/helicopter/speed_frame.png")
    val POWER_RULER = loc("textures/overlay/vehicle/aircraft/power_ruler.png")

    private val COMPASS = loc("textures/overlay/vehicle/base/compass.png")
    private val BOMB_RING = loc("textures/overlay/crosshair/rex_circle.png")

    private var mouseX = 0f
    private var mouseY = 0f
    private var lerpPower = 0f

    private var dis = 512.0

    @SubscribeEvent
    fun onAircraftHudClientTick(event: ClientTickEvent.Post) {
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
        val speed = vehicle.absoluteSpeed * 72

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

            val power = vehicle.power
            lerpPower = Mth.lerp(0.5f * partialTick, lerpPower, power)

            RenderHelper.preciseBlitWithColor(
                guiGraphics,
                HelicopterHud.HELI_POWER,
                x - 105f,
                (y - 57f + 124f - Math.min(lerpPower, 1f) * 117.6f),
                0f,
                0f,
                4f,
                Math.min(lerpPower, 1f) * 117.6f,
                4f,
                Math.min(lerpPower, 1f) * 117.6f,
                color
            )

            if (lerpPower > 1) {
                RenderHelper.preciseBlitWithColor(
                    guiGraphics,
                    HelicopterHud.HELI_POWER,
                    x - 105f,
                    (y - 57f + 124f - (lerpPower - 1f) * 58.8f),
                    0f,
                    0f,
                    4f,
                    (lerpPower - 1f) * 58.8f,
                    4f,
                    (lerpPower - 1f) * 58.8f,
                    0xFF6B00
                )
            }

            RenderHelper.preciseBlitWithColor(
                guiGraphics,
                POWER_RULER,
                x - 135f,
                y - 57f,
                0f,
                0f,
                64f,
                128f,
                64f,
                128f,
                color
            )

            //一些文本

            poseStack.pushPose()
            poseStack.translate(x.toDouble(), y.toDouble(), 0.0)
            //时速
            guiGraphics.drawString(
                mc.font, Component.literal(format0D(speed)),
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
                Mth.lerp((0.25f * partialTick).toDouble(), lerpG.toDouble(), (400 * vehicle.getAcceleration()) / 9.8).toFloat()
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
                x - 144 + diffY,
                y - 128,
                0f,
                722.5f + 4.725f * pitch,
                288f,
                256f,
                288f,
                1701f,
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

        if (pos.canBeSeen()) {
            var x = pCross.x.toFloat()
            var y = pCross.y.toFloat()
            val xCross = x
            val yCross = y

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
                if (gunData.get(GunProp.CROSSHAIR) == "@AirBomb") {
                    bombHitPosX = Mth.lerp(0.25 * partialTick.toDouble(), bombHitPosX, xCross.toDouble())
                    bombHitPosY = Mth.lerp(0.25 * partialTick.toDouble(), bombHitPosY, yCross.toDouble())

                    RenderHelper.preciseBlit(
                        guiGraphics,
                        BOMB_RING,
                        bombHitPosX.toFloat() - 12f,
                        bombHitPosY.toFloat() - 12f,
                        0f,
                        0f,
                        24f,
                        24f,
                        24f,
                        24f
                    )
                    x = p.x.toFloat()
                    y = p.y.toFloat()
                }

                mouseX = Mth.lerp(0.1f * partialTick, mouseX, ClientMouseHandler.lerpSpeedX.toFloat())
                mouseY = Mth.lerp(0.1f * partialTick, mouseY, ClientMouseHandler.lerpSpeedY.toFloat())
                RenderHelper.preciseBlit(guiGraphics,
                    HelicopterHud.RING, x - 2 + mouseX, y - 2 + mouseY, 0f, 0f, 4f, 4f, 4f, 4f)

                val originPos = Vec3(x.toDouble(), y.toDouble(), 0.0)
                val ringPos = Vec3(x + mouseX.toDouble(), y + mouseY.toDouble(), 0.0)

                val distance = ringPos.distanceTo(originPos)
                var i = 0.0
                while (i < distance - 3) {
                    val toVec = ringPos.vectorTo(originPos).normalize()
                    val p0 = ringPos.add(toVec.scale(i))
                    RenderHelper.preciseBlitWithColor(
                        guiGraphics,
                        HelicopterHud.BLOCK,
                        (p0.x - 0.25).toFloat(),
                        (p0.y - 0.25).toFloat(),
                        0f,
                        0f,
                        0.5f,
                        0.5f,
                        0.5f,
                        0.5f,
                        -1
                    )
                    i += 3
                }

                val pitch = vehicle.getPitch(partialTick)
                RenderHelper.preciseBlitWithColor(
                    guiGraphics,
                    HUD_LINE_3P,
                    x - 96,
                    y - 48,
                    0f,
                    195 + 1.36f * pitch,
                    192f,
                    96f,
                    192f,
                    486f,
                    -1
                )

                RenderHelper.preciseBlitWithColor(
                    guiGraphics,
                    ROLL_HUD_3P,
                    x - 48,
                    y - 48,
                    0f,
                    0f,
                    96f,
                    96f,
                    96f,
                    96f,
                    -1
                )

                poseStack.pushPose()
                poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), x, y, 0f)
                RenderHelper.preciseBlit(guiGraphics,
                    HelicopterHud.CROSSHAIR_3P, x - 34, y - 8.5f, 0f, 0f, 68f, 17f, 68f, 17f)
                renderKillIndicatorDynamic(
                    guiGraphics,
                    x - 7.5f + (2 * (Math.random() - 0.5f)).toFloat(),
                    y - 7.5f + (2 * (Math.random() - 0.5f)).toFloat()
                )

                //
                poseStack.pushPose()
                poseStack.translate(x, y, 0f)
                poseStack.scale(0.75f, 0.75f, 1f)
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    Component.translatable(format0D(vehicle.getRoll(partialTick).toDouble()) + "°"),
                    -42,
                    -9,
                    -1,
                    false
                )
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    Component.translatable(format0D(vehicle.y) + "m"),
                    -42,
                    2,
                    -1,
                    false
                )

                poseStack.popPose()
                //

                poseStack.popPose()

                // 时速
                //
                poseStack.pushPose()
                poseStack.translate(x, y, 0f)
                poseStack.scale(0.75f, 0.75f, 1f)
                guiGraphics.drawString(
                    mc.font,
                    Component.literal(format0D(speed, "km/h")),
                    -60,
                    -52,
                    -1,
                    false
                )

                val component = Component.literal(format0D(lerpVy.toDouble()) + "m/s")
                val font = Minecraft.getInstance().font

                guiGraphics.drawString(font, component, 60 - font.width(component), -52, -1, false)
                poseStack.popPose()

                poseStack.pushPose()
                poseStack.translate(x, y + 50, 0f)
                poseStack.scale(0.75f, 0.75f, 1f)

                VehicleMainWeaponHudOverlay.renderWeaponInfoThirdAir(guiGraphics, vehicle, player, gunData, font)

                if (vehicle.hasDecoy()) {
                    if (vehicle.decoyReady) {
                        val componentReady = Component.translatable("tips.superbwarfare.flare.ready").append(
                            Component.literal(
                                " [" + ModKeyMappings.RELEASE_DECOY.key.displayName.string + "]"
                            )
                        )
                        val length = font.width(componentReady)

                        guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            componentReady,
                            -length / 2,
                            1,
                            -1,
                            false
                        )
                    } else {
                        val componentReloading = Component.translatable("tips.superbwarfare.flare.reloading")
                        val length = font.width(componentReloading)

                        guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            componentReloading,
                            -length / 2,
                            1,
                            0xFF0000,
                            false
                        )
                    }
                }

                poseStack.popPose()
            }
        }
        poseStack.popPose()
        poseStack.popPose()
    }
}
