package com.atsuishio.superbwarfare.client.overlay.weapon;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.MathTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Math;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;

// TODO 预制通用固定翼飞机HUD，提取准星
@OnlyIn(Dist.CLIENT)
public class AircraftHud {

    public static final String ID = "@Aircraft";

    private static float lerpVy = 1;
    private static float lerpG = 1;
    private static float diffY;
    private static float diffX;


    private static final ResourceLocation BOMB_SCOPE = Mod.loc("textures/overlay/vehicle/aircraft/bomb_scope.png");
    private static final ResourceLocation BOMB_SCOPE_PITCH = Mod.loc("textures/overlay/vehicle/aircraft/bomb_scope_pitch.png");
    private static final ResourceLocation HUD_BASE_MISSILE = Mod.loc("textures/overlay/vehicle/aircraft/hud_base_missile.png");
    private static final ResourceLocation HUD_BASE = Mod.loc("textures/overlay/vehicle/aircraft/hud_base.png");
    private static final ResourceLocation HUD_LINE = Mod.loc("textures/overlay/vehicle/aircraft/hud_line.png");
    private static final ResourceLocation HUD_IND = Mod.loc("textures/overlay/vehicle/aircraft/hud_ind.png");
    private static final ResourceLocation HUD_BOMB = Mod.loc("textures/overlay/vehicle/aircraft/bomb.png");
    private static final ResourceLocation HUD_BASE2 = Mod.loc("textures/overlay/vehicle/aircraft/hud_base2.png");
    private static final ResourceLocation COMPASS_IND = Mod.loc("textures/overlay/vehicle/aircraft/compass_ind.png");
    private static final ResourceLocation HELICOPTER_ROLL_IND = Mod.loc("textures/overlay/vehicle/helicopter/roll_ind.png");
    private static final ResourceLocation HELICOPTER_SPEED_FRAME = Mod.loc("textures/overlay/vehicle/helicopter/speed_frame.png");

    private static final ResourceLocation COMPASS = Mod.loc("textures/overlay/vehicle/base/compass.png");
    private static final ResourceLocation CROSSHAIR_3P = Mod.loc("textures/overlay/vehicle/crosshair/third_camera.png");
    private static final ResourceLocation BOMB_RING = Mod.loc("textures/overlay/crosshair/rex_circle.png");

