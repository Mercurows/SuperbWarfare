package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.tools.*;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;

import java.util.List;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.HEAT;
import static com.atsuishio.superbwarfare.event.ClientEventHandler.zoomVehicle;

@OnlyIn(Dist.CLIENT)
public class AircraftOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("aircraft_hud");

    private static float lerpVy = 1;
    private static float lerpLock = 1;
    private static float lerpG = 1;

    private static final ResourceLocation FRAME_GREEN = Mod.loc("textures/overlay/frame/frame_green.png");
    private static final ResourceLocation FRAME_TARGET = Mod.loc("textures/overlay/frame/frame_target.png");
    private static final ResourceLocation FRAME_LOCK = Mod.loc("textures/overlay/frame/frame_lock.png");

    private static final ResourceLocation IND_1 = Mod.loc("textures/overlay/vehicle/aircraft/locking_ind1.png");
    private static final ResourceLocation IND_2 = Mod.loc("textures/overlay/vehicle/aircraft/locking_ind2.png");
    private static final ResourceLocation IND_3 = Mod.loc("textures/overlay/vehicle/aircraft/locking_ind3.png");
    private static final ResourceLocation IND_4 = Mod.loc("textures/overlay/vehicle/aircraft/locking_ind4.png");
    private static final ResourceLocation BOMB_SCOPE = Mod.loc("textures/overlay/vehicle/aircraft/bomb_scope.png");
    private static final ResourceLocation BOMB_SCOPE_PITCH = Mod.loc("textures/overlay/vehicle/aircraft/bomb_scope_pitch.png");
    private static final ResourceLocation HUD_BASE_MISSILE = Mod.loc("textures/overlay/vehicle/aircraft/hud_base_missile.png");
    private static final ResourceLocation HUD_BASE = Mod.loc("textures/overlay/vehicle/aircraft/hud_base.png");
    private static final ResourceLocation HUD_LINE = Mod.loc("textures/overlay/vehicle/aircraft/hud_line.png");
    private static final ResourceLocation HUD_IND = Mod.loc("textures/overlay/vehicle/aircraft/hud_ind.png");
    private static final ResourceLocation HUD_BASE2 = Mod.loc("textures/overlay/vehicle/aircraft/hud_base2.png");
    private static final ResourceLocation COMPASS_IND = Mod.loc("textures/overlay/vehicle/aircraft/compass_ind.png");
    private static final ResourceLocation CROSSHAIR_IND = Mod.loc("textures/overlay/vehicle/aircraft/crosshair_ind.png");

    private static final ResourceLocation HELICOPTER_ROLL_IND = Mod.loc("textures/overlay/vehicle/helicopter/roll_ind.png");
    private static final ResourceLocation HELICOPTER_SPEED_FRAME = Mod.loc("textures/overlay/vehicle/helicopter/speed_frame.png");

    private static final ResourceLocation COMPASS = Mod.loc("textures/overlay/vehicle/base/compass.png");
    private static final ResourceLocation CROSSHAIR_3P = Mod.loc("textures/overlay/vehicle/crosshair/third_camera.png");
    private static final ResourceLocation BOMB_RING = Mod.loc("textures/overlay/crosshair/rex_circle.png");

    @Override
    public void render(GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = guiGraphics.pose();
        var screenWidth = guiGraphics.guiWidth();
        var screenHeight = guiGraphics.guiHeight();

        var partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);

        if (player == null) return;

        if (ClientEventHandler.isEditing)
            return;

        if (player.getVehicle() instanceof ArmedVehicleEntity armedVehicle
                && armedVehicle instanceof VehicleEntity vehicle
                && armedVehicle.isDriver(player)
                && player.getVehicle() instanceof WeaponVehicleEntity weaponVehicle
                && vehicle.getVehicleType() == VehicleType.AIRPLANE) {
            poseStack.pushPose();

            int color = vehicle.getHudColor() | 0xFF000000;
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            lerpVy = (float) Mth.lerp(0.021f * partialTick, lerpVy, vehicle.getDeltaMovement().y());
            float diffY = (float) ClientMouseHandler.lerpSpeedX;
            float diffX = (float) ClientMouseHandler.lerpSpeedY;

            Vec3 pos = cameraPos.add(vehicle.getViewVector(partialTick).scale(192));
            Vec3 posCross = vehicle.getShootPos(player, partialTick).add(vehicle.getViewVec(player, partialTick).scale(192));

            Vec3 p = VectorUtil.worldToScreen(pos);
            Vec3 pCross = VectorUtil.worldToScreen(posCross);

            // 投弹准星
            if (vehicle instanceof A10Entity a10Entity && weaponVehicle.getWeaponIndex(0) == 2 && (zoomVehicle || mc.options.getCameraType() != CameraType.FIRST_PERSON)) {
                Vec3 p0 = a10Entity.bombLandingPosO;
                Vec3 p1 = a10Entity.bombLandingPos;
                if (p0 != null && p1 != null) {
                    Vec3 bombCross = p0.lerp(p1, partialTick);
                    pCross = VectorUtil.worldToScreen(bombCross);

                    if (zoomVehicle && VectorUtil.canSee(bombCross)) {
                        float f = (float) Math.min(screenWidth, screenHeight);
                        float f1 = Math.min((float) screenWidth / f, (float) screenHeight / f);
                        int i = Mth.floor(f * f1);
                        int j = Mth.floor(f * f1);

                        float x = (float) pCross.x;
                        float y = (float) pCross.y;


                        poseStack.pushPose();
                        poseStack.translate(x, y, 0);
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("MK82 BOMB " + vehicle.getAmmoCount(player)), 25, -11, 1, false);
                        poseStack.popPose();

                        preciseBlit(guiGraphics, BOMB_SCOPE, x - 1.5f * i, y - 1.5f * j, 0, 0, 3 * i, 3 * j, 3 * i, 3 * j);

                        poseStack.pushPose();
                        poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), x, y, 0);
                        preciseBlit(guiGraphics, BOMB_SCOPE_PITCH, x - 1.5f * i, y - 1.5f * j - 4 * a10Entity.getPitch(partialTick), 0, 0, 3 * i, 3 * j, 3 * i, 3 * j);
                        renderKillIndicator(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));
                        poseStack.popPose();
                        return;
                    }
                }
            }

            poseStack.pushPose();

            if (mc.options.getCameraType() == CameraType.FIRST_PERSON && VectorUtil.canSee(pos)) {
                float x = (float) p.x;
                float y = (float) p.y;

                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                RenderSystem.setShaderColor(1, 1, 1, 1);

                if (vehicle instanceof A10Entity && weaponVehicle.getWeaponIndex(0) == 3) {
                    RenderHelper.preciseBlitWithColor(guiGraphics, HUD_BASE_MISSILE, x - 160, y - 160, 0, 0, 320, 320, 320, 320, color);
                } else {
                    RenderHelper.preciseBlitWithColor(guiGraphics, HUD_BASE, x - 160, y - 160, 0, 0, 320, 320, 320, 320, color);
                }

                //指南针
                RenderHelper.preciseBlitWithColor(guiGraphics, COMPASS, x - 128, y - 122, 128 + ((float) 64 / 45 * vehicle.getYRot()), 0, 256, 16, 512, 16, color);
                RenderHelper.preciseBlitWithColor(guiGraphics, COMPASS_IND, x - 4, y - 130, 0, 0, 8, 8, 8, 8, color);

                //滚转指示
                poseStack.pushPose();
                poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), x, y + 48, 0);
                RenderHelper.preciseBlitWithColor(guiGraphics, HELICOPTER_ROLL_IND, x - 4, y + 144, 0, 0, 8, 8, 8, 8, color);
                poseStack.popPose();

                //时速
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(vehicle.getDeltaMovement().dot(vehicle.getViewVector(1)) * 72)),
                        (int) x - 105, (int) y - 61, color, false);
                //高度
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(vehicle.getY())),
                        (int) x + 111 - 36, (int) y - 61, color, false);
                //框
                RenderHelper.preciseBlitWithColor(guiGraphics, HELICOPTER_SPEED_FRAME, x - 108, y - 64, 0, 0, 36, 12, 36, 12, color);
                RenderHelper.preciseBlitWithColor(guiGraphics, HELICOPTER_SPEED_FRAME, x + 108 - 36, y - 64, 0, 0, 36, 12, 36, 12, color);
                //垂直速度
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.DECIMAL_FORMAT_1ZZ.format(lerpVy * 20)), (int) x - 96, (int) y + 60, color, false);
                //加速度
                lerpG = (float) Mth.lerp(0.1f * partialTick, lerpG, vehicle.acceleration / 9.8);
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("M"), (int) x - 105, (int) y + 70, color, false);
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("0.2"), (int) x - 96, (int) y + 70, color, false);
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("G"), (int) x - 105, (int) y + 78, color, false);
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.DECIMAL_FORMAT_1ZZ.format(lerpG)), (int) x - 96, (int) y + 78, color, false);

                // 热诱弹
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("IR FLARES " + vehicle.getDecoyState()), (int) x + 72, (int) y, color, false);
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("TGT"), (int) x + 76, (int) y + 78, color, false);

                if (vehicle instanceof A10Entity a10Entity) {
                    if (weaponVehicle.getWeaponIndex(0) == 0) {
                        int heat = a10Entity.getEntityData().get(HEAT);
                        String name = "30MM CANNON";
                        int width = Minecraft.getInstance().font.width(name);
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(name), (int) x - width / 2, (int) y + 67, MathTool.getGradientColor(color, 0xFF0000, heat, 2), false);

                        String count = InventoryTool.hasCreativeAmmoBox(player) ? "∞" : String.valueOf(vehicle.getAmmoCount(player));
                        int width2 = Minecraft.getInstance().font.width(count);
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(count), (int) x - width2 / 2, (int) y + 76, MathTool.getGradientColor(color, 0xFF0000, heat, 2), false);
                    } else if (weaponVehicle.getWeaponIndex(0) == 1) {
                        String name = "70MM ROCKET";
                        int width = Minecraft.getInstance().font.width(name);
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(name), (int) x - width / 2, (int) y + 67, color, false);

                        String count = String.valueOf(vehicle.getAmmoCount(player));
                        int width2 = Minecraft.getInstance().font.width(count);
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(count), (int) x - width2 / 2, (int) y + 76, color, false);
                    } else if (weaponVehicle.getWeaponIndex(0) == 2) {
                        String name = "MK82 BOMB";
                        int width = Minecraft.getInstance().font.width(name);
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(name), (int) x - width / 2, (int) y + 67, color, false);

                        String count = String.valueOf(vehicle.getAmmoCount(player));
                        int width2 = Minecraft.getInstance().font.width(count);
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(count), (int) x - width2 / 2, (int) y + 76, color, false);
                    } else if (weaponVehicle.getWeaponIndex(0) == 3) {
                        String name = "AGM-65";
                        int width = Minecraft.getInstance().font.width(name);
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(name), (int) x - width / 2, (int) y + 67, color, false);

                        String count = String.valueOf(vehicle.getAmmoCount(player));
                        int width2 = Minecraft.getInstance().font.width(count);
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(count), (int) x - width2 / 2, (int) y + 76, color, false);
                    }
                }

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
                poseStack.popPose();

                // 能量警告
                if (vehicle.hasEnergyStorage()) {
                    if (vehicle.getEnergy() < 0.02 * vehicle.getMaxEnergy()) {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("NO POWER!"),
                                (int) x - 144, (int) y + 14, -65536, false);
                    } else if (vehicle.getEnergy() < 0.2 * vehicle.getMaxEnergy()) {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("LOW POWER"),
                                (int) x - 144, (int) y + 14, 0xFF6B00, false);
                    }
                }
            }

            // 准星
            poseStack.pushPose();

            if (VectorUtil.canSee(posCross)) {
                float x = (float) pCross.x;
                float y = (float) pCross.y;

                if (mc.options.getCameraType() == CameraType.FIRST_PERSON && !(vehicle instanceof A10Entity a10Entity && a10Entity.getWeaponIndex(0) == 3)) {
                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                    RenderSystem.enableBlend();
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    RenderSystem.setShaderColor(1, 1, 1, 1);

                    RenderHelper.preciseBlitWithColor(guiGraphics, HUD_BASE2, x - 72 + diffY, y - 72 + diffX, 0, 0, 144, 144, 144, 144, color);

                    RenderHelper.preciseBlitWithColor(guiGraphics, CROSSHAIR_IND, x - 16, y - 16, 0, 0, 32, 32, 32, 32, color);
                    renderKillIndicator(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));
                } else if (mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
                    poseStack.pushPose();
                    poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), x, y, 0);
                    poseStack.pushPose();
                    poseStack.translate(x, y, 0);
                    poseStack.scale(0.75f, 0.75f, 1);

                    ResourceLocation cross = CROSSHAIR_3P;
                    float size = 16;

                    if (vehicle instanceof A10Entity a10Entity) {
                        if (weaponVehicle.getWeaponIndex(0) == 0) {
                            double heat = a10Entity.getEntityData().get(HEAT) / 100.0F;
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("30MM CANNON " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : vehicle.getAmmoCount(player))), 25, -9, Mth.hsvToRgb(0F, (float) heat, 1.0F), false);
                        } else if (weaponVehicle.getWeaponIndex(0) == 1) {
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("70MM ROCKET " + vehicle.getAmmoCount(player)), 25, -9, -1, false);
                        } else if (weaponVehicle.getWeaponIndex(0) == 2) {
                            cross = BOMB_RING;
                            size = 24;
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("MK82 BOMB " + vehicle.getAmmoCount(player)), 25, -9, -1, false);
                        } else if (weaponVehicle.getWeaponIndex(0) == 3) {
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("AGM-65 " + vehicle.getAmmoCount(player)), 25, -9, -1, false);
                        }
                    }

                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("IR FLARES " + vehicle.getDecoyState()), 25, 1, -1, false);
                    poseStack.popPose();
                    preciseBlit(guiGraphics, cross, x - 0.5f * size, y - 0.5f * size, 0, 0, size, size, size, size);
                    renderKillIndicator(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));
                    poseStack.popPose();
                }
            }

            poseStack.popPose();

            // A-10的导弹锁定
            if (vehicle instanceof A10Entity a10Entity && a10Entity.getWeaponIndex(0) == 3) {
                Entity targetEntity = EntityFindUtil.findEntity(player.level(), a10Entity.getTargetUuid());
                List<Entity> entities = new SeekTool.Builder(a10Entity)
                        .withinRange(384)
                        .withinAngle(20)
                        .baseFilter()
                        .onGround(10)
                        .sizeBiggerThan(0.9)
                        .smokeFilter()
                        .noVehicle()
                        .noClip()
                        .notFriendly()
                        .build();

                for (var e : entities) {
                    Vec3 pos3 = new Vec3(Mth.lerp(partialTick, e.xo, e.getX()), Mth.lerp(partialTick, e.yo + e.getEyeHeight(), e.getEyeY()), Mth.lerp(partialTick, e.zo, e.getZ()));
                    if (VectorUtil.canSee(pos3)) {
                        Vec3 point = VectorUtil.worldToScreen(pos3);
                        boolean nearest = e == targetEntity;
                        boolean lockOn = a10Entity.locked && nearest;

                        poseStack.pushPose();
                        float x = (float) point.x;
                        float y = (float) point.y;

                        if (lockOn) {
                            RenderHelper.preciseBlit(guiGraphics, FRAME_LOCK, x - 12, y - 12, 24, 24, 0, 0, 24, 24, 24, 24);
                        } else if (nearest) {
                            lerpLock = Mth.lerp(partialTick, lerpLock, 2 * a10Entity.lockTime);
                            float lockTime = Mth.clamp(20 - lerpLock, 0, 20);
                            RenderHelper.preciseBlit(guiGraphics, IND_1, x - 12, y - 12 - lockTime, 24, 24, 0, 0, 24, 24, 24, 24);
                            RenderHelper.preciseBlit(guiGraphics, IND_2, x - 12, y - 12 + lockTime, 24, 24, 0, 0, 24, 24, 24, 24);
                            RenderHelper.preciseBlit(guiGraphics, IND_3, x - 12 - lockTime, y - 12, 24, 24, 0, 0, 24, 24, 24, 24);
                            RenderHelper.preciseBlit(guiGraphics, IND_4, x - 12 + lockTime, y - 12, 24, 24, 0, 0, 24, 24, 24, 24);
                            RenderHelper.preciseBlit(guiGraphics, FRAME_TARGET, x - 12, y - 12, 24, 24, 0, 0, 24, 24, 24, 24);
                        } else {
                            RenderHelper.preciseBlit(guiGraphics, FRAME_GREEN, x - 12, y - 12, 24, 24, 0, 0, 24, 24, 24, 24);
                        }
                        poseStack.popPose();
                    }
                }
            }

            poseStack.popPose();
        }
    }

    private static void renderKillIndicator(GuiGraphics guiGraphics, float posX, float posY) {
        VehicleHudOverlay.renderKillIndicator3P(guiGraphics, posX, posY);
    }

    public static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }
}
