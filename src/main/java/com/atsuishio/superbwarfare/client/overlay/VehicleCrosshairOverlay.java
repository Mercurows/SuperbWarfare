package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.ResourceOnceLogger;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Math;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.client.overlay.weapon.LandVehicleHud.lerpRecoil;

@OnlyIn(Dist.CLIENT)
public class VehicleCrosshairOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("vehicle_crosshair");

    private static final ResourceOnceLogger LOGGER = new ResourceOnceLogger();

    public static final Map<String, ResourceLocation> CROSSHAIR_MAP = Map.ofEntries(
            Map.entry("@VehicleUsApc", Mod.loc("textures/overlay/vehicle/crosshair/us_apc.png")),
            Map.entry("@VehicleUsTank", Mod.loc("textures/overlay/vehicle/crosshair/us_tank.png")),
            Map.entry("@VehicleRuApc", Mod.loc("textures/overlay/vehicle/crosshair/ru_apc.png")),
            Map.entry("@VehicleCommonMissile", Mod.loc("textures/overlay/vehicle/crosshair/common_missile.png")),
            Map.entry("@VehicleCommonSeekMissile", Mod.loc("textures/overlay/vehicle/crosshair/common_seek_missile.png")),
            Map.entry("@VehicleCommonGun", Mod.loc("textures/overlay/vehicle/crosshair/common_gun.png")),
            Map.entry("@VehicleCommonGunDynamic", Mod.loc("textures/overlay/vehicle/crosshair/common_gun.png")),
            Map.entry("@VehicleCommonCannon", Mod.loc("textures/overlay/vehicle/crosshair/common_cannon.png")),
            Map.entry("@VehicleCommonCross", Mod.loc("textures/overlay/vehicle/crosshair/common_cross.png")),
            Map.entry("@VehicleDynamicCross", Mod.loc("textures/overlay/vehicle/crosshair/common_dynamic_cross.png")),
            Map.entry("@VehicleFixedPoint", Mod.loc("textures/overlay/vehicle/crosshair/common_fixed_point.png")),
            Map.entry("@VehicleCnHpjZooming", Mod.loc("textures/overlay/vehicle/crosshair/cn_hpj_zooming.png")),
            Map.entry("@VehicleCommonCannonZooming", Mod.loc("textures/overlay/vehicle/crosshair/common_cannon_zooming.png")),
            Map.entry("@VehicleLaserCannon", Mod.loc("textures/overlay/vehicle/crosshair/laser_cannon.png")),
            Map.entry("@AirCraftCommon", Mod.loc("textures/overlay/vehicle/aircraft/common.png"))
    );

    private static final ResourceLocation CROSSHAIR_THIRD_CAMERA = Mod.loc("textures/overlay/vehicle/crosshair/third_camera.png");
    private static float scopeScale = 1;

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || player.isSpectator()) {
            resetScale();
            return;
        }

        var entity = player.getVehicle();
        if (!(entity instanceof VehicleEntity vehicle)) {
            resetScale();
            return;
        }

        int index = vehicle.getSeatIndex(player);
        var data = vehicle.getGunData(index);
        if (data == null) {
            resetScale();
            return;
        }

        PoseStack poseStack = guiGraphics.pose();

        String crosshairPath = data.compute().crosshair;

        if (crosshairPath.equals(CrossHairOverlay.CROSSHAIR_EMPTY)) {
            resetScale();
            return;
        }

        if (ClientEventHandler.zoomVehicle && !data.compute().crosshairZooming.equals(CrossHairOverlay.CROSSHAIR_EMPTY)) {
            crosshairPath = data.compute().crosshairZooming;
        }

        int color = data.compute().crosshairColor.get();

        poseStack.pushPose();

        float recoil = Mth.lerp(partialTick, (float) vehicle.recoilShakeO, (float) vehicle.getRecoilShake());
        poseStack.translate(lerpRecoil * 6 + screenWidth * 0.025f * recoil, recoil * 3 + screenHeight * 0.025f * recoil, 0);
        poseStack.scale(1 - recoil * 0.05f, 1 - recoil * 0.05f, 1);
        poseStack.rotateAround(Axis.ZP.rotationDegrees(-0.3f * ClientEventHandler.cameraRoll + 4 * lerpRecoil), screenWidth / 2f, screenHeight / 2f, 0);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        scopeScale = Mth.lerp(partialTick, scopeScale, 1F);
        float scale = scopeScale;

        Vec3 shootPos = vehicle.getShootPosForHud(player, partialTick);

        BlockHitResult result = player.level().clip(new ClipContext(shootPos, shootPos.add(vehicle.getShootDirectionForHud(player, partialTick).scale(512)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 hitPos = result.getLocation();

        double dis = shootPos.distanceTo(hitPos);

        Entity lookingEntity = vehicle.getPlayerLookAtEntityOnVehicle(player, 512, partialTick);

        if (lookingEntity != null) {
            dis = shootPos.distanceTo(lookingEntity.position());
        }

        Vec3 pos = shootPos.add(vehicle.getShootDirectionForHud(player, partialTick).scale(dis));
        Vec3 p = VectorUtil.worldToScreen(pos);

        // 渲染第一人称
        if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) {
            poseStack.pushPose();

            ResourceLocation texture;
            if (crosshairPath.startsWith("@")) {
                texture = CROSSHAIR_MAP.get(crosshairPath);
            } else {
                texture = ResourceLocation.tryParse(crosshairPath);
            }

            if (texture == null) {
                String finalCrosshairPath = crosshairPath;
                LOGGER.log(crosshairPath, logger -> logger.error("Failed to load crosshair texture for {}", finalCrosshairPath));
            } else {
                float minWH = (float) Math.min(screenWidth, screenHeight);
                float scaledMinWH = Mth.floor(minWH * scale);
                float centerW = (screenWidth - scaledMinWH) / 2;
                float centerH = (screenHeight - scaledMinWH) / 2;
                float x = (float) p.x;
                float y = (float) p.y;

                if (crosshairPath.equals("@VehicleDynamicCross") && VectorUtil.canSee(pos)) {
                    RenderHelper.preciseBlitWithColor(guiGraphics, texture, x - scaledMinWH / 2, y - scaledMinWH / 2, 0, 0, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
                    VehicleHudOverlay.renderKillIndicatorDynamic(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));
                    ResourceLocation fixedTexture = CROSSHAIR_MAP.get("@VehicleFixedPoint");
                    RenderHelper.preciseBlitWithColor(guiGraphics, fixedTexture, centerW, centerH, 0, 0, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
                } else if ((crosshairPath.equals("@AirCraftCommon") || crosshairPath.equals("@VehicleLaserCannon") || crosshairPath.equals("@VehicleCommonGunDynamic")) && VectorUtil.canSee(pos)) {
                    RenderHelper.preciseBlitWithColor(guiGraphics, texture, x - scaledMinWH / 2, y - scaledMinWH / 2, 0, 0, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
                    VehicleHudOverlay.renderKillIndicatorDynamic(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));
                } else if (crosshairPath.equals("@VehicleCnHpjZooming")) {
                    ResourceLocation dynamicTexture = CROSSHAIR_MAP.get("@VehicleDynamicCross");
                    RenderHelper.preciseBlitWithColor(guiGraphics, dynamicTexture, x - scaledMinWH / 2, y - scaledMinWH / 2, 0, 0, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
                    VehicleHudOverlay.renderKillIndicatorDynamic(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));
                } else if (crosshairPath.equals("@VehicleCommonCannonZooming")) {
                    float fovAdjust = 70F / Minecraft.getInstance().options.fov().get();
                    float f = (float) Math.min(screenWidth, screenHeight);
                    float f1 = Math.min((float) screenWidth / f, (float) screenHeight / f) * fovAdjust;
                    int i = Mth.floor(f * f1);
                    int j = Mth.floor(f * f1);
                    int k = (screenWidth - i) / 2;
                    int l = (screenHeight - j) / 2;
                    preciseBlit(guiGraphics, texture, k, l, 0, 0, i, j, i, j);
                    VehicleHudOverlay.renderKillIndicator(guiGraphics, screenWidth, screenHeight);
                } else if (crosshairPath.equals("@VehicleCommonSeekMissile") && data.compute().seekWeaponInfo != null && data.compute().seekWeaponInfo.onlyLockBlock) {
                    Vec3 vec3 = ClientEventHandler.seekingPosVehicle;
                    if (ClientEventHandler.seekingTimeVehicle > 0) {
                        vec3 = ClientEventHandler.lockingPosVehicle;
                    }
                    String string = "[ " + FormatTool.format0D(vec3.x) + ", " + FormatTool.format0D(vec3.y) + ", " + FormatTool.format0D(vec3.z) + " ]";
                    int width = Minecraft.getInstance().font.width(string);
                    RenderHelper.preciseBlitWithColor(guiGraphics, texture, centerW, centerH, 0, 0, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
                    guiGraphics.drawString(Minecraft.getInstance().font, string, (float) screenWidth / 2 - (float) width / 2, (float) screenHeight / 2 + 78, color, false);
                } else {
                    RenderHelper.preciseBlitWithColor(guiGraphics, texture, centerW, centerH, 0, 0, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
                    VehicleHudOverlay.renderKillIndicator(guiGraphics, screenWidth, screenHeight);
                }
            }

            poseStack.popPose();
            poseStack.pushPose();


            poseStack.popPose();
        } else if (Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK && !ClientEventHandler.zoomVehicle) {
            // 渲染第三人称
            if (VectorUtil.canSee(pos) && !((vehicle.getVehicleType() == VehicleType.AIRPLANE || vehicle.getVehicleType() == VehicleType.HELICOPTER) && player == vehicle.getFirstPassenger())) {
                float x = (float) p.x;
                float y = (float) p.y;

                preciseBlit(guiGraphics, CROSSHAIR_THIRD_CAMERA, x - 12, y - 12, 0, 0, 24, 24, 24, 24);
                VehicleHudOverlay.renderKillIndicatorDynamic(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));

                poseStack.pushPose();

                poseStack.translate(x, y, 0);
                poseStack.scale(0.75f, 0.75f, 1);

                VehicleMainWeaponHudOverlay.renderWeaponInfoThird(guiGraphics, vehicle, player, data, mc.font);

                if (player == vehicle.getFirstPassenger()) {
                    if (vehicle.hasDecoy()) {
                        if (vehicle.getDecoyState().equals("READY")) {
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("superbwarfare.smoke.ready").append(Component.literal(" [" + ModKeyMappings.RELEASE_DECOY.getKey().getDisplayName().getString() + "]")), 30, 1, -1, false);
                        } else {
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("superbwarfare.smoke.reloading"), 30, 1, 0xFF0000, false);
                        }
                    }
                }

                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }

    private static void resetScale() {
        scopeScale = 0.7f;
    }
}