    public static void render(VehicleEntity vehicle, Player player, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = guiGraphics.pose();
        GunData gunData = vehicle.getGunData(player);

        if (gunData == null) return;

        poseStack.pushPose();

        boolean bomb = gunData.compute().crosshair.equals("@AirBomb");

        int color = vehicle.getHudColor();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        lerpVy = (float) Mth.lerp(0.021f * partialTick, lerpVy, vehicle.getDeltaMovement().y());
        diffY = (float) Mth.lerp(partialTick, diffY, ClientMouseHandler.lerpSpeedX);
        diffX = (float) Mth.lerp(partialTick, diffX, ClientMouseHandler.lerpSpeedY);

        Vec3 shootPos = vehicle.getShootPosForHud(player, partialTick);

        BlockHitResult result = player.level().clip(new ClipContext(shootPos, shootPos.add(vehicle.getShootDirectionForHud(player, partialTick).scale(512)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 hitPos = result.getLocation();

        double dis = shootPos.distanceTo(hitPos);

        Vec3 entityPos = TraceTool.vehicleFindLookingPos(player, vehicle, shootPos, 512, partialTick);

        if (entityPos != null) {
            dis = shootPos.distanceTo(entityPos);
        }

        Vec3 pos = cameraPos.add(vehicle.getViewVector(partialTick).scale(512));
        Vec3 posCross = shootPos.add(vehicle.getShootDirectionForHud(player, partialTick).scale(dis));

        if (bomb) {
            posCross = vehicle.bombHitPos(player, partialTick);
        }

        Vec3 p = VectorUtil.worldToScreen(pos);
        Vec3 pCross = VectorUtil.worldToScreen(posCross);

        // 投弹准星
        if (bomb && ClientEventHandler.zoomVehicle) {
            if (VectorUtil.canSee(posCross)) {
                float f = (float) Math.min(screenWidth, screenHeight);
                float f1 = Math.min((float) screenWidth / f, (float) screenHeight / f);
                int i = Mth.floor(f * f1);
                int j = Mth.floor(f * f1);

                float x = (float) pCross.x;
                float y = (float) pCross.y;


                poseStack.pushPose();
                poseStack.translate(x, y, 0);
                var component = vehicle.thirdPersonAmmoComponent(gunData, player);
                guiGraphics.drawString(mc.font, component, 25, -11, 1, false);
                poseStack.popPose();

                preciseBlit(guiGraphics, BOMB_SCOPE, x - 1.5f * i, y - 1.5f * j, 0, 0, 3 * i, 3 * j, 3 * i, 3 * j);

                poseStack.pushPose();
                poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), x, y, 0);
                preciseBlit(guiGraphics, BOMB_SCOPE_PITCH, x - 1.5f * i, y - 1.5f * j - 4 * vehicle.getPitch(partialTick), 0, 0, 3 * i, 3 * j, 3 * i, 3 * j);
                VehicleHudOverlay.renderKillIndicator3P(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));
                poseStack.popPose();
                return;
            }
        }

        poseStack.pushPose();

        if ((mc.options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) && VectorUtil.canSee(pos)) {
            float x = (float) p.x;
            float y = (float) p.y;

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            if (gunData.compute().crosshair.equals("@AirCraftMissile")) {
                RenderHelper.preciseBlitWithColor(guiGraphics, HUD_BASE_MISSILE, x - 160, y - 160, 0, 0, 320, 320, 320, 320, color);
            } else {
                RenderHelper.preciseBlitWithColor(guiGraphics, HUD_BASE, x - 160, y - 160, 0, 0, 320, 320, 320, 320, color);
            }

            //指南针
            RenderHelper.preciseBlitWithColor(guiGraphics, COMPASS, x - 128, y - 122, 128 + (64F / 45 * vehicle.getYRot()), 0, 256, 16, 512, 16, color);
            RenderHelper.preciseBlitWithColor(guiGraphics, COMPASS_IND, x - 4, y - 130, 0, 0, 8, 8, 8, 8, color);

            //滚转指示
            poseStack.pushPose();
            poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), x, y + 48, 0);
            RenderHelper.preciseBlitWithColor(guiGraphics, HELICOPTER_ROLL_IND, x - 4, y + 144, 0, 0, 8, 8, 8, 8, color);
            poseStack.popPose();

            //时速
            guiGraphics.drawString(mc.font, Component.literal(FormatTool.format0D(vehicle.getDeltaMovement().dot(vehicle.getViewVector(1)) * 72)),
                    (int) x - 105, (int) y - 61, color, false);
            //高度
            guiGraphics.drawString(mc.font, Component.literal(FormatTool.format0D(vehicle.getY())),
                    (int) x + 111 - 36, (int) y - 61, color, false);
            //框
            RenderHelper.preciseBlitWithColor(guiGraphics, HELICOPTER_SPEED_FRAME, x - 108, y - 64, 0, 0, 36, 12, 36, 12, color);
            RenderHelper.preciseBlitWithColor(guiGraphics, HELICOPTER_SPEED_FRAME, x + 108 - 36, y - 64, 0, 0, 36, 12, 36, 12, color);
            //垂直速度
            guiGraphics.drawString(mc.font, Component.literal(FormatTool.DECIMAL_FORMAT_1ZZ.format(lerpVy * 20)), (int) x - 96, (int) y + 60, color, false);
            //加速度
            lerpG = (float) Mth.lerp(0.1f * partialTick, lerpG, vehicle.acceleration / 9.8);
            guiGraphics.drawString(mc.font, Component.literal("M"), (int) x - 105, (int) y + 70, color, false);
            guiGraphics.drawString(mc.font, Component.literal("0.2"), (int) x - 96, (int) y + 70, color, false);
            guiGraphics.drawString(mc.font, Component.literal("G"), (int) x - 105, (int) y + 78, color, false);
            guiGraphics.drawString(mc.font, Component.literal(FormatTool.DECIMAL_FORMAT_1ZZ.format(lerpG)), (int) x - 96, (int) y + 78, color, false);

