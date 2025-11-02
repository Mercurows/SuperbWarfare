package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.MathTool;
import com.atsuishio.superbwarfare.tools.ResourceOnceLogger;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Math;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;

@OnlyIn(Dist.CLIENT)
public class VehicleCrosshairOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("vehicle_crosshair");

    private static final ResourceOnceLogger LOGGER = new ResourceOnceLogger();

    public static final Map<String, ResourceLocation> CROSSHAIR_MAP = Map.ofEntries(
            Map.entry("@VehicleUsApc", Mod.loc("textures/overlay/vehicle/crosshair/us_apc.png")),
            Map.entry("@VehicleUsTank", Mod.loc("textures/overlay/vehicle/crosshair/us_tank.png")),
            Map.entry("@VehicleRuApc", Mod.loc("textures/overlay/vehicle/crosshair/ru_apc.png")),
            Map.entry("@VehicleCommonMissile", Mod.loc("textures/overlay/vehicle/crosshair/common_missile.png")),
            Map.entry("@VehicleCommonGun", Mod.loc("textures/overlay/vehicle/crosshair/common_gun.png")),
            Map.entry("@VehicleCommonCannon", Mod.loc("textures/overlay/vehicle/crosshair/common_cannon.png"))
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

        if (vehicle.getVehicleType() == VehicleType.AIRPLANE || vehicle.getVehicleType() == VehicleType.HELICOPTER)
            return;

        int index = vehicle.getSeatIndex(player);
        var data = vehicle.getGunData(index);
        if (data == null) {
            resetScale();
            return;
        }

        PoseStack poseStack = guiGraphics.pose();

        String crosshairPath = data.get(GunProp.CROSSHAIR);
        int color = data.get(GunProp.CROSSHAIR_COLOR).get();

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

        // 渲染第一人称
        if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) {
            poseStack.pushPose();

            if (crosshairPath.equals(CrossHairOverlay.CROSSHAIR_CUSTOM)) {
                // 载具自定义第一人称渲染
                vehicle.renderFirstPersonOverlay(guiGraphics, poseStack, mc.font, player, screenWidth, screenHeight, scale, color);
            } else {
                ResourceLocation texture;
                if (crosshairPath.startsWith("@")) {
                    texture = CROSSHAIR_MAP.get(crosshairPath);
                } else {
                    texture = ResourceLocation.tryParse(crosshairPath);
                }

                if (texture == null) {
                    LOGGER.log(crosshairPath, logger -> logger.error("Failed to load crosshair texture for {}", crosshairPath));
                } else {
                    float minWH = (float) Math.min(screenWidth, screenHeight);
                    float scaledMinWH = Mth.floor(minWH * scale);
                    float centerW = (screenWidth - scaledMinWH) / 2;
                    float centerH = (screenHeight - scaledMinWH) / 2;

                    RenderHelper.preciseBlitWithColor(guiGraphics, texture, centerW, centerH, 0, 0, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
                }
            }

            poseStack.popPose();

            poseStack.pushPose();

            renderWeaponInfoFirst(guiGraphics, vehicle, player, data, mc.font, screenWidth, screenHeight, color);

            poseStack.popPose();
        } else if (Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_BACK && !ClientEventHandler.zoomVehicle) {
            // 渲染第三人称
            Vec3 pos = vehicle.getShootPos(player, partialTick).add(vehicle.getViewVec(player, partialTick).scale(192));
            Vec3 p = VectorUtil.worldToScreen(pos);

            if (VectorUtil.canSee(pos)) {
                float x = (float) p.x;
                float y = (float) p.y;

                preciseBlit(guiGraphics, CROSSHAIR_THIRD_CAMERA, x - 12, y - 12, 0, 0, 24, 24, 24, 24);
                VehicleHudOverlay.renderKillIndicator3P(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));

                poseStack.pushPose();

                poseStack.translate(x, y, 0);
                poseStack.scale(0.75f, 0.75f, 1);

                // 载具自定义第三人称渲染
                vehicle.renderThirdPersonOverlay(guiGraphics, mc.font, player, screenWidth, screenHeight, scale);

                renderWeaponInfoThird(guiGraphics, vehicle, player, data, mc.font);

                double health = 1 - vehicle.getHealth() / vehicle.getMaxHealth();
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("HP " +
                        FormatTool.format0D(100 * vehicle.getHealth() / vehicle.getMaxHealth())), 30, 1, Mth.hsvToRgb(0F, (float) health, 1.0F), false);

                if (vehicle.hasDecoy()) {
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("SMOKE " + vehicle.getDecoyState()), 30, 11, -1, false);
                }

                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }

    private static void resetScale() {
        scopeScale = 0.7f;
    }

    // TODO 正确显示文本和备弹数量，正确判断是否应该显示武器名称
    private static void renderWeaponInfoFirst(GuiGraphics guiGraphics, VehicleEntity vehicle, Player player, GunData data, Font font, int screenWidth, int screenHeight, int color) {
        if (!(vehicle instanceof WeaponVehicleEntity weaponVehicle)) return;
        if (!vehicle.amphibiousVehicle()) return;

        int heat = weaponVehicle.getWeaponHeat(player);
        int ammoCount = weaponVehicle.getAmmoCount(player);
        var component = Component.translatable(data.get(GunProp.NAME), ammoCount == Integer.MAX_VALUE ? "∞" : ammoCount);

        guiGraphics.drawString(font, component, (screenWidth - font.width(component)) / 2, screenHeight - 65,
                MathTool.getGradientColor(color, 0xFF0000, heat, 2), false);
    }

    private static void renderWeaponInfoThird(GuiGraphics guiGraphics, VehicleEntity vehicle, Player player, GunData data, Font font) {
        if (!(vehicle instanceof WeaponVehicleEntity weaponVehicle)) return;

        float heat = weaponVehicle.getWeaponHeat(player) / 100F;

        int ammoCount = weaponVehicle.getAmmoCount(player);
        var component = Component.translatable(data.get(GunProp.NAME), ammoCount == Integer.MAX_VALUE ? "∞" : ammoCount);

        guiGraphics.drawString(font, component, 30, -9, Mth.hsvToRgb(0F, heat, 1.0F), false);
    }
}
