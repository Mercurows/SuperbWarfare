package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.client.overlay.weapon.AircraftHud;
import com.atsuishio.superbwarfare.client.overlay.weapon.ArtilleryHud;
import com.atsuishio.superbwarfare.client.overlay.weapon.HelicopterHud;
import com.atsuishio.superbwarfare.client.overlay.weapon.LandVehicleHud;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.*;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/**
 * 控制载具主武器的玩家显示的HUD
 */
@OnlyIn(Dist.CLIENT)
public class VehicleMainWeaponHudOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_vehicle_main_weapon_hud";
    public static final String EMPTY = "@Empty";

    private static float lerpLock = 1;

    public static boolean lock = false;

    private static final ResourceLocation FRAME_GREEN = Mod.loc("textures/overlay/frame/frame_green.png");
    private static final ResourceLocation FRAME_TARGET = Mod.loc("textures/overlay/frame/frame_target.png");
    private static final ResourceLocation FRAME_TARGET_TRIANGLE = Mod.loc("textures/overlay/frame/frame_target_triangle.png");
    private static final ResourceLocation FRAME_LOCK = Mod.loc("textures/overlay/frame/frame_lock.png");

    private static final ResourceLocation IND_1 = Mod.loc("textures/overlay/vehicle/aircraft/locking_ind1.png");
    private static final ResourceLocation IND_2 = Mod.loc("textures/overlay/vehicle/aircraft/locking_ind2.png");
    private static final ResourceLocation IND_3 = Mod.loc("textures/overlay/vehicle/aircraft/locking_ind3.png");
    private static final ResourceLocation IND_4 = Mod.loc("textures/overlay/vehicle/aircraft/locking_ind4.png");

    private static final ResourceLocation SHOOT_INDICATOR = Mod.loc("textures/overlay/frame/frame_diamond.png");
    private static final ResourceLocation BLOCK = Mod.loc("textures/overlay/misc/block.png");

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        var player = gui.getMinecraft().player;
        if (player == null) return;
        if (!(player.getVehicle() instanceof VehicleEntity vehicle)) return;
        if (ClientEventHandler.isEditing) return;

        var type = vehicle.computed().mainWeaponHudType;
        if (type.equals(EMPTY)) return;

        var gunData = vehicle.getGunData(player);
        if (gunData == null) return;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        switch (type) {
            case LandVehicleHud.ID ->
                    LandVehicleHud.render(vehicle, player, gui, guiGraphics, partialTick, screenWidth, screenHeight);
            case HelicopterHud.ID ->
                    HelicopterHud.render(vehicle, player, gui, guiGraphics, partialTick, screenWidth, screenHeight);
            case ArtilleryHud.ID ->
                    ArtilleryHud.render(vehicle, player, gui, guiGraphics, partialTick, screenWidth, screenHeight);
            case AircraftHud.ID ->
                    AircraftHud.render(vehicle, player, gui, guiGraphics, partialTick, screenWidth, screenHeight);
        }

        var seekInfo = gunData.compute().seekWeaponInfo;
        if (seekInfo == null) {
            poseStack.popPose();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        int seekTime = seekInfo.seekTime;

        if (seekInfo.onlyLockEntity) {
            Entity targetEntity = ClientEventHandler.lockingEntityVehicle;
            Entity nearestEntity = ClientEventHandler.nearestEntityVehicle;
            Vec3 seekVec = vehicle.getSeekVec(player, partialTick);

            List<Entity> entities = new SeekTool.Builder(vehicle)
                    .withinRange(seekInfo.seekRange)
                    .withinAngle(cameraPos, seekVec, seekInfo.seekAngle)
                    .baseFilter()
                    .heightRange(seekInfo.minTargetHeight, seekInfo.maxTargetHeight)
                    .sizeBiggerThan(seekInfo.minTargetSize)
                    .smokeFilter()
                    .noVehicle()
                    .noClip()
                    .notFriendly()
                    .build();

            Entity decoy = TraceTool.findLookDecoy(player, cameraPos, seekVec, seekInfo.seekRange);

            if (decoy != null && decoy.getType().is(ModTags.EntityTypes.DECOY)) return;

            for (var e : entities) {
                if (e.getType().is(ModTags.EntityTypes.DECOY)) continue;

                Vec3 pos3 = VectorTool.lerpGetEntityBoundingBoxCenter(e, partialTick);
                if (VectorUtil.canSee(pos3) && !seekInfo.onlyLockBlock) {
                    Vec3 point = VectorUtil.worldToScreen(pos3);
                    boolean lockOn = ClientEventHandler.lockOnVehicle && targetEntity != null && e == targetEntity;
                    boolean nearest = e == (ClientEventHandler.seekingEntityVehicle == null ? nearestEntity : ClientEventHandler.seekingEntityVehicle);

                    poseStack.pushPose();
                    float x = (float) point.x;
                    float y = (float) point.y;

                    if (lockOn) {
                        lock = true;
                        RenderHelper.blit(poseStack, FRAME_LOCK, x - 12, y - 12, 0, 0, 24, 24, 24, 24, 1f);
                        nearestEntity = targetEntity;
                        if (seekInfo.calculateTrajectory) {
                            Vec3 shootVector = RangeTool.calculateFiringSolution(vehicle.getShootPos(player, partialTick), VectorTool.lerpGetEntityBoundingBoxCenter(targetEntity, partialTick), targetEntity.getDeltaMovement().scale(1.25), vehicle.getProjectileVelocity(player), vehicle.getProjectileGravity(player)).normalize();
                            Vec3 shootPos = vehicle.getShootPos(player, partialTick).add(shootVector.scale(vehicle.getShootPos(player, partialTick).distanceTo(VectorTool.lerpGetEntityBoundingBoxCenter(targetEntity, partialTick))));
                            Vec3 point0 = VectorUtil.worldToScreen(shootPos);

                            if (VectorUtil.canSee(shootPos)) {
                                poseStack.pushPose();
                                float x0 = (float) point0.x;
                                float y0 = (float) point0.y;

                                Vec3 targetHudPos = new Vec3(x, y, 0);
                                Vec3 shootHudPos = new Vec3(x0, y0, 0);

                                RenderHelper.blit(poseStack, SHOOT_INDICATOR, x0 - 12, y0 - 12, 0, 0, 24, 24, 24, 24, 1f);
                                poseStack.popPose();

                                double dis = targetHudPos.distanceTo(shootHudPos);
                                for (double i = 3; i < dis - 3; i += 3) {
                                    Vec3 toVec = targetHudPos.vectorTo(shootHudPos).normalize();
                                    Vec3 p0 = targetHudPos.add(toVec.scale(i));
                                    RenderHelper.blit(poseStack, BLOCK, (float) (p0.x - 0.5), (float) (p0.y - 0.5), 0, 0, 1, 1, 1, 1, 1f);
                                }
                            }
                        }
                    } else if (nearest && !lock) {
                        lerpLock = Mth.lerp(partialTick, lerpLock, ClientEventHandler.seekingTimeVehicle);
                        float lockTime = Mth.clamp((seekTime - lerpLock) * (20f / seekTime), 0, 20);
                        if (ClientEventHandler.seekingTimeVehicle > 0) {
                            RenderHelper.blit(poseStack, IND_1, x - 12, y - 12 - lockTime, 0, 0, 24, 24, 24, 24, 1f);
                            RenderHelper.blit(poseStack, IND_2, x - 12, y - 12 + lockTime, 0, 0, 24, 24, 24, 24, 1f);
                            RenderHelper.blit(poseStack, IND_3, x - 12 - lockTime, y - 12, 0, 0, 24, 24, 24, 24, 1f);
                            RenderHelper.blit(poseStack, IND_4, x - 12 + lockTime, y - 12, 0, 0, 24, 24, 24, 24, 1f);
                        }

                        if (ClientEventHandler.seekingTimeVehicle == 0) {
                            poseStack.pushPose();
                            poseStack.translate(x, y, 0);
                            String string = "[" + ModKeyMappings.VEHICLE_SEEK.getKey().getDisplayName().getString() + "]";
                            int width = Minecraft.getInstance().font.width(string);
                            guiGraphics.drawString(
                                    mc.font,
                                    string,
                                    -width / 2,
                                    10,
                                    0xFFBD7F,
                                    false
                            );
                            poseStack.popPose();
                        }

                        RenderHelper.blit(poseStack, ClientEventHandler.seekingTimeVehicle > 0 ? FRAME_TARGET : FRAME_TARGET_TRIANGLE, x - 12, y - 12, 0, 0, 24, 24, 24, 24, 1f);
                    } else {
                        RenderHelper.blit(poseStack, FRAME_GREEN, x - 12, y - 12, 0, 0, 24, 24, 24, 24, 1f);
                    }
                    poseStack.popPose();
                }
            }
        } else {
            Vec3 pos = ClientEventHandler.lockingPosVehicle;
            if (pos != null) {
                boolean lockOn = ClientEventHandler.lockOnVehicle;
                Vec3 point = VectorUtil.worldToScreen(pos);
                if (VectorUtil.canSee(pos)) {
                    poseStack.pushPose();
                    float x = (float) point.x;
                    float y = (float) point.y;
                    lerpLock = Mth.lerp(partialTick, lerpLock, ClientEventHandler.seekingTimeVehicle);
                    float lockTime = Mth.clamp((seekTime - lerpLock) * (20f / seekTime), 0, 20);
                    if (ClientEventHandler.seekingTimeVehicle > 0 && !lockOn) {
                        RenderHelper.blit(poseStack, IND_1, x - 12, y - 12 - lockTime, 0, 0, 24, 24, 24, 24, 1f);
                        RenderHelper.blit(poseStack, IND_2, x - 12, y - 12 + lockTime, 0, 0, 24, 24, 24, 24, 1f);
                        RenderHelper.blit(poseStack, IND_3, x - 12 - lockTime, y - 12, 0, 0, 24, 24, 24, 24, 1f);
                        RenderHelper.blit(poseStack, IND_4, x - 12 + lockTime, y - 12, 0, 0, 24, 24, 24, 24, 1f);
                    }

                    if (ClientEventHandler.seekingTimeVehicle == 0) {
                        poseStack.pushPose();
                        poseStack.translate(x, y, 0);
                        String string = "[" + ModKeyMappings.VEHICLE_SEEK.getKey().getDisplayName().getString() + "]";
                        int width = Minecraft.getInstance().font.width(string);
                        guiGraphics.drawString(
                                mc.font,
                                string,
                                -width / 2,
                                10,
                                0xFFBD7F,
                                false
                        );
                        poseStack.popPose();
                    }

                    RenderHelper.blit(poseStack, lockOn ? FRAME_LOCK : FRAME_TARGET, x - 12, y - 12, 0, 0, 24, 24, 24, 24, 1f);
                    poseStack.popPose();
                }
            }
        }

        poseStack.popPose();
    }

    /**
     * 通用渲染方法，在低电量时渲染警告
     */
    public static void renderEnergyInfo(VehicleEntity vehicle, GuiGraphics guiGraphics, int screenWidth, int screenHeight, Font font) {
        if (!vehicle.hasEnergyStorage()) return;

        if (vehicle.getEnergy() < 0.02 * vehicle.getMaxEnergy()) {
            guiGraphics.drawString(font, Component.literal("NO POWER!"), screenWidth / 2 - 144, screenHeight / 2 + 14, -65536, false);
        } else if (vehicle.getEnergy() < 0.2 * vehicle.getMaxEnergy()) {
            guiGraphics.drawString(font, Component.literal("LOW POWER"), screenWidth / 2 - 144, screenHeight / 2 + 14, 0xFF6B00, false);
        }
    }

    // TODO 正确显示文本和备弹数量，正确判断是否应该显示武器名称
    public static void renderWeaponInfoFirst(GuiGraphics guiGraphics, VehicleEntity vehicle, Player player, GunData data, Font font, int screenWidth, int screenHeight, int color) {
        int heat = vehicle.getWeaponHeat(player);
        var component = vehicle.firstPersonAmmoComponent(data, player);

        guiGraphics.drawString(font, component, (screenWidth - font.width(component)) / 2, screenHeight - 65,
                MathTool.getGradientColor(color, 0xFF0000, heat, 2), false);
    }

    public static void renderWeaponInfoThird(GuiGraphics guiGraphics, VehicleEntity vehicle, Player player, GunData data, Font font) {
        if (!(vehicle instanceof WeaponVehicleEntity)) return;

        float heat = vehicle.getWeaponHeat(player) / 100F;
        var component = vehicle.thirdPersonAmmoComponent(data, player);

        guiGraphics.drawString(font, component, 30, -9, Mth.hsvToRgb(0F, heat, 1F), false);
    }

    public static Vec3 getAroundPos(Vec3 direction, Vec3 center, double radius) {
        direction = direction.normalize();

        // 构建垂直正交基
        Vec3 randomPerp = getRandomPerpendicular(direction);
        Vec3 u = randomPerp.normalize();
        Vec3 v = direction.cross(u).normalize();

        double theta = 2 * Math.PI;
        double xOffset = radius * (Math.cos(theta) * u.x + Math.sin(theta) * v.x);
        double yOffset = radius * (Math.cos(theta) * u.y + Math.sin(theta) * v.y);
        double zOffset = radius * (Math.cos(theta) * u.z + Math.sin(theta) * v.z);

        return center.add(xOffset, yOffset, zOffset);
    }

    private static Vec3 getRandomPerpendicular(Vec3 dir) {
        Vec3 candidate1 = new Vec3(dir.y, -dir.x, 0); // 在XY平面垂直
        if (candidate1.lengthSqr() > 1e-4) return candidate1;
        return new Vec3(0, dir.z, -dir.y); // 备用垂直向量
    }
}
