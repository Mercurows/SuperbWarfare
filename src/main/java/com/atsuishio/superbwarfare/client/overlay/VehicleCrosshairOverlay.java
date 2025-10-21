package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Math;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class VehicleCrosshairOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("vehicle_crosshair");

    public static final Map<String, ResourceLocation> CROSSHAIR_MAP = Map.ofEntries(
            Map.entry("@VehicleUsApc", Mod.loc("textures/overlay/vehicle/crosshair/us_apc.png")),
            Map.entry("@VehicleUsTank", Mod.loc("textures/overlay/vehicle/crosshair/us_tank.png")),
            Map.entry("@VehicleRuApc", Mod.loc("textures/overlay/vehicle/crosshair/ru_apc.png")),
            Map.entry("@VehicleCommonMissile", Mod.loc("textures/overlay/vehicle/crosshair/common_missile.png")),
            Map.entry("@VehicleCommonGun", Mod.loc("textures/overlay/vehicle/crosshair/common_gun.png"))
    );

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

        String crosshairPath = data.get(GunProp.CROSSHAIR);
        int color = data.get(GunProp.CROSSHAIR_COLOR);

        poseStack.pushPose();

        poseStack.translate(0, 0 - 0.3 * ClientEventHandler.shakeTime + 3 * ClientEventHandler.cameraRoll, 0);
        poseStack.rotateAround(Axis.ZP.rotationDegrees(-0.3f * ClientEventHandler.cameraRoll), screenWidth / 2f, screenHeight / 2f, 0);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        scopeScale = Mth.lerp(partialTick, scopeScale, 1F);
        float scale = scopeScale;

        if (crosshairPath.equals(CrossHairOverlay.CROSSHAIR_CUSTOM)) {
            // 载具自定义第一人称渲染
            if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) {
                vehicle.renderFirstPersonOverlay(guiGraphics, poseStack, mc.font, player, screenWidth, screenHeight, scale, color);
            }
        } else {
            ResourceLocation texture;
            if (crosshairPath.startsWith("@")) {
                texture = CROSSHAIR_MAP.get(crosshairPath);
            } else {
                texture = ResourceLocation.tryParse(crosshairPath);
            }

            float minWH = (float) Math.min(screenWidth, screenHeight);
            float scaledMinWH = Mth.floor(minWH * scale);
            float centerW = ((screenWidth - scaledMinWH) / 2);
            float centerH = ((screenHeight - scaledMinWH) / 2);

            RenderHelper.preciseBlitWithColor(guiGraphics, texture, centerW, centerH, 0, 0, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
        }

        poseStack.popPose();
    }

    private static void resetScale() {
        scopeScale = 0.7f;
    }
}
