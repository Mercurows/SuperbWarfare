package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.ClickHandler;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.AircraftEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
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

    private static final ResourceLocation FRAME = Mod.loc("textures/screens/aircraft/frame.png");
    private static final ResourceLocation FRAME_TARGET = Mod.loc("textures/screens/aircraft/frame_target.png");
    private static final ResourceLocation FRAME_LOCK = Mod.loc("textures/screens/aircraft/frame_lock.png");
    private static final ResourceLocation IND_1 = Mod.loc("textures/screens/aircraft/locking_ind1.png");
    private static final ResourceLocation IND_2 = Mod.loc("textures/screens/aircraft/locking_ind2.png");
    private static final ResourceLocation IND_3 = Mod.loc("textures/screens/aircraft/locking_ind3.png");
    private static final ResourceLocation IND_4 = Mod.loc("textures/screens/aircraft/locking_ind4.png");

    @Override
    public void render(GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack poseStack = guiGraphics.pose();
        Vec3 lookVec = new Vec3(camera.getLookVector());

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

            lerpVy = (float) Mth.lerp(0.021f * partialTick, lerpVy, mobileVehicle.getDeltaMovement().y());
            float diffY = (float) ClientMouseHandler.lerpSpeedX;
            float diffX = (float) ClientMouseHandler.lerpSpeedY;

            float fovAdjust2 = (float) (Minecraft.getInstance().options.fov().get() / 30) - 1;
            double zoom = 3 + 0.06 * fovAdjust2;

            Vec3 pos = cameraPos.add(mobileVehicle.getViewVector(partialTick).scale(192));
            Vec3 lookAngle = lookVec.normalize().scale(pos.distanceTo(cameraPos) * (1 - 1.0 / zoom));

            Vec3 posCross = aircraftEntity.shootPos(partialTick).add(aircraftEntity.shootVec(partialTick).scale(192));
            Vec3 lookAngle2 = lookVec.normalize().scale(posCross.distanceTo(cameraPos) * (1 - 1.0 / zoom));

            var cPos = cameraPos.add(lookAngle);
            var cPos2 = cameraPos.add(lookAngle2);

            Vec3 p = RenderHelper.worldToScreen(pos, zoomVehicle ? cPos : cameraPos);
            Vec3 pCross = RenderHelper.worldToScreen(posCross, zoomVehicle ? cPos2 : cameraPos);

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

                    if (mobileVehicle instanceof A10Entity && weaponVehicle.getWeaponIndex(0) == 3) {
                        preciseBlit(guiGraphics, Mod.loc("textures/screens/aircraft/hud_base_missile.png"), x - 160, y - 160, 0, 0, 320, 320, 320, 320);
                    } else {
                        preciseBlit(guiGraphics, Mod.loc("textures/screens/aircraft/hud_base.png"), x - 160, y - 160, 0, 0, 320, 320, 320, 320);
                    }

                    //指南针
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/compass.png"), x - 128, y - 122, 128 + ((float) 64 / 45 * mobileVehicle.getYRot()), 0, 256, 16, 512, 16);
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/aircraft/compass_ind.png"), x - 4, y - 130, 0, 0, 8, 8, 8, 8);

                    //滚转指示
                    poseStack.pushPose();
                    poseStack.rotateAround(Axis.ZP.rotationDegrees(aircraftEntity.getRotZ(partialTick)), x, y + 48, 0);
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/helicopter/roll_ind.png"), x - 4, y + 144, 0, 0, 8, 8, 8, 8);
                    poseStack.popPose();

                    //时速
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(mobileVehicle.getDeltaMovement().dot(mobileVehicle.getViewVector(1)) * 72)),
                            (int) x - 105, (int) y - 61, 0x66FF00, false);
                    //高度
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(mobileVehicle.getY())),
                            (int) x + 111 - 36, (int) y - 61, 0x66FF00, false);
                    //框
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/helicopter/speed_frame.png"), x - 108, y - 64, 0, 0, 36, 12, 36, 12);
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/helicopter/speed_frame.png"), x + 108 - 36, y - 64, 0, 0, 36, 12, 36, 12);
                    //垂直速度
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.DECIMAL_FORMAT_1ZZ.format(lerpVy * 20)), (int) x - 96, (int) y + 60, 0x66FF00, false);
                    //加速度
                    lerpG = (float) Mth.lerp(0.1f * partialTick, lerpG, mobileVehicle.acceleration / 9.8);
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("M"), (int) x - 105, (int) y + 70, 0x66FF00, false);
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("0.2"), (int) x - 96, (int) y + 70, 0x66FF00, false);
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("G"), (int) x - 105, (int) y + 78, 0x66FF00, false);
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.DECIMAL_FORMAT_1ZZ.format(lerpG)), (int) x - 96, (int) y + 78, 0x66FF00, false);

                    // 热诱弹
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("IR FLARES " + aircraftEntity.getDecoy()), (int) x + 72, (int) y, 0x66FF00, false);

                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("TGT"), (int) x + 76, (int) y + 78, 0x66FF00, false);

                    if (mobileVehicle instanceof A10Entity a10Entity) {
                        if (weaponVehicle.getWeaponIndex(0) == 0) {
                            double heat = 1 - a10Entity.getEntityData().get(HEAT) / 100.0F;
                            String name = "30MM CANNON";
                            int width = Minecraft.getInstance().font.width(name);
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(name), (int) x - width / 2, (int) y + 67, Mth.hsvToRgb((float) heat / 3.745318352059925F, 1.0F, 1.0F), false);

                            String count = InventoryTool.hasCreativeAmmoBox(player) ? "∞" : String.valueOf(aircraftEntity.getAmmoCount(player));
                            int width2 = Minecraft.getInstance().font.width(count);
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(count), (int) x - width2 / 2, (int) y + 76, Mth.hsvToRgb((float) heat / 3.745318352059925F, 1.0F, 1.0F), false);
                        } else if (weaponVehicle.getWeaponIndex(0) == 1) {
                            String name = "70MM ROCKET";
                            int width = Minecraft.getInstance().font.width(name);
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(name), (int) x - width / 2, (int) y + 67, 0x66FF00, false);

                            String count = String.valueOf(aircraftEntity.getAmmoCount(player));
                            int width2 = Minecraft.getInstance().font.width(count);
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(count), (int) x - width2 / 2, (int) y + 76, 0x66FF00, false);
                        } else if (weaponVehicle.getWeaponIndex(0) == 2) {
                            String name = "MK82 BOMB";
                            int width = Minecraft.getInstance().font.width(name);
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(name), (int) x - width / 2, (int) y + 67, 0x66FF00, false);

                            String count = String.valueOf(aircraftEntity.getAmmoCount(player));
                            int width2 = Minecraft.getInstance().font.width(count);
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(count), (int) x - width2 / 2, (int) y + 76, 0x66FF00, false);
                        } else if (weaponVehicle.getWeaponIndex(0) == 3) {
                            String name = "AGM-65";
                            int width = Minecraft.getInstance().font.width(name);
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(name), (int) x - width / 2, (int) y + 67, 0x66FF00, false);

                            String count = String.valueOf(aircraftEntity.getAmmoCount(player));
                            int width2 = Minecraft.getInstance().font.width(count);
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(count), (int) x - width2 / 2, (int) y + 76, 0x66FF00, false);
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

                    poseStack.rotateAround(Axis.ZP.rotationDegrees(-aircraftEntity.getRotZ(partialTick)), x, y, 0);
                    float pitch = aircraftEntity.getRotX(partialTick);
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/aircraft/hud_line.png"), x - 96 + diffY, y - 128, 0, 448 + 4.10625f * pitch, 192, 256, 192, 1152);
                    preciseBlit(guiGraphics, Mod.loc("textures/screens/aircraft/hud_ind.png"), x - 18 + diffY, y - 12, 0, 0, 36, 24, 36, 24);
                    poseStack.popPose();

                    // 能量警告
                    if (mobileVehicle.getEnergy() < 0.02 * mobileVehicle.getMaxEnergy()) {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("NO POWER!"),
                                (int) x - 144, (int) y + 14, -65536, false);
                    } else if (mobileVehicle.getEnergy() < 0.2 * mobileVehicle.getMaxEnergy()) {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("LOW POWER"),
                                (int) x - 144, (int) y + 14, 0xFF6B00, false);
                    }
                }
            }

            // 准星
            if (pCross != null) {
                poseStack.pushPose();
                float x = (float) pCross.x;
                float y = (float) pCross.y + (zoomVehicle ? 15 * (float) (Minecraft.getInstance().options.fov().get() / 70) : 5);

                if (mc.options.getCameraType() == CameraType.FIRST_PERSON && !(mobileVehicle instanceof A10Entity a10Entity && a10Entity.getWeaponIndex(0) == 3)) {
                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                    RenderSystem.enableBlend();
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    RenderSystem.setShaderColor(1, 1, 1, 1);

                    preciseBlit(guiGraphics, Mod.loc("textures/screens/aircraft/hud_base2.png"), x - 72 + diffY, y - 72 + diffX, 0, 0, 144, 144, 144, 144);

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

                    if (mobileVehicle instanceof A10Entity a10Entity) {
                        if (weaponVehicle.getWeaponIndex(0) == 0) {
                            double heat = a10Entity.getEntityData().get(HEAT) / 100.0F;
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("30MM CANNON " + (InventoryTool.hasCreativeAmmoBox(player) ? "∞" : aircraftEntity.getAmmoCount(player))), 25, -9, Mth.hsvToRgb(0F, (float) heat, 1.0F), false);
                        } else if (weaponVehicle.getWeaponIndex(0) == 1) {
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("70MM ROCKET " + aircraftEntity.getAmmoCount(player)), 25, -9, -1, false);
                        } else if (weaponVehicle.getWeaponIndex(0) == 2) {
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("MK82 BOMB " + aircraftEntity.getAmmoCount(player)), 25, -9, -1, false);
                        } else if (weaponVehicle.getWeaponIndex(0) == 3) {
                            guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("AGM-65 " + aircraftEntity.getAmmoCount(player)), 25, -9, -1, false);
                        }
                    }

                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("IR FLARES " + aircraftEntity.getDecoy()), 25, 1, -1, false);
                    poseStack.popPose();
                    poseStack.popPose();
                }
                poseStack.popPose();
            }

            // A-10的导弹锁定
            if (mobileVehicle instanceof A10Entity a10Entity && a10Entity.getWeaponIndex(0) == 3) {
                Entity targetEntity = EntityFindUtil.findEntity(player.level(), a10Entity.getTargetUuid());
                List<Entity> entities = SeekTool.seekCustomSizeEntities(a10Entity, player.level(), 384, 20, 0.9, true);

                for (var e : entities) {
                    Vec3 pos3 = new Vec3(Mth.lerp(partialTick, e.xo, e.getX()), Mth.lerp(partialTick, e.yo + e.getEyeHeight(), e.getEyeY()), Mth.lerp(partialTick, e.zo, e.getZ()));
                    Vec3 lookAngle3 = lookVec.normalize().scale(pos3.distanceTo(cameraPos) * (1 - 1.0 / zoom));
                    var cPos3 = cameraPos.add(lookAngle3);
                    Vec3 point = RenderHelper.worldToScreen(pos3, zoomVehicle ? cPos3 : cameraPos);
                    if (point != null) {
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
                            RenderHelper.preciseBlit(guiGraphics, FRAME, x - 12, y - 12, 24, 24, 0, 0, 24, 24, 24, 24);
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
