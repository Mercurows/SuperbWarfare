package com.atsuishio.superbwarfare.client.overlay.weapon;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay;
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.MathTool;
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
import net.minecraft.sounds.SoundSource;
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
    private static final ResourceLocation FRAME = Mod.loc("textures/overlay/vehicle/land/tv_frame.png");
    private static final ResourceLocation LINE = Mod.loc("textures/overlay/vehicle/land/line.png");

    private static final ResourceLocation COMPASS = Mod.loc("textures/overlay/vehicle/base/compass.png");
    private static final ResourceLocation CROSSHAIR_3P = Mod.loc("textures/overlay/vehicle/crosshair/third_camera.png");

    private static float scopeScale = 1;
    private static float lerpVy = 1;
    private static float lerpPower = 1;

    public static void render(VehicleEntity vehicle, Player player, ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = gui.getMinecraft();
        PoseStack poseStack = guiGraphics.pose();
        int index = vehicle.getSeatIndex(player);
        var data = vehicle.getGunData(index);
        if (data == null) {
            scopeScale = 0.7f;
            return;
        }
        int color = vehicle.getHudColor();

        if (vehicle.getSeatIndex(player) == vehicle.computed().turretControllerIndex && vehicle.hasTurret()) {
            if (ClientEventHandler.zoomVehicle) {
                // 武器名

                VehicleMainWeaponHudOverlay.renderWeaponInfoFirst(guiGraphics, vehicle, player, vehicle.getGunData(player), mc.font, screenWidth, screenHeight, color);

                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                RenderSystem.setShaderColor(1, 1, 1, 1);

                // 指南针
                RenderHelper.blit(poseStack, COMPASS, (float) screenWidth / 2 - 128, 10F, (float) (128 - (64F / 45 * VehicleVecUtils.getYRotFromVector(vehicle.getBarrelVector(partialTick)))), 0, 256, 16, 512, 16, color);
                RenderHelper.blit(poseStack, ROLL_IND, screenWidth / 2f - 8, 30, 0, 0F, 16, 16, 16, 16, color);

                // 电视
                int addW = (screenWidth / screenHeight) * 48;
                int addH = (screenWidth / screenHeight) * 27;
                preciseBlit(guiGraphics, FRAME, (float) -addW / 2, (float) -addH / 2, 10, 0, 0F, screenWidth + addW, screenHeight + addH, screenWidth + addW, screenHeight + addH);
                RenderHelper.blit(poseStack, LINE, screenWidth / 2f - 64, screenHeight - 56, 0, 0F, 128, 1, 128, 1, color);

                // 时速
                guiGraphics.drawString(mc.font, Component.literal(FormatTool.format0D(vehicle.getDeltaMovement().dot(vehicle.getViewVector(partialTick)) * 72, " km/h")),
                        screenWidth / 2 + 160, screenHeight / 2 - 48, color, false);
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(vehicle.getY(), " m")),
                        screenWidth / 2 + 160, screenHeight / 2 - 39, color, false);

                // 低电量警告
                VehicleMainWeaponHudOverlay.renderEnergyInfo(vehicle, guiGraphics, screenWidth, screenHeight, mc.font);

                // 测距
                boolean lookAtEntity = false;

                BlockHitResult result = player.level().clip(new ClipContext(vehicle.getShootPosForHud(player, partialTick), vehicle.getShootPosForHud(player, partialTick).add(vehicle.getShootDirectionForHud(player, partialTick).scale(512)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
                Vec3 hitPos = result.getLocation();

                double blockRange = player.getEyePosition(1).distanceTo(hitPos);
                double entityRange = 0;

                Entity lookingEntity = TraceTool.camerafFindLookingEntity(player, vehicle.getShootPosForHud(player, partialTick), vehicle.getShootDirectionForHud(player, partialTick), 512);
                if (lookingEntity != null) {
                    lookAtEntity = true;
                    entityRange = player.distanceTo(lookingEntity);
                }

                if (lookAtEntity) {
                    int width = Minecraft.getInstance().font.width(FormatTool.format0D(entityRange, " m"));
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(entityRange, " m")), screenWidth / 2 - width / 2, screenHeight - 53, color, false);
                } else {
                    if (blockRange > 500) {
                        int width = Minecraft.getInstance().font.width("---m");
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("---m"), screenWidth / 2 - width / 2, screenHeight - 53, color, false);
                    } else {
                        int width = Minecraft.getInstance().font.width(FormatTool.format0D(blockRange, " m"));
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(blockRange, " m")), screenWidth / 2 - width / 2, screenHeight - 53, color, false);
                    }
                }
            }
        } else {
            poseStack.pushPose();
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

            Vec3 shootPos = vehicle.getShootPosForHud(player, partialTick);

            BlockHitResult result = player.level().clip(new ClipContext(shootPos, shootPos.add(vehicle.getShootDirectionForHud(player, partialTick).scale(512)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            Vec3 hitPos = result.getLocation();

            double dis = shootPos.distanceTo(hitPos);

            Entity lookingEntity = vehicle.getPlayerLookAtEntityOnVehicle(player, 512, partialTick);

            if (lookingEntity != null) {
                dis = shootPos.distanceTo(lookingEntity.position());
            }

            Vec3 pos = shootPos.add(vehicle.getShootDirectionForHud(player, partialTick).scale(dis));
            Vec3 screenPos = VectorUtil.worldToScreen(pos);
            float x = (float) screenPos.x;
            float y = (float) screenPos.y;

            if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle) {
                RenderHelper.blit(poseStack, HELI_BASE, k, l, 0, 0, i, j, i, j, color);

                float diffY = -Mth.lerp(partialTick, vehicle.turretYRotO, vehicle.getTurretYRot()) * 0.3f;
                float diffX = (float) (Mth.wrapDegrees(-VehicleVecUtils.getXRotFromVector(vehicle.getBarrelVector(partialTick)) - Mth.lerp(partialTick, vehicle.xRotO, vehicle.getXRot())) * 0.072f);
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
                double blockInWay = vehicle.position().distanceTo((Vec3.atLowerCornerOf(vehicle.level().clip(new ClipContext(vehicle.position(), vehicle.position().add(vehicle.getDeltaMovement().add(0, 0.06, 0).normalize().scale(100)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, vehicle)).getBlockPos())));

                float power = vehicle.getPower();
                lerpPower = Mth.lerp(0.001f * partialTick, lerpPower, power);
                RenderHelper.blit(poseStack, HELI_POWER, (float) screenWidth / 2 + 130f, ((float) screenHeight / 2 - 64 + 124 - power * 980), 0, 0, 4, power * 980, 4, power * 980, color);

                lerpVy = (float) Mth.lerp(0.021f * partialTick, lerpVy, vehicle.getDeltaMovement().y() * 20);
                RenderHelper.blit(poseStack, HELI_VY_MOVE, (float) screenWidth / 2 + 138, ((float) screenHeight / 2 - 3 - Math.max(lerpVy, -24) * 2.5f), 0, 0, 8, 8, 8, 8, color);

                double speed = vehicle.getDeltaMovement().length() * 72;

                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(lerpVy, "m/s")),
                        screenWidth / 2 + 146, (int) (screenHeight / 2F - 3 - Math.max(lerpVy, -24) * 2.5), (lerpVy < -24 || ((lerpVy < -10 || (lerpVy < -1 && speed > 100)) && height < 36) || (speed > 40 && blockInWay < 72) ? -65536 : color), false);
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(vehicle.getY())),
                        screenWidth / 2 + 104, screenHeight / 2, color, false);
                RenderHelper.blit(poseStack, SPEED_FRAME, (float) screenWidth / 2 - 144, (float) screenHeight / 2 - 6, 0, 0, 50, 18, 50, 18, color);
                guiGraphics.drawString(Minecraft.getInstance().font, Component.literal(FormatTool.format0D(speed, "km/h")),
                        screenWidth / 2 - 140, screenHeight / 2, color, false);

                if (vehicle.hasDecoy()) {
                    if (vehicle.getDecoyState().equals("READY")) {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.flare.ready").append(Component.literal(" [" + ModKeyMappings.RELEASE_DECOY.getKey().getDisplayName().getString() + "]")), screenWidth / 2 - 160, screenHeight / 2 - 50, color, false);
                    } else {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.flare.reloading"), screenWidth / 2 - 160, screenHeight / 2 - 50, 0xFF0000, false);
                    }
                }
                var component = vehicle.firstPersonAmmoComponent(data, player);

                int heat = vehicle.getWeaponHeat(player);
                guiGraphics.drawString(mc.font, component, screenWidth / 2 - 160, screenHeight / 2 - 59,
                        MathTool.getGradientColor(color, 0xFF0000, heat, 2), false);

                if (lerpVy < -18) {
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("SINK RATE，PULL UP!"),
                            screenWidth / 2 - 53, screenHeight / 2 + 24, -65536, false);
                    if (player.tickCount % 30 == 0) {
                        player.level().playLocalSound(player.getOnPos(), ModSounds.PULL_UP.get(), SoundSource.PLAYERS, 3, 1, false);
                    }
                } else if (((lerpVy < -10 || (lerpVy < -1 && speed > 100)) && height < 36) || (speed > 40 && blockInWay < 72)) {
                    guiGraphics.drawString(Minecraft.getInstance().font, Component.literal("TERRAIN TERRAIN"),
                            screenWidth / 2 - 42, screenHeight / 2 + 24, -65536, false);
                    if (player.tickCount % 30 == 0) {
                        player.level().playLocalSound(player.getOnPos(), ModSounds.TERRAIN.get(), SoundSource.PLAYERS, 3, 1, false);
                    }
                }

                VehicleMainWeaponHudOverlay.renderEnergyInfo(vehicle, guiGraphics, screenWidth, screenHeight, mc.font);

                RenderHelper.blit(poseStack, CROSSHAIR_IND, x - 8, y - 8, 0, 0, 16, 16, 16, 16, color);
                VehicleHudOverlay.renderKillIndicatorDynamic(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));
            } else if (VectorUtil.canSee(pos)) {
                poseStack.pushPose();
                poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.getRoll(partialTick)), x, y, 0);
                preciseBlit(guiGraphics, CROSSHAIR_3P, x - 8, y - 8, 0, 0, 16, 16, 16, 16);
                VehicleHudOverlay.renderKillIndicatorDynamic(guiGraphics, x - 7.5f + (float) (2 * (Math.random() - 0.5f)), y - 7.5f + (float) (2 * (Math.random() - 0.5f)));

                poseStack.pushPose();

                poseStack.translate(x, y, 0);
                poseStack.scale(0.75f, 0.75f, 1);

                VehicleMainWeaponHudOverlay.renderWeaponInfoThird(guiGraphics, vehicle, player, data, mc.font);

                if (vehicle.hasDecoy()) {
                    if (vehicle.getDecoyState().equals("READY")) {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.flare.ready").append(Component.literal(" [" + ModKeyMappings.RELEASE_DECOY.getKey().getDisplayName().getString() + "]")), 30, 1, -1, false);
                    } else {
                        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.flare.reloading"), 30, 1, 0xFF0000, false);
                    }
                }

                poseStack.popPose();
                poseStack.popPose();
            }

            poseStack.popPose();
        }


    }

    private static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }
}
