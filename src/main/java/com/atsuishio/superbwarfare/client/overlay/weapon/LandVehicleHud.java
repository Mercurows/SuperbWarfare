package com.atsuishio.superbwarfare.client.overlay.weapon;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay;
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.MathTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.joml.Math;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.*;

@OnlyIn(Dist.CLIENT)
public class LandVehicleHud {

    public static final String ID = "@Land";

    private static final ResourceLocation COMPASS = Mod.loc("textures/overlay/vehicle/base/compass.png");
    private static final ResourceLocation ROLL_IND = Mod.loc("textures/overlay/vehicle/helicopter/roll_ind.png");

    // 地面载具车身显示
    private static final ResourceLocation FRAME = Mod.loc("textures/overlay/vehicle/land/tv_frame.png");
    private static final ResourceLocation LINE = Mod.loc("textures/overlay/vehicle/land/line.png");
    private static final ResourceLocation BARREL = Mod.loc("textures/overlay/vehicle/land/line.png");
    private static final ResourceLocation BODY = Mod.loc("textures/overlay/vehicle/land/body.png");
    private static final ResourceLocation LEFT_WHEEL = Mod.loc("textures/overlay/vehicle/land/left_wheel.png");
    private static final ResourceLocation RIGHT_WHEEL = Mod.loc("textures/overlay/vehicle/land/right_wheel.png");
    private static final ResourceLocation ENGINE = Mod.loc("textures/overlay/vehicle/land/engine.png");

    public static float lerpRecoil;

