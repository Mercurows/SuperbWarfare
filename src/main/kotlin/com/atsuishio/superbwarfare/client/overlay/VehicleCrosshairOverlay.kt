package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay.renderKillIndicator
import com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay.renderKillIndicatorDynamic
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay.renderWeaponInfoThird
import com.atsuishio.superbwarfare.client.overlay.weapon.LandVehicleHud
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.tools.*
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.math.Axis
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Math

@OnlyIn(Dist.CLIENT)
object VehicleCrosshairOverlay : CommonOverlay("vehicle_crosshair") {

    private val LOGGER = ResourceOnceLogger()

    val CROSSHAIR_MAP = mapOf(
        "@VehicleUsApc" to loc("textures/overlay/vehicle/crosshair/us_apc.png"),
        "@VehicleUsTank" to loc("textures/overlay/vehicle/crosshair/us_tank.png"),
        "@VehicleRuApc" to loc("textures/overlay/vehicle/crosshair/ru_apc.png"),
        "@VehicleCnTank" to loc("textures/overlay/vehicle/crosshair/cn_tank.png"),
        "@VehicleCommonMissile" to loc("textures/overlay/vehicle/crosshair/common_missile.png"),
        "@VehicleCommonSeekMissile" to loc("textures/overlay/vehicle/crosshair/common_seek_missile.png"),
        "@VehicleCommonGun" to loc("textures/overlay/vehicle/crosshair/common_gun.png"),
        "@VehicleCommonGunDynamic" to loc("textures/overlay/vehicle/crosshair/common_gun.png"),
        "@VehicleCommonCannon" to loc("textures/overlay/vehicle/crosshair/common_cannon.png"),
        "@VehicleCommonCross" to loc("textures/overlay/vehicle/crosshair/common_cross.png"),
        "@VehicleDynamicCross" to loc("textures/overlay/vehicle/crosshair/common_dynamic_cross.png"),
        "@VehicleFixedPoint" to loc("textures/overlay/vehicle/crosshair/common_fixed_point.png"),
        "@VehicleCnHpjZooming" to loc("textures/overlay/vehicle/crosshair/cn_hpj_zooming.png"),
        "@VehicleCommonCannonZooming" to loc("textures/overlay/vehicle/crosshair/common_cannon_zooming.png"),
        "@VehicleLaserCannon" to loc("textures/overlay/vehicle/crosshair/laser_cannon.png"),
        "@AirCraftCommon" to loc("textures/overlay/vehicle/aircraft/common.png"),
        "@AirCraftNacelle" to loc("textures/overlay/vehicle/crosshair/nacelle.png"),
        "@NoCross" to loc("textures/overlay/vehicle/crosshair/empty.png")

    )

    private val CROSSHAIR_THIRD_CAMERA = loc("textures/overlay/vehicle/crosshair/third_camera.png")
    private var scopeScale = 1f

    override fun shouldRender(): Boolean {
        val shouldRender = super.shouldRender()
        if (!shouldRender) {
            resetScale()
        }
        return shouldRender
    }

