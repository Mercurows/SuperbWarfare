package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.ClickHandler;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.base.AircraftEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;

@OnlyIn(Dist.CLIENT)
public class AircraftOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("aircraft_hud");

    private static float scopeScale = 1;
    private static float lerpVy = 1;
    private static float lerpPower = 1;

    @Override
    public void render(GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = guiGraphics.pose();

        var partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);

        if (player == null) return;

        if (ClickHandler.isEditing)
            return;

        if (player.getVehicle() instanceof AircraftEntity aircraftEntity && aircraftEntity instanceof MobileVehicleEntity mobileVehicle && aircraftEntity.isDriver(player) && player.getVehicle() instanceof WeaponVehicleEntity weaponVehicle) {
            poseStack.pushPose();

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            scopeScale = Mth.lerp(partialTick, scopeScale, 0.75F);
            float f = (float) Math.min(screenWidth, screenHeight);
            float f1 = Math.min((float) screenWidth / f, (float) screenHeight / f) * scopeScale;
            float i = Mth.floor(f * f1);
            float j = Mth.floor(f * f1);
            float k = ((screenWidth - i) / 2);
            float l = ((screenHeight - j) / 2);

            float diffY = Mth.wrapDegrees(Mth.lerp(partialTick, player.yHeadRotO, player.getYHeadRot()) - Mth.lerp(partialTick, mobileVehicle.yRotO, mobileVehicle.getYRot())) * 0.5f;
            float diffX = Mth.wrapDegrees(Mth.lerp(partialTick, player.xRotO, player.getXRot()) - Mth.lerp(partialTick, mobileVehicle.xRotO, mobileVehicle.getXRot())) * 0.5f;

            float fovAdjust2 = (float) (Minecraft.getInstance().options.fov().get() / 30) - 1;
            double zoom = 0.96 * 3 + 0.06 * fovAdjust2;

            Vec3 pos = aircraftEntity.shootPos(partialTick).add(mobileVehicle.getViewVector(partialTick).scale(192));
            Vec3 posCross = aircraftEntity.shootPos(partialTick).add(aircraftEntity.shootVec(partialTick).scale(192));
            Vec3 lookAngle = player.getViewVector(partialTick).normalize().scale(pos.distanceTo(cameraPos) * (1 - 1.0 / zoom));

            var cPos = cameraPos.add(lookAngle);

            Vec3 p = RenderHelper.worldToScreen(pos, ClientEventHandler.zoomVehicle ? cPos : cameraPos);
            Vec3 pCross = RenderHelper.worldToScreen(posCross, ClientEventHandler.zoomVehicle ? cPos : cameraPos);

            if (p != null) {
                poseStack.pushPose();
                float x = (float) p.x;
                float y = (float) p.y;

                if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {

                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                    RenderSystem.enableBlend();
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    RenderSystem.setShaderColor(1, 1, 1, 1);

                    preciseBlit(guiGraphics, Mod.loc("textures/screens/aircraft/hud_base.png"), x - 128, y - 128, 0, 0, 256, 256, 256, 256);

                    preciseBlit(guiGraphics, Mod.loc("textures/screens/compass.png"), x - 128, (float) 6, 128 + ((float) 64 / 45 * mobileVehicle.getYRot()), 0, 256, 16, 512, 16);

                    poseStack.pushPose();
                    poseStack.rotateAround(Axis.ZP.rotationDegrees(aircraftEntity.getRotZ(partialTick)), x, y, 0);
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/helicopter/roll_ind.png"), x - 8, y + 112, 0, 0, 16, 16, 16, 16);
                    poseStack.popPose();


                    double height = mobileVehicle.position().distanceTo((Vec3.atLowerCornerOf(mobileVehicle.level().clip(new ClipContext(mobileVehicle.position(), mobileVehicle.position().add(new Vec3(0, -1, 0).scale(100)),
                            ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, mobileVehicle)).getBlockPos())));
                    double blockInWay = mobileVehicle.position().distanceTo((Vec3.atLowerCornerOf(mobileVehicle.level().clip(new ClipContext(mobileVehicle.position(), mobileVehicle.position().add(new Vec3(mobileVehicle.getDeltaMovement().x, mobileVehicle.getDeltaMovement().y + 0.06, mobileVehicle.getDeltaMovement().z).normalize().scale(100)),
                            ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, mobileVehicle)).getBlockPos())));


                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(lerpVy * 20, "m/s")), (int) x + 146, (int) y, 0x66FF00, false);
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(mobileVehicle.getY())), (int) x + 104, (int) y, 0x66FF00, false);
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/helicopter/speed_frame.png"), x - 144, y, 0, 0, 50, 18, 50, 18);
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(mobileVehicle.getDeltaMovement().dot(mobileVehicle.getViewVector(1)) * 72, "km/h")), (int) x - 140, (int) y, 0x66FF00, false);