            // 热诱弹
            guiGraphics.drawString(mc.font, Component.literal("IR FLARES " + vehicle.getDecoyState()), (int) x + 72, (int) y, vehicle.getDecoyState().equals("READY") ? color : 0xFF0000, false);
            guiGraphics.drawString(mc.font, Component.literal("TGT"), (int) x + 76, (int) y + 78, color, false);

            // 武器名

            int heat = vehicle.getWeaponHeat(player);
            var component = vehicle.firstPersonAmmoComponent(gunData, player);

            guiGraphics.drawString(mc.font, component, (int) x - mc.font.width(component) / 2, (int) y + 67,
                    MathTool.getGradientColor(color, 0xFF0000, heat, 2), false);

            //角度
            poseStack.pushPose();

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            poseStack.rotateAround(Axis.ZP.rotationDegrees(-vehicle.getRoll(partialTick)), x, y, 0);
            float pitch = vehicle.getPitch(partialTick);
            RenderHelper.preciseBlitWithColor(guiGraphics, HUD_LINE, x - 96 + diffY, y - 128, 0, 448 + 4.10625f * pitch, 192, 256, 192, 1152, color);
            RenderHelper.preciseBlitWithColor(guiGraphics, HUD_IND, x - 18 + diffY, y - 12, 0, 0, 36, 24, 36, 24, color);
            RenderHelper.preciseBlitWithColor(guiGraphics, HUD_LINE, x - 96 + diffY, y - 128, 0, 448 + 4.10625f * pitch, 192, 256, 192, 1152, color);

            if (bomb) {
                RenderHelper.preciseBlitWithColor(guiGraphics, HUD_BOMB, x - 64 + diffY, y - 64, 0, 0, 128, 128, 128, 128, color);
            } else {
                RenderHelper.preciseBlitWithColor(guiGraphics, HUD_IND, x - 18 + diffY, y - 12, 0, 0, 36, 24, 36, 24, color);
            }

            poseStack.popPose();

            // 能量警告
            if (vehicle.hasEnergyStorage()) {
                if (vehicle.getEnergy() < 0.02 * vehicle.getMaxEnergy()) {
                    guiGraphics.drawString(mc.font, Component.literal("NO POWER!"),
                            (int) x - 144, (int) y + 14, -65536, false);
                } else if (vehicle.getEnergy() < 0.2 * vehicle.getMaxEnergy()) {
                    guiGraphics.drawString(mc.font, Component.literal("LOW POWER"),
                            (int) x - 144, (int) y + 14, 0xFF6B00, false);
                }
            }
        }

        poseStack.pushPose();

        if (VectorUtil.canSee(posCross)) {
            float x = (float) pCross.x;
            float y = (float) pCross.y;

            if ((mc.options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) && !gunData.compute().crosshair.equals("@AirBomb") && !gunData.compute().crosshair.equals("@AirCraftMissile")) {
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                RenderHelper.preciseBlitWithColor(guiGraphics, HUD_BASE2, x - 72 + diffY, y - 72 + diffX, 0, 0, 144, 144, 144, 144, color);

            } else if (mc.options.getCameraType() != CameraType.FIRST_PERSON && !ClientEventHandler.zoomVehicle) {

                poseStack.pushPose();
                poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), x, y, 0);
                poseStack.pushPose();
                poseStack.translate(x, y, 0);
                poseStack.scale(0.75f, 0.75f, 1);

                ResourceLocation cross = CROSSHAIR_3P;
                float size = 16;

                if (gunData.compute().crosshair.equals("@AirBomb")) {
                    cross = BOMB_RING;
                    size = 24;
                }

                float heat = vehicle.getWeaponHeat(player) / 100F;
                var component = vehicle.thirdPersonAmmoComponent(gunData, player);

                guiGraphics.drawString(mc.font, component, 25, -9, Mth.hsvToRgb(0F, heat, 1F), false);
                guiGraphics.drawString(mc.font, Component.literal("IR FLARES " + vehicle.getDecoyState()), 25, 1, vehicle.getDecoyState().equals("READY") ? -1 : 0xFF0000, false);

                poseStack.popPose();
                preciseBlit(guiGraphics, cross, x - 0.5f * size, y - 0.5f * size, 0, 0, size, size, size, size);
                VehicleHudOverlay.renderKillIndicator3P(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));
                poseStack.popPose();
            }
        }

        poseStack.popPose();
        poseStack.popPose();
    }

    public static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }
}