    override fun RenderContext.render() {
        val entity = player.vehicle
        if (entity !is VehicleEntity) {
            resetScale()
            return
        }

        val index = entity.getSeatIndex(player)
        val data = entity.getGunData(index)
        if (data == null) {
            resetScale()
            return
        }

        val poseStack = guiGraphics.pose()

        var crosshairPath = data.get(GunProp.CROSSHAIR)

        if (crosshairPath == CrossHairOverlay.CROSSHAIR_EMPTY) {
            resetScale()
            return
        }

        if (ClientEventHandler.zoomVehicle && data.get(GunProp.CROSSHAIR_ZOOMING) != CrossHairOverlay.CROSSHAIR_EMPTY) {
            crosshairPath = data.get(GunProp.CROSSHAIR_ZOOMING)
        }

        val color = data.get(GunProp.CROSSHAIR_COLOR).get()

        poseStack.pushPose()

        val recoil = Mth.lerp(partialTick, entity.recoilShakeO.toFloat(), entity.recoilShake.toFloat())
        poseStack.translate(
            LandVehicleHud.lerpRecoil * 6 + screenWidth * 0.025f * recoil,
            recoil * 3 + screenHeight * 0.025f * recoil,
            0f
        )
        poseStack.scale(1 - recoil * 0.05f, 1 - recoil * 0.05f, 1f)
        poseStack.rotateAround(
            Axis.ZP.rotationDegrees(-0.3f * ClientEventHandler.cameraRoll + 4 * LandVehicleHud.lerpRecoil),
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

        scopeScale = Mth.lerp(partialTick, scopeScale, 1f)
        val scale: Float = scopeScale

        var shootPos = entity.getShootPosForHud(player, 1f)
        var shootVec = entity.getShootDirectionForHud(player, 1f).scale(512.0)
        val nacelleCam = ClientEventHandler.isNacelleCam(player)

        if (nacelleCam) {
            shootPos = camera.position
            shootVec = Vec3(camera.lookVector).scale(512.0)
        }

        val result = player.level().clip(
            ClipContext(
                shootPos, shootPos.add(shootVec),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player
            )
        )

        val hitPos = result.location

        var dis = shootPos.distanceTo(hitPos)

        var lookingEntity = entity.getPlayerLookAtEntityOnVehicle(player, 512.0, 1f)
        if (nacelleCam) {
            lookingEntity = TraceTool.camerafFindLookingEntity(player, shootPos, shootVec, 512.0)
        }

        if (lookingEntity != null) {
            dis = shootPos.distanceTo(lookingEntity.position())
        }

        val pos = shootPos.add(entity.getShootDirectionForHud(player, partialTick).scale(dis))
        val p = pos.worldToScreen()

        // 渲染第一人称
        if (Minecraft.getInstance().options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) {
            poseStack.pushPose()

            val texture: ResourceLocation?
            if (crosshairPath.startsWith("@")) {
                texture = CROSSHAIR_MAP.get(crosshairPath)
            } else {
                texture = ResourceLocation.tryParse(crosshairPath)
            }

            if (texture == null) {
                val finalCrosshairPath = crosshairPath
                if (finalCrosshairPath != "@Custom") {
                    LOGGER.log(
                        crosshairPath
                    ) { logger ->
                        logger!!.error(
                            "Failed to load crosshair texture for {}",
                            finalCrosshairPath
                        )
                    }
                }
            } else {
                val minWH = Math.min(screenWidth, screenHeight).toFloat()
                val scaledMinWH = Mth.floor(minWH * scale).toFloat()
                val centerW = (screenWidth - scaledMinWH) / 2
                val centerH = (screenHeight - scaledMinWH) / 2
                val x = p.x.toFloat()
                val y = p.y.toFloat()

                if (crosshairPath == "@VehicleDynamicCross" && pos.canBeSeen()) {
                    RenderHelper.blit(
                        poseStack,
                        texture,
                        x - scaledMinWH / 2,
                        y - scaledMinWH / 2,
                        0f,
                        0f,
                        scaledMinWH,
                        scaledMinWH,
                        scaledMinWH,
                        scaledMinWH,
                        color
                    )
                    renderKillIndicatorDynamic(
                        guiGraphics,
                        x - 7.5f + (2 * (Math.random() - 0.5f)).toFloat(),
                        y - 7.5f + (2 * (Math.random() - 0.5f)).toFloat()
                    )
                    val fixedTexture: ResourceLocation? = CROSSHAIR_MAP["@VehicleFixedPoint"]
                    RenderHelper.blit(
                        poseStack,
                        fixedTexture,
                        centerW,
                        centerH,
                        0f,
                        0f,
                        scaledMinWH,
                        scaledMinWH,
                        scaledMinWH,
                        scaledMinWH,
                        color
                    )
                } else if ((crosshairPath == "@AirCraftCommon" || crosshairPath == "@VehicleLaserCannon" || crosshairPath == "@VehicleCommonGunDynamic") && pos.canBeSeen()) {
                    RenderHelper.blit(
                        poseStack,
                        texture,
                        x - scaledMinWH / 2,
                        y - scaledMinWH / 2,
                        0f,
                        0f,
                        scaledMinWH,
                        scaledMinWH,
                        scaledMinWH,
                        scaledMinWH,
                        color
                    )
                    renderKillIndicatorDynamic(
                        guiGraphics,
                        x - 7.5f + (2 * (Math.random() - 0.5f)).toFloat(),
                        y - 7.5f + (2 * (Math.random() - 0.5f)).toFloat()
                    )
                } else if (crosshairPath == "@AirCraftNacelle") {
                    RenderHelper.blit(
                        poseStack,
                        texture,
                        centerW,
                        centerH,
                        0f,
                        0f,
                        scaledMinWH,
                        scaledMinWH,
                        scaledMinWH,
                        scaledMinWH,
                        color
                    )
                    renderKillIndicator(guiGraphics, screenWidth.toFloat(), screenHeight.toFloat())

                    val width = Minecraft.getInstance().font.width(FormatTool.format0D(dis, " m"))
                    guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        Component.literal(FormatTool.format0D(dis, " m")),
                        screenWidth / 2 - width / 2,
                        screenHeight / 2 + 30,
                        color,
                        false
                    )

                    val heat = entity.getWeaponHeat(player)
                    val component = entity.firstPersonAmmoComponent(data, player)

                    guiGraphics.drawString(
                        font, component, (screenWidth) / 2 - 50 - font.width(component), screenHeight / 2 + 2,
                        MathTool.getGradientColor(color, 0xFF0000, heat, 2), false
                    )

                } else if (crosshairPath == "@VehicleCnHpjZooming") {
                    val dynamicTexture: ResourceLocation? = CROSSHAIR_MAP.get("@VehicleDynamicCross")
                    RenderHelper.blit(
                        poseStack,
                        dynamicTexture,
                        x - scaledMinWH / 2,
                        y - scaledMinWH / 2,
                        0f,
                        0f,
                        scaledMinWH,
                        scaledMinWH,
                        scaledMinWH,
                        scaledMinWH,
                        color
                    )
                    renderKillIndicatorDynamic(
                        guiGraphics,
                        x - 7.5f + (2 * (Math.random() - 0.5f)).toFloat(),
                        y - 7.5f + (2 * (Math.random() - 0.5f)).toFloat()
                    )
                } else if (crosshairPath == "@VehicleCommonCannonZooming") {
                    val fovAdjust = 60f / Minecraft.getInstance().options.fov().get()
                    val f = Math.min(screenWidth, screenHeight).toFloat()
                    val f1 = Math.min(screenWidth.toFloat() / f, screenHeight.toFloat() / f) * fovAdjust
                    val i = Mth.floor(f * f1)
                    val j = Mth.floor(f * f1)
                    val k = (screenWidth - i) / 2
                    val l = (screenHeight - j) / 2
                    RenderHelper.preciseBlit(
                        guiGraphics,
                        texture,
                        k.toFloat(),
                        l.toFloat(),
                        0f,
                        0f,
                        i.toFloat(),
                        j.toFloat(),
                        i.toFloat(),
                        j.toFloat()
                    )
                    renderKillIndicator(guiGraphics, screenWidth.toFloat(), screenHeight.toFloat())
                } else if (crosshairPath == "@VehicleCommonSeekMissile" && data.get(GunProp.SEEK_WEAPON_INFO) != null && data.get(
                        GunProp.SEEK_WEAPON_INFO
                    )?.onlyLockBlock ?: false
                ) {
                    var vec3 = ClientEventHandler.seekingPosVehicle
                    if (ClientEventHandler.seekingTimeVehicle > 0) {
                        vec3 = ClientEventHandler.lockingPosVehicle
                    }
                    if (vec3 != null) {
                        val string = vec3.toFormattedString()
                        val width = Minecraft.getInstance().font.width(string)
                        RenderHelper.blit(
                            poseStack,
                            texture,
                            centerW,
                            centerH,
                            0f,
                            0f,
                            scaledMinWH,
                            scaledMinWH,
                            scaledMinWH,
                            scaledMinWH,
                            color
                        )
                        guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            string,
                            screenWidth.toFloat() / 2 - width.toFloat() / 2,
                            screenHeight.toFloat() - 73,
                            color,
                            false
                        )
                    }
                } else {
                    RenderHelper.blit(
                        poseStack,
                        texture,
                        centerW,
                        centerH,
                        0f,
                        0f,
                        scaledMinWH,
                        scaledMinWH,
                        scaledMinWH,
                        scaledMinWH,
                        color
                    )
                    renderKillIndicator(guiGraphics, screenWidth.toFloat(), screenHeight.toFloat())
                }
            }

            poseStack.popPose()
        } else if (Minecraft.getInstance().options.cameraType == CameraType.THIRD_PERSON_BACK && !ClientEventHandler.zoomVehicle) {
            // 渲染第三人称
            if (pos.canBeSeen() && !((entity.vehicleType == VehicleType.AIRPLANE || entity.vehicleType == VehicleType.HELICOPTER) && player === entity.getFirstPassenger())) {
                val x = p.x.toFloat()
                val y = p.y.toFloat()

                RenderHelper.preciseBlit(
                    guiGraphics,
                    CROSSHAIR_THIRD_CAMERA,
                    x - 12,
                    y - 12,
                    0f,
                    0f,
                    24f,
                    24f,
                    24f,
                    24f
                )
                renderKillIndicatorDynamic(
                    guiGraphics,
                    x - 7.5f + (2 * (Math.random() - 0.5f)).toFloat(),
                    y - 7.5f + (2 * (Math.random() - 0.5f)).toFloat()
                )

                poseStack.pushPose()

                poseStack.translate(x, y, 0f)
                poseStack.scale(0.75f, 0.75f, 1f)

                renderWeaponInfoThird(guiGraphics, entity, player, data, mc.font)

                if (player === entity.getFirstPassenger()) {
                    if (entity.hasDecoy()) {
                        if (entity.decoyReady) {
                            guiGraphics.drawString(
                                Minecraft.getInstance().font,
                                Component.translatable("tips.superbwarfare.smoke.ready").append(
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
                                Component.translatable("tips.superbwarfare.smoke.reloading"),
                                30,
                                1,
                                0xFF0000,
                                false
                            )
                        }
                    }
                }

                poseStack.popPose()
            }
        }

        poseStack.popPose()
    }


    private fun resetScale() {
        scopeScale = 0.7f
    }
}
