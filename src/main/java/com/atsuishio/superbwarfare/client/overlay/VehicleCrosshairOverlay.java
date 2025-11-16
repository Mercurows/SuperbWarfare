package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.ResourceOnceLogger;
import com.atsuishio.superbwarfare.tools.TraceTool;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.joml.Math;

import java.util.Map;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.client.overlay.weapon.LandVehicleHud.lerpRecoil;

@OnlyIn(Dist.CLIENT)
public class VehicleCrosshairOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_vehicle_crosshair";

    private static final ResourceOnceLogger LOGGER = new ResourceOnceLogger();

    public static final Map<String, ResourceLocation> CROSSHAIR_MAP = Map.ofEntries(
            Map.entry("@VehicleUsApc", Mod.loc("textures/overlay/vehicle/crosshair/us_apc.png")),
            Map.entry("@VehicleUsTank", Mod.loc("textures/overlay/vehicle/crosshair/us_tank.png")),
            Map.entry("@VehicleRuApc", Mod.loc("textures/overlay/vehicle/crosshair/ru_apc.png")),
            Map.entry("@VehicleCommonMissile", Mod.loc("textures/overlay/vehicle/crosshair/common_missile.png")),
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
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = gui.getMinecraft();
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

        Vec3 entityPos = TraceTool.vehicleFindLookingPos(player, vehicle, shootPos, 512, partialTick);

        if (entityPos != null) {
            dis = shootPos.distanceTo(entityPos);
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
                    RenderHelper.blit(poseStack, texture, x - scaledMinWH / 2, y - scaledMinWH / 2, 0, 0, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
                    VehicleHudOverlay.renderKillIndicatorDynamic(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));
                    ResourceLocation fixedTexture = CROSSHAIR_MAP.get("@VehicleFixedPoint");
                    RenderHelper.blit(poseStack, fixedTexture, centerW, centerH, 0, 0, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
                } else if ((crosshairPath.equals("@AirCraftCommon") || crosshairPath.equals("@VehicleLaserCannon") || crosshairPath.equals("@VehicleCommonGunDynamic")) && VectorUtil.canSee(pos)) {
                    RenderHelper.blit(poseStack, texture, x - scaledMinWH / 2, y - scaledMinWH / 2, 0, 0, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
                    VehicleHudOverlay.renderKillIndicatorDynamic(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));
                } else {
                    if (crosshairPath.equals("@VehicleCnHpjZooming")) {
                        ResourceLocation fixedTexture = CROSSHAIR_MAP.get("@VehicleFixedPoint");
                        RenderHelper.blit(poseStack, fixedTexture, centerW, centerH, 0, 0, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
                        ResourceLocation dynamicTexture = CROSSHAIR_MAP.get("@VehicleDynamicCross");
                        RenderHelper.blit(poseStack, dynamicTexture, x - scaledMinWH / 2, y - scaledMinWH / 2, 0, 0, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
                        VehicleHudOverlay.renderKillIndicatorDynamic(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));
                    }

                    if (crosshairPath.equals("@VehicleCommonCannonZooming")) {
                        float fovAdjust = 70F / Minecraft.getInstance().options.fov().get();
                        float f = (float) Math.min(screenWidth, screenHeight);
                        float f1 = Math.min((float) screenWidth / f, (float) screenHeight / f) * fovAdjust;
                        int i = Mth.floor(f * f1);
                        int j = Mth.floor(f * f1);
                        int k = (screenWidth - i) / 2;
                        int l = (screenHeight - j) / 2;
                        preciseBlit(guiGraphics, texture, k, l, 0, 0, i, j, i, j);
                        VehicleHudOverlay.renderKillIndicator(guiGraphics, screenWidth, screenHeight);
                    } else {
                        RenderHelper.blit(poseStack, texture, centerW, centerH, 0, 0, scaledMinWH, scaledMinWH, scaledMinWH, scaledMinWH, color);
                        VehicleHudOverlay.renderKillIndicator(guiGraphics, screenWidth, screenHeight);
                    }

                }
            }

            poseStack.popPose();
            poseStack.pushPose();

            VehicleMainWeaponHudOverlay.renderWeaponInfoFirst(guiGraphics, vehicle, player, data, mc.font, screenWidth, screenHeight, color);

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

                double health = 1 - vehicle.getHealth() / vehicle.getMaxHealth();
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("HP " +
                        FormatTool.format0D(100 * vehicle.getHealth() / vehicle.getMaxHealth())), 30, 1, Mth.hsvToRgb(0F, (float) health, 1F), false);

                if (vehicle.hasDecoy()) {
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("SMOKE " + vehicle.getDecoyState()), 30, 11, vehicle.getDecoyState().equals("READY") ? -1 : 0xFF0000, false);
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
