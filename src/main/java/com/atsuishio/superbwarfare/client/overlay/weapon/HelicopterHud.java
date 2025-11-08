package com.atsuishio.superbwarfare.client.overlay.weapon;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay;
import com.atsuishio.superbwarfare.client.overlay.VehicleWeaponHudOverlay;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.tools.FormatTool;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.joml.Math;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;

@OnlyIn(Dist.CLIENT)
public class HelicopterHud {

    public static final String ID = "@Helicopter";

    private static final ResourceLocation HELI_BASE = Mod.loc("textures/overlay/vehicle/helicopter/heli_base.png");
    private static final ResourceLocation ROLL_IND = Mod.loc("textures/overlay/vehicle/helicopter/roll_ind.png");
    private static final ResourceLocation HELI_LINE = Mod.loc("textures/overlay/vehicle/helicopter/heli_line.png");
    private static final ResourceLocation HELI_POWER_RULER = Mod.loc("textures/overlay/vehicle/helicopter/heli_power_ruler.png");
    private static final ResourceLocation HELI_POWER = Mod.loc("textures/overlay/vehicle/helicopter/heli_power.png");
    private static final ResourceLocation HELI_VY_MOVE = Mod.loc("textures/overlay/vehicle/helicopter/heli_vy_move.png");
    private static final ResourceLocation SPEED_FRAME = Mod.loc("textures/overlay/vehicle/helicopter/speed_frame.png");
    private static final ResourceLocation CROSSHAIR_IND = Mod.loc("textures/overlay/vehicle/helicopter/crosshair_ind.png");
    private static final ResourceLocation HELI_DRIVER_ANGLE = Mod.loc("textures/overlay/vehicle/helicopter/heli_driver_angle.png");

    private static final ResourceLocation COMPASS = Mod.loc("textures/overlay/vehicle/base/compass.png");
    private static final ResourceLocation CROSSHAIR_3P = Mod.loc("textures/overlay/vehicle/crosshair/third_camera.png");

    private static float scopeScale = 1;
    private static float lerpVy = 1;
    private static float lerpPower = 1;

    public static void render(VehicleEntity vehicle, Player player, ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = gui.getMinecraft();
        PoseStack poseStack = guiGraphics.pose();

        poseStack.pushPose();

        int color = vehicle.getHudColor();

        int index = vehicle.getSeatIndex(player);
        var data = vehicle.getGunData(index);
        if (data == null) {
            scopeScale = 0.7f;
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        scopeScale = Mth.lerp(partialTick, scopeScale, 1F);
        float f = (float) Math.min(screenWidth, screenHeight);
        float f1 = Math.min((float) screenWidth / f, (float) screenHeight / f) * scopeScale;
        float i = Mth.floor(f * f1);
        float j = Mth.floor(f * f1);
        float k = (screenWidth - i) / 2f;
        float l = (screenHeight - j) / 2f;

        Vec3 pos = vehicle.getShootPos(player, partialTick).add(vehicle.getViewVec(player, partialTick).scale(192));
        Vec3 screenPos = VectorUtil.worldToScreen(pos);
        float x = (float) screenPos.x;
        float y = (float) screenPos.y;

        if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) {
            RenderHelper.blit(poseStack, HELI_BASE, k, l, 0, 0, i, j, i, j, color);

            float diffY = Mth.wrapDegrees(Mth.lerp(partialTick, player.yHeadRotO, player.getYHeadRot()) - Mth.lerp(partialTick, vehicle.yRotO, vehicle.getYRot())) * 0.35f;
            float diffX = Mth.wrapDegrees(Mth.lerp(partialTick, player.xRotO, player.getXRot()) - Mth.lerp(partialTick, vehicle.xRotO, vehicle.getXRot())) * 0.072f;
            RenderHelper.blit(poseStack, HELI_DRIVER_ANGLE, k + diffY, l + diffX, 0, 0, i, j, i, j, color);

            RenderHelper.blit(poseStack, COMPASS, (float) screenWidth / 2 - 128, 6F, 128 + (64F / 45 * vehicle.getYRot()), 0, 256, 16, 512, 16, color);

            poseStack.pushPose();
            poseStack.rotateAround(Axis.ZP.rotationDegrees(-vehicle.getRoll(partialTick)), screenWidth / 2f, screenHeight / 2f, 0);
            float pitch = vehicle.getPitch(partialTick);
            RenderHelper.blit(poseStack, HELI_LINE, (float) screenWidth / 2 - 128, (float) screenHeight / 2 - 512 - 5.475f * pitch, 0, 0, 256, 1024, 256, 1024, color);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), screenWidth / 2f, screenHeight / 2f - 56, 0);
            RenderHelper.blit(poseStack, ROLL_IND, (float) screenWidth / 2 - 8, (float) screenHeight / 2 - 88, 0, 0, 16, 16, 16, 16, color);
            poseStack.popPose();

