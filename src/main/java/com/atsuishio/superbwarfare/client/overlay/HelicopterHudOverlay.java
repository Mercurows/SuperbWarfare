package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.Ah6Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.AirEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.MathTool;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Math;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.HEAT;

@OnlyIn(Dist.CLIENT)
public class HelicopterHudOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("helicopter_hud");

    private static float scopeScale = 1;
    private static float lerpVy = 1;
    private static float lerpPower = 1;

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        PoseStack poseStack = guiGraphics.pose();

        if (player == null) return;

        if (ClientEventHandler.isEditing)
            return;

        if (player.getVehicle() instanceof AirEntity airEntity
                && player.getVehicle() instanceof VehicleEntity vehicle
                && airEntity.isDriver(player) && player.getVehicle() instanceof WeaponVehicleEntity weaponVehicle
                && vehicle.vehicleType == VehicleEntity.VehicleType.HELICOPTER) {
            poseStack.pushPose();

            int color = vehicle.getHudColor();

            poseStack.translate(-6 * ClientEventHandler.turnRot[1], -6 * ClientEventHandler.turnRot[0], 0);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
            scopeScale = Mth.lerp(partialTick, scopeScale, 1F);
            float f = (float) Math.min(screenWidth, screenHeight);
            float f1 = Math.min((float) screenWidth / f, (float) screenHeight / f) * scopeScale;
            float i = Mth.floor(f * f1);
            float j = Mth.floor(f * f1);
            float k = ((screenWidth - i) / 2);
            float l = ((screenHeight - j) / 2);

            if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
                RenderHelper.preciseBlitWithColor(guiGraphics, Mod.loc("textures/screens/helicopter/heli_base.png"), k, l, 0, 0.0F, i, j, i, j, color);
                renderDriverAngle(guiGraphics, player, vehicle, k, l, i, j, partialTick, color, poseStack);

                RenderHelper.preciseBlitWithColor(guiGraphics, Mod.loc("textures/screens/compass.png"), (float) screenWidth / 2 - 128, (float) 6, 128 + ((float) 64 / 45 * vehicle.getYRot()), 0, 256, 16, 512, 16, color);

                poseStack.pushPose();
                poseStack.rotateAround(Axis.ZP.rotationDegrees(-vehicle.getRoll(partialTick)), screenWidth / 2f, screenHeight / 2f, 0);
                float pitch = vehicle.getPitch(partialTick);

                RenderHelper.preciseBlitWithColor(guiGraphics, Mod.loc("textures/screens/helicopter/heli_line.png"), (float) screenWidth / 2 - 128, (float) screenHeight / 2 - 512 - 5.475f * pitch, 0, 0, 256, 1024, 256, 1024, color);
                poseStack.popPose();
                poseStack.pushPose();
                poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), screenWidth / 2f, screenHeight / 2f - 56, 0);
                RenderHelper.preciseBlitWithColor(guiGraphics, Mod.loc("textures/screens/helicopter/roll_ind.png"), (float) screenWidth / 2 - 8, (float) screenHeight / 2 - 88, 0, 0, 16, 16, 16, 16, color);
                poseStack.popPose();

                RenderHelper.preciseBlitWithColor(guiGraphics, Mod.loc("textures/screens/helicopter/heli_power_ruler.png"), (float) screenWidth / 2 + 100, (float) screenHeight / 2 - 64, 0, 0, 64, 128, 64, 128, color);

                double height = vehicle.position().distanceTo((Vec3.atLowerCornerOf(vehicle.level().clip(new ClipContext(vehicle.position(), vehicle.position().add(new Vec3(0, -1, 0).scale(100)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, vehicle)).getBlockPos())));
                double blockInWay = vehicle.position().distanceTo((Vec3.atLowerCornerOf(vehicle.level().clip(new ClipContext(vehicle.position(), vehicle.position().add(new Vec3(vehicle.getDeltaMovement().x, vehicle.getDeltaMovement().y + 0.06, vehicle.getDeltaMovement().z).normalize().scale(100)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, vehicle)).getBlockPos())));

                float power = vehicle.getPower();
                lerpPower = Mth.lerp(0.001f * partialTick, lerpPower, power);
                RenderHelper.preciseBlitWithColor(guiGraphics, Mod.loc("textures/screens/helicopter/heli_power.png"), (float) screenWidth / 2 + 130f, ((float) screenHeight / 2 - 64 + 124 - power * 980), 0, 0, 4, power * 980, 4, power * 980, color);
                lerpVy = (float) Mth.lerp(0.021f * partialTick, lerpVy, vehicle.getDeltaMovement().y());
                RenderHelper.preciseBlitWithColor(guiGraphics, Mod.loc("textures/screens/helicopter/heli_vy_move.png"), (float) screenWidth / 2 + 138, ((float) screenHeight / 2 - 3 - Math.max(lerpVy * 20, -24) * 2.5f), 0, 0, 8, 8, 8, 8, color);
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(lerpVy * 20, "m/s")),
                        screenWidth / 2 + 146, (int) (screenHeight / 2F - 3 - Math.max(lerpVy * 20, -24) * 2.5), (lerpVy * 20 < -24 || ((lerpVy * 20 < -10 || (lerpVy * 20 < -1 && length(vehicle.getDeltaMovement().x, vehicle.getDeltaMovement().y, vehicle.getDeltaMovement().z) * 72 > 100)) && height < 36) || (length(vehicle.getDeltaMovement().x, vehicle.getDeltaMovement().y, vehicle.getDeltaMovement().z) * 72 > 40 && blockInWay < 72) ? -65536 : color), false);
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(vehicle.getY())),
                        screenWidth / 2 + 104, screenHeight / 2, color, false);
                RenderHelper.preciseBlitWithColor(guiGraphics, Mod.loc("textures/screens/helicopter/speed_frame.png"), (float) screenWidth / 2 - 144, (float) screenHeight / 2 - 6, 0, 0, 50, 18, 50, 18, color);
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(length(vehicle.getDeltaMovement().x, vehicle.getDeltaMovement().y, vehicle.getDeltaMovement().z) * 72, "km/h")),
                        screenWidth / 2 - 140, screenHeight / 2, color, false);

                if (vehicle instanceof Ah6Entity ah6Entity) {
                    if (weaponVehicle.getWeaponIndex(0) == 0) {
                        int heat = ah6Entity.getEntityData().get(HEAT);
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("20MM CANNON " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : airEntity.getAmmoCount(player))), screenWidth / 2 - 160, screenHeight / 2 - 60, MathTool.getGradientColor(color, 0xFF0000, heat, 2), false);
                    } else {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("70MM ROCKET " + airEntity.getAmmoCount(player)), screenWidth / 2 - 160, screenHeight / 2 - 60, color, false);
                    }
                }

                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("FLARE " + vehicle.getDecoyCount()), screenWidth / 2 - 160, screenHeight / 2 - 50, color, false);

                if (lerpVy * 20 < -24) {
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("SINK RATE，PULL UP!"),
                            screenWidth / 2 - 53, screenHeight / 2 + 24, -65536, false);
                } else if (((lerpVy * 20 < -10 || (lerpVy * 20 < -1 && length(vehicle.getDeltaMovement().x, vehicle.getDeltaMovement().y, vehicle.getDeltaMovement().z) * 72 > 100)) && height < 36)
                        || (length(vehicle.getDeltaMovement().x, vehicle.getDeltaMovement().y, vehicle.getDeltaMovement().z) * 72 > 40 && blockInWay < 72)) {
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("TERRAIN TERRAIN"),
                            screenWidth / 2 - 42, screenHeight / 2 + 24, -65536, false);
                }

                if (vehicle.hasEnergyStorage()) {
                    if (vehicle.getEnergy() < 0.02 * vehicle.getMaxEnergy()) {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("NO POWER!"),
                                screenWidth / 2 - 144, screenHeight / 2 + 14, -65536, false);
                    } else if (vehicle.getEnergy() < 0.2 * vehicle.getMaxEnergy()) {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("LOW POWER"),
                                screenWidth / 2 - 144, screenHeight / 2 + 14, 0xFF6B00, false);
                    }
                }

            }

            Vec3 pos = airEntity.shootPos(partialTick).add(airEntity.shootVec(partialTick).scale(192));
            Vec3 p = VectorUtil.worldToScreen(pos);

            poseStack.pushPose();
            float x = (float) p.x;
            float y = (float) p.y;

            if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
                RenderHelper.preciseBlitWithColor(guiGraphics, Mod.loc("textures/screens/helicopter/crosshair_ind.png"), x - 8, y - 8, 0, 0, 16, 16, 16, 16, color);
                renderKillIndicator(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));
            } else if (VectorUtil.canSee(pos)) {
                poseStack.pushPose();
                poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), x, y, 0);
                preciseBlit(guiGraphics, Mod.loc("textures/screens/drone.png"), x - 8, y - 8, 0, 0, 16, 16, 16, 16);
                renderKillIndicator(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));

                poseStack.pushPose();

                poseStack.translate(x, y, 0);
                poseStack.scale(0.75f, 0.75f, 1);

                if (vehicle instanceof Ah6Entity ah6Entity) {
                    if (weaponVehicle.getWeaponIndex(0) == 0) {
                        double heat = ah6Entity.getEntityData().get(HEAT) / 100.0F;
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("20MM CANNON " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : airEntity.getAmmoCount(player))), 25, -9, Mth.hsvToRgb(0F, (float) heat, 1.0F), false);
                    } else {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("70MM ROCKET " + airEntity.getAmmoCount(player)), 25, -9, -1, false);
                    }
                }

                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("FLARE " + vehicle.getDecoyCount()), 25, 1, -1, false);
                poseStack.popPose();
                poseStack.popPose();
            }
            poseStack.popPose();

            poseStack.popPose();
        } else {
            scopeScale = 0.7f;
        }
    }

    private static void renderKillIndicator(GuiGraphics guiGraphics, float posX, float posY) {
        VehicleHudOverlay.renderKillIndicator3P(guiGraphics, posX, posY);
    }

    private static void renderDriverAngle(GuiGraphics guiGraphics, Player player, Entity heli, float k, float l, float i, float j, float ticks, int color, PoseStack poseStack) {
        float diffY = Mth.wrapDegrees(Mth.lerp(ticks, player.yHeadRotO, player.getYHeadRot()) - Mth.lerp(ticks, heli.yRotO, heli.getYRot())) * 0.35f;
        float diffX = Mth.wrapDegrees(Mth.lerp(ticks, player.xRotO, player.getXRot()) - Mth.lerp(ticks, heli.xRotO, heli.getXRot())) * 0.072f;

        RenderHelper.preciseBlitWithColor(guiGraphics, Mod.loc("textures/screens/helicopter/heli_driver_angle.png"), k + diffY, l + diffX, 0, 0.0F, i, j, i, j, color);
    }

    public static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }
}