    public static void render(VehicleEntity vehicle, Player player, ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = gui.getMinecraft();

        PoseStack poseStack = guiGraphics.pose();

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 viewVec = new Vec3(camera.getLookVector());

        int color = vehicle.getHudColor();

        poseStack.pushPose();

        float recoil = Mth.lerp(partialTick, (float) vehicle.recoilShakeO, (float) vehicle.getRecoilShake());
        lerpRecoil = Mth.lerp(0.1f * partialTick, lerpRecoil, recoil * (float) (2 * (Math.random() - 0.5f)));
        poseStack.translate(lerpRecoil * 6 + screenWidth * 0.025f * recoil, recoil * 3 + screenHeight * 0.025f * recoil, 0);
        poseStack.scale(1 - recoil * 0.05f, 1 - recoil * 0.05f, 1);
        poseStack.rotateAround(Axis.ZP.rotationDegrees(-0.3f * ClientEventHandler.cameraRoll + 2.5f * lerpRecoil), screenWidth / 2f, screenHeight / 2f, 0);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) {
            int addW = (screenWidth / screenHeight) * 48;
            int addH = (screenWidth / screenHeight) * 27;
            preciseBlit(guiGraphics, FRAME, (float) -addW / 2, (float) -addH / 2, 10, 0, 0F, screenWidth + addW, screenHeight + addH, screenWidth + addW, screenHeight + addH);
            RenderHelper.blit(poseStack, LINE, screenWidth / 2f - 64, screenHeight - 56, 0, 0F, 128, 1, 128, 1, color);

            // 指南针
            RenderHelper.blit(poseStack, COMPASS, (float) screenWidth / 2 - 128, 10F, 128 + (64F / 45 * player.getYRot()), 0, 256, 16, 512, 16, color);
            RenderHelper.blit(poseStack, ROLL_IND, screenWidth / 2f - 8, 30, 0, 0F, 16, 16, 16, 16, color);

            int turretHeal = (int) (100 - (100 * vehicle.getEntityData().get(TURRET_HEALTH) / vehicle.getTurretMaxHealth()));
            RenderHelper.blit(poseStack, BARREL, screenWidth / 2f + 112, screenHeight - 71, 0, 0F, 1, 16, 1, 16, MathTool.getGradientColor(color, 0xFF0000, turretHeal, 2));

            // 车身方向
            poseStack.pushPose();
            poseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, vehicle.turretYRotO, vehicle.getTurretYRot())), screenWidth / 2f + 112, screenHeight - 56, 0);
            int bodyHeal = (int) (100 - (100 * vehicle.getHealth() / vehicle.getMaxHealth()));
            RenderHelper.blit(poseStack, BODY, screenWidth / 2f + 96, screenHeight - 72, 0, 0F, 32, 32, 32, 32, MathTool.getGradientColor(color, 0xFF0000, bodyHeal, 2));
            int leftWheelHeal = (int) (100 - (100 * vehicle.getEntityData().get(L_WHEEL_HEALTH) / vehicle.getWheelMaxHealth()));
            RenderHelper.blit(poseStack, LEFT_WHEEL, screenWidth / 2f + 96, screenHeight - 72, 0, 0F, 32, 32, 32, 32, MathTool.getGradientColor(color, 0xFF0000, leftWheelHeal, 2));
            int rightWheelHeal = (int) (100 - (100 * vehicle.getEntityData().get(R_WHEEL_HEALTH) / vehicle.getWheelMaxHealth()));
            RenderHelper.blit(poseStack, RIGHT_WHEEL, screenWidth / 2f + 96, screenHeight - 72, 0, 0F, 32, 32, 32, 32, MathTool.getGradientColor(color, 0xFF0000, rightWheelHeal, 2));
            int engineHeal = (int) (100 - (100 * vehicle.getEntityData().get(MAIN_ENGINE_HEALTH) / vehicle.getEngineMaxHealth()));
            RenderHelper.blit(poseStack, ENGINE, screenWidth / 2f + 96, screenHeight - 72, 0, 0F, 32, 32, 32, 32, MathTool.getGradientColor(color, 0xFF0000, engineHeal, 2));
            poseStack.popPose();

            // 时速
            guiGraphics.drawString(mc.font, Component.literal(FormatTool.format0D(vehicle.getDeltaMovement().dot(vehicle.getViewVector(partialTick)) * 72, " km/h")),
                    screenWidth / 2 + 160, screenHeight / 2 - 48, color, false);

            // 低电量警告
            VehicleMainWeaponHudOverlay.renderEnergyInfo(vehicle, guiGraphics, screenWidth, screenHeight, mc.font);

            // 测距
            boolean lookAtEntity = false;

            BlockHitResult result = player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getViewVector(1).scale(512)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            Vec3 hitPos = result.getLocation();

            double blockRange = player.getEyePosition(1).distanceTo(hitPos);
            double entityRange = 0;

            Entity lookingEntity = TraceTool.camerafFindLookingEntity(player, cameraPos, viewVec, 512);
            if (lookingEntity != null) {
                lookAtEntity = true;
                entityRange = player.distanceTo(lookingEntity);
            }

            // 测距
            if (lookAtEntity) {
                guiGraphics.drawString(mc.font, Component.literal(FormatTool.format1D(entityRange, "m")),
                        screenWidth / 2 - 6, screenHeight - 53, color, false);
            } else {
                if (blockRange > 500) {
                    guiGraphics.drawString(mc.font, Component.literal("---m"), screenWidth / 2 - 6, screenHeight - 53, color, false);
                } else {
                    guiGraphics.drawString(mc.font, Component.literal(FormatTool.format1D(blockRange, "m")),
                            screenWidth / 2 - 6, screenHeight - 53, color, false);
                }
            }

            // 血量
            int heal = (int) (100 - (100 * vehicle.getHealth() / vehicle.getMaxHealth()));
            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(100 - heal)), screenWidth / 2 - 165, screenHeight / 2 - 46, MathTool.getGradientColor(color, 0xFF0000, bodyHeal, 2), false);

            // 诱饵
            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("SMOKE " + vehicle.getDecoyState()), screenWidth / 2 - 165, screenHeight / 2 - 36, vehicle.getDecoyState().equals("READY") ? color : 0xFF0000, false);

            VehicleHudOverlay.renderKillIndicator(guiGraphics, screenWidth, screenHeight);
        }
        poseStack.popPose();
    }
}