            RenderHelper.blit(poseStack, HELI_POWER_RULER, (float) screenWidth / 2 + 100, (float) screenHeight / 2 - 64, 0, 0, 64, 128, 64, 128, color);

            double height = vehicle.position().distanceTo((Vec3.atLowerCornerOf(vehicle.level().clip(new ClipContext(vehicle.position(), vehicle.position().add(new Vec3(0, -1, 0).scale(100)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, vehicle)).getBlockPos())));
            double blockInWay = vehicle.position().distanceTo((Vec3.atLowerCornerOf(vehicle.level().clip(new ClipContext(vehicle.position(), vehicle.position().add(new Vec3(vehicle.getDeltaMovement().x, vehicle.getDeltaMovement().y + 0.06, vehicle.getDeltaMovement().z).normalize().scale(100)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, vehicle)).getBlockPos())));

            float power = vehicle.getPower();
            lerpPower = Mth.lerp(0.001f * partialTick, lerpPower, power);
            RenderHelper.blit(poseStack, HELI_POWER, (float) screenWidth / 2 + 130f, ((float) screenHeight / 2 - 64 + 124 - power * 980), 0, 0, 4, power * 980, 4, power * 980, color);

            lerpVy = (float) Mth.lerp(0.021f * partialTick, lerpVy, vehicle.getDeltaMovement().y());
            RenderHelper.blit(poseStack, HELI_VY_MOVE, (float) screenWidth / 2 + 138, ((float) screenHeight / 2 - 3 - Math.max(lerpVy * 20, -24) * 2.5f), 0, 0, 8, 8, 8, 8, color);

            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(lerpVy * 20, "m/s")),
                    screenWidth / 2 + 146, (int) (screenHeight / 2F - 3 - Math.max(lerpVy * 20, -24) * 2.5), (lerpVy * 20 < -24 || ((lerpVy * 20 < -10 || (lerpVy * 20 < -1 && length(vehicle.getDeltaMovement().x, vehicle.getDeltaMovement().y, vehicle.getDeltaMovement().z) * 72 > 100)) && height < 36) || (length(vehicle.getDeltaMovement().x, vehicle.getDeltaMovement().y, vehicle.getDeltaMovement().z) * 72 > 40 && blockInWay < 72) ? -65536 : color), false);
            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(vehicle.getY())),
                    screenWidth / 2 + 104, screenHeight / 2, color, false);
            RenderHelper.blit(poseStack, SPEED_FRAME, (float) screenWidth / 2 - 144, (float) screenHeight / 2 - 6, 0, 0, 50, 18, 50, 18, color);
            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(length(vehicle.getDeltaMovement().x, vehicle.getDeltaMovement().y, vehicle.getDeltaMovement().z) * 72, "km/h")),
                    screenWidth / 2 - 140, screenHeight / 2, color, false);

            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("FLARE " + vehicle.getDecoyState()), screenWidth / 2 - 160, screenHeight / 2 - 50, vehicle.getDecoyState().equals("READY") ? color : 0xFF0000, false);

            if (lerpVy * 20 < -24) {
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("SINK RATE，PULL UP!"),
                        screenWidth / 2 - 53, screenHeight / 2 + 24, -65536, false);
            } else if (((lerpVy * 20 < -10 || (lerpVy * 20 < -1 && length(vehicle.getDeltaMovement().x, vehicle.getDeltaMovement().y, vehicle.getDeltaMovement().z) * 72 > 100)) && height < 36)
                    || (length(vehicle.getDeltaMovement().x, vehicle.getDeltaMovement().y, vehicle.getDeltaMovement().z) * 72 > 40 && blockInWay < 72)) {
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("TERRAIN TERRAIN"),
                        screenWidth / 2 - 42, screenHeight / 2 + 24, -65536, false);
            }

            VehicleWeaponHudOverlay.renderEnergyInfo(vehicle, guiGraphics, screenWidth, screenHeight, mc.font);

            RenderHelper.blit(poseStack, CROSSHAIR_IND, x - 8, y - 8, 0, 0, 16, 16, 16, 16, color);
            VehicleHudOverlay.renderKillIndicator3P(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));
        } else if (VectorUtil.canSee(pos)) {
            poseStack.pushPose();
            poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), x, y, 0);
            preciseBlit(guiGraphics, CROSSHAIR_3P, x - 8, y - 8, 0, 0, 16, 16, 16, 16);
            VehicleHudOverlay.renderKillIndicator3P(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));

            poseStack.pushPose();

            poseStack.translate(x, y, 0);
            poseStack.scale(0.75f, 0.75f, 1);

            VehicleWeaponHudOverlay.renderWeaponInfoThird(guiGraphics, vehicle, player, data, mc.font);

            if (vehicle.hasDecoy()) {
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("FLARE " + vehicle.getDecoyState()), 30, 1, vehicle.getDecoyState().equals("READY") ? -1 : 0xFF0000, false);
            }

            poseStack.popPose();
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }
}