//                if (mobileVehicle instanceof Ah6Entity ah6Entity) {
//                    if (weaponVehicle.getWeaponIndex(0) == 0) {
//                        double heat = 1 - ah6Entity.getEntityData().get(HEAT) / 100.0F;
//                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("20MM CANNON " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : aircraftEntity.getAmmoCount(player))), screenWidth / 2 - 160, screenHeight / 2 - 60, Mth.hsvToRgb((float) heat / 3.745318352059925F, 1.0F, 1.0F), false);
//                    } else {
//                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("70MM ROCKET " + aircraftEntity.getAmmoCount(player)), screenWidth / 2 - 160, screenHeight / 2 - 60, 0x66FF00, false);
//                    }
//                }

                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("FLARE " + aircraftEntity.getDecoy()), (int) x - 160, (int) y - 50, 0x66FF00, false);

                    if (lerpVy * 20 < -24) {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("SINK RATE，PULL UP!"),
                                (int) x - 53, (int) y + 24, -65536, false);
                    } else if (((lerpVy * 20 < -10 || (lerpVy * 20 < -1 && length(mobileVehicle.getDeltaMovement().x, mobileVehicle.getDeltaMovement().y, mobileVehicle.getDeltaMovement().z) * 72 > 100)) && height < 36)
                            || (length(mobileVehicle.getDeltaMovement().x, mobileVehicle.getDeltaMovement().y, mobileVehicle.getDeltaMovement().z) * 72 > 40 && blockInWay < 72)) {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("TERRAIN TERRAIN"),
                                (int) x - 42, (int) y + 24, -65536, false);
                    }

                    if (mobileVehicle.getEnergy() < 0.02 * mobileVehicle.getMaxEnergy()) {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("NO POWER!"),
                                (int) x - 144, (int) y + 14, -65536, false);
                    } else if (mobileVehicle.getEnergy() < 0.2 * mobileVehicle.getMaxEnergy()) {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("LOW POWER"),
                                (int) x - 144, (int) y + 14, 0xFF6B00, false);
                    }

                    //角度
                    poseStack.pushPose();

                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                    RenderSystem.enableBlend();
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    RenderSystem.setShaderColor(1, 1, 1, 1);

                    poseStack.rotateAround(Axis.ZP.rotationDegrees(-aircraftEntity.getRotZ(partialTick)), x, y, 0);
                    float pitch = aircraftEntity.getRotX(partialTick);
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/aircraft/hud_line.png"), x - 128 + diffY, y - 512 - 5.475f * pitch, 0, 0, 256, 1024, 256, 1024);
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/aircraft/hud_line2.png"), x - 128 + diffY, y - 512 - 5.475f * pitch + diffX, 0, 0, 256, 1024, 256, 1024);
                    poseStack.popPose();
                }
            }

            // 准星
            if (pCross != null) {
                poseStack.pushPose();
                float x = (float) pCross.x;
                float y = (float) pCross.y;

                if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {

                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                    RenderSystem.enableBlend();
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    RenderSystem.setShaderColor(1, 1, 1, 1);

                    preciseBlit(guiGraphics, Mod.loc("textures/screens/aircraft/hud_base2.png"), x - 96 + diffX, y - 96 + diffY, 0, 0, 192, 192, 192, 192);

                    preciseBlit(guiGraphics, Mod.loc("textures/screens/aircraft/crosshair_ind.png"), x - 16, y - 16, 0, 0, 32, 32, 32, 32);
                    renderKillIndicator(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));

                } else if (mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
                    poseStack.pushPose();
                    poseStack.rotateAround(Axis.ZP.rotationDegrees(aircraftEntity.getRotZ(partialTick)), x, y, 0);
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/drone.png"), x - 8, y - 8, 0, 0, 16, 16, 16, 16);
                    renderKillIndicator(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));

                    poseStack.pushPose();

                    poseStack.translate(x, y, 0);
                    poseStack.scale(0.75f, 0.75f, 1);

//                    if (mobileVehicle instanceof Ah6Entity ah6Entity) {
//                        if (weaponVehicle.getWeaponIndex(0) == 0) {
//                            double heat = ah6Entity.getEntityData().get(HEAT) / 100.0F;
//                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("20MM CANNON " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : aircraftEntity.getAmmoCount(player))), 25, -9, Mth.hsvToRgb(0F, (float) heat, 1.0F), false);
//                        } else {
//                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("70MM ROCKET " + aircraftEntity.getAmmoCount(player)), 25, -9, -1, false);
//                        }
//                    }

                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("FLARE " + aircraftEntity.getDecoy()), 25, 1, -1, false);
                    poseStack.popPose();
                    poseStack.popPose();
                }
                poseStack.popPose();
            }

            poseStack.popPose();
        } else {
            scopeScale = 0.7f;
        }
    }

    private static void renderKillIndicator(GuiGraphics guiGraphics, float posX, float posY) {
        VehicleHudOverlay.renderKillIndicator3P(guiGraphics, posX, posY);
    }

    public static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }
}
