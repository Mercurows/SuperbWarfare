package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.ClickHandler;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.Ah6Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.HelicopterEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Math;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity.HEAT;

@OnlyIn(Dist.CLIENT)
public class HelicopterHudOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("helicopter_hud");

    private static float scopeScale = 1;
    private static float lerpVy = 1;
    private static float lerpPower = 1;

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = guiGraphics.pose();

        if (player == null) return;

        if (ClickHandler.isEditing)
            return;

        if (player.getVehicle() instanceof HelicopterEntity iHelicopterEntity && player.getVehicle() instanceof MobileVehicleEntity mobileVehicle && iHelicopterEntity.isDriver(player) && player.getVehicle() instanceof WeaponVehicleEntity weaponVehicle) {
            poseStack.pushPose();

            poseStack.translate(-6 * ClientEventHandler.turnRot[1], -6 * ClientEventHandler.turnRot[0], 0);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
            scopeScale = Mth.lerp(partialTick, scopeScale, 1F);
            float f = (float) Math.min(w, h);
            float f1 = Math.min((float) w / f, (float) h / f) * scopeScale;
            float i = Mth.floor(f * f1);
            float j = Mth.floor(f * f1);
            float k = ((w - i) / 2);
            float l = ((h - j) / 2);

            if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
                preciseBlit(guiGraphics, Mod.loc("textures/screens/helicopter/heli_base.png"), k, l, 0, 0.0F, i, j, i, j);
                renderDriverAngle(guiGraphics, player, mobileVehicle, k, l, i, j, partialTick);

                preciseBlit(guiGraphics, Mod.loc("textures/screens/compass.png"), (float) w / 2 - 128, (float) 6, 128 + ((float) 64 / 45 * mobileVehicle.getYRot()), 0, 256, 16, 512, 16);

                poseStack.pushPose();
                poseStack.rotateAround(Axis.ZP.rotationDegrees(-iHelicopterEntity.getRotZ(partialTick)), w / 2f, h / 2f, 0);
                float pitch = iHelicopterEntity.getRotX(partialTick);

                preciseBlit(guiGraphics, Mod.loc("textures/screens/helicopter/heli_line.png"), (float) w / 2 - 128, (float) h / 2 - 512 - 5.475f * pitch, 0, 0, 256, 1024, 256, 1024);
                poseStack.popPose();
                poseStack.pushPose();
                poseStack.rotateAround(Axis.ZP.rotationDegrees(iHelicopterEntity.getRotZ(partialTick)), w / 2f, h / 2f - 56, 0);
                preciseBlit(guiGraphics, Mod.loc("textures/screens/helicopter/roll_ind.png"), (float) w / 2 - 8, (float) h / 2 - 88, 0, 0, 16, 16, 16, 16);
                poseStack.popPose();

                guiGraphics.blit(Mod.loc("textures/screens/helicopter/heli_power_ruler.png"), w / 2 + 100, h / 2 - 64, 0, 0, 64, 128, 64, 128);

                double height = mobileVehicle.position().distanceTo((Vec3.atLowerCornerOf(mobileVehicle.level().clip(new ClipContext(mobileVehicle.position(), mobileVehicle.position().add(new Vec3(0, -1, 0).scale(100)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, mobileVehicle)).getBlockPos())));
                double blockInWay = mobileVehicle.position().distanceTo((Vec3.atLowerCornerOf(mobileVehicle.level().clip(new ClipContext(mobileVehicle.position(), mobileVehicle.position().add(new Vec3(mobileVehicle.getDeltaMovement().x, mobileVehicle.getDeltaMovement().y + 0.06, mobileVehicle.getDeltaMovement().z).normalize().scale(100)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, mobileVehicle)).getBlockPos())));

                float power = iHelicopterEntity.getPower();
                lerpPower = Mth.lerp(0.001f * partialTick, lerpPower, power);
                preciseBlit(guiGraphics, Mod.loc("textures/screens/helicopter/heli_power.png"), (float) w / 2 + 130f, ((float) h / 2 - 64 + 124 - power * 980), 0, 0, 4, power * 980, 4, power * 980);
                lerpVy = (float) Mth.lerp(0.021f * partialTick, lerpVy, mobileVehicle.getDeltaMovement().y());
                preciseBlit(guiGraphics, Mod.loc("textures/screens/helicopter/heli_vy_move.png"), (float) w / 2 + 138, ((float) h / 2 - 3 - Math.max(lerpVy * 20, -24) * 2.5f), 0, 0, 8, 8, 8, 8);
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(lerpVy * 20, "m/s")),
                        w / 2 + 146, (int) (h / 2 - 3 - Math.max(lerpVy * 20, -24) * 2.5), (lerpVy * 20 < -24 || ((lerpVy * 20 < -10 || (lerpVy * 20 < -1 && length(mobileVehicle.getDeltaMovement().x, mobileVehicle.getDeltaMovement().y, mobileVehicle.getDeltaMovement().z) * 72 > 100)) && height < 36) || (length(mobileVehicle.getDeltaMovement().x, mobileVehicle.getDeltaMovement().y, mobileVehicle.getDeltaMovement().z) * 72 > 40 && blockInWay < 72) ? -65536 : 0x66FF00), false);
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(mobileVehicle.getY())),
                        w / 2 + 104, h / 2, 0x66FF00, false);
                preciseBlit(guiGraphics, Mod.loc("textures/screens/helicopter/speed_frame.png"), (float) w / 2 - 144, (float) h / 2 - 6, 0, 0, 50, 18, 50, 18);
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(length(mobileVehicle.getDeltaMovement().x, mobileVehicle.getDeltaMovement().y, mobileVehicle.getDeltaMovement().z) * 72, "km/h")),
                        w / 2 - 140, h / 2, 0x66FF00, false);

                if (mobileVehicle instanceof Ah6Entity ah6Entity) {
                    if (weaponVehicle.getWeaponIndex(0) == 0) {
                        double heat = 1 - ah6Entity.getEntityData().get(HEAT) / 100.0F;
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("20MM CANNON " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : iHelicopterEntity.getAmmoCount(player))), w / 2 - 160, h / 2 - 60, Mth.hsvToRgb((float) heat / 3.745318352059925F, 1.0F, 1.0F), false);
                    } else {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("70MM ROCKET " + iHelicopterEntity.getAmmoCount(player)), w / 2 - 160, h / 2 - 60, 0x66FF00, false);
                    }
                }


                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("FLARE " + iHelicopterEntity.getDecoy()), w / 2 - 160, h / 2 - 50, 0x66FF00, false);

                if (lerpVy * 20 < -24) {
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("SINK RATE，PULL UP!"),
                            w / 2 - 53, h / 2 + 24, -65536, false);
                } else if (((lerpVy * 20 < -10 || (lerpVy * 20 < -1 && length(mobileVehicle.getDeltaMovement().x, mobileVehicle.getDeltaMovement().y, mobileVehicle.getDeltaMovement().z) * 72 > 100)) && height < 36)
                        || (length(mobileVehicle.getDeltaMovement().x, mobileVehicle.getDeltaMovement().y, mobileVehicle.getDeltaMovement().z) * 72 > 40 && blockInWay < 72)) {
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("TERRAIN TERRAIN"),
                            w / 2 - 42, h / 2 + 24, -65536, false);
                }

                if (mobileVehicle.getEnergy() < 0.02 * mobileVehicle.getMaxEnergy()) {
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("NO POWER!"),
                            w / 2 - 144, h / 2 + 14, -65536, false);
                } else if (mobileVehicle.getEnergy() < 0.2 * mobileVehicle.getMaxEnergy()) {
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("LOW POWER"),
                            w / 2 - 144, h / 2 + 14, 0xFF6B00, false);
                }

            }


            float fovAdjust2 = (float) (Minecraft.getInstance().options.fov().get() / 30) - 1;
            double zoom = 0.96 * 3 + 0.06 * fovAdjust2;

            Vec3 lookVec = new Vec3(camera.getLookVector());
            Vec3 pos = iHelicopterEntity.shootPos(partialTick).add(iHelicopterEntity.shootVec(partialTick).scale(192));
            Vec3 lookAngle = lookVec.normalize().scale(pos.distanceTo(cameraPos) * (1 - 1.0 / zoom));

            var cPos = cameraPos.add(lookAngle);

            Vec3 p = RenderHelper.worldToScreen(iHelicopterEntity.shootPos(partialTick).add(mobileVehicle.getViewVector(partialTick).scale(192)), ClientEventHandler.zoomVehicle ? cPos : cameraPos);

            if (p != null) {
                poseStack.pushPose();
                float x = (float) p.x;
                float y = (float) p.y;


                if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/helicopter/crosshair_ind.png"), x - 8, y - 8, 0, 0, 16, 16, 16, 16);
                    renderKillIndicator(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));
                } else if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
                    poseStack.pushPose();
                    poseStack.rotateAround(Axis.ZP.rotationDegrees(iHelicopterEntity.getRotZ(partialTick)), x, y, 0);
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/drone.png"), x - 8, y - 8, 0, 0, 16, 16, 16, 16);
                    renderKillIndicator(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));

                    poseStack.pushPose();

                    poseStack.translate(x, y, 0);
                    poseStack.scale(0.75f, 0.75f, 1);

                    if (mobileVehicle instanceof Ah6Entity ah6Entity) {
                        if (weaponVehicle.getWeaponIndex(0) == 0) {
                            double heat = ah6Entity.getEntityData().get(HEAT) / 100.0F;
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("20MM CANNON " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : iHelicopterEntity.getAmmoCount(player))), 25, -9, Mth.hsvToRgb(0F, (float) heat, 1.0F), false);
                        } else {
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("70MM ROCKET " + iHelicopterEntity.getAmmoCount(player)), 25, -9, -1, false);
                        }
                    }


                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("FLARE " + iHelicopterEntity.getDecoy()), 25, 1, -1, false);
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

    private static void renderDriverAngle(GuiGraphics guiGraphics, Player player, Entity heli, float k, float l, float i, float j, float ticks) {
        float diffY = Mth.wrapDegrees(Mth.lerp(ticks, player.yHeadRotO, player.getYHeadRot()) - Mth.lerp(ticks, heli.yRotO, heli.getYRot())) * 0.35f;
        float diffX = Mth.wrapDegrees(Mth.lerp(ticks, player.xRotO, player.getXRot()) - Mth.lerp(ticks, heli.xRotO, heli.getXRot())) * 0.072f;

        preciseBlit(guiGraphics, Mod.loc("textures/screens/helicopter/heli_driver_angle.png"), k + diffY, l + diffX, 0, 0.0F, i, j, i, j);
    }

    public static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }
}
