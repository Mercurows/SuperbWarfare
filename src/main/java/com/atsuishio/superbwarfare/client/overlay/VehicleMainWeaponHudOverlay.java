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
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.MathTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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

import java.util.List;

/**
 * 控制载具主武器的玩家显示的HUD
 */
@OnlyIn(Dist.CLIENT)
public class VehicleMainWeaponHudOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("vehicle_main_weapon_hud");
    public static final String EMPTY = "@Empty";

    private static float lerpLock = 1;

    private static final ResourceLocation FRAME_GREEN = Mod.loc("textures/overlay/frame/frame_green.png");
    private static final ResourceLocation FRAME_TARGET = Mod.loc("textures/overlay/frame/frame_target.png");
    private static final ResourceLocation FRAME_LOCK = Mod.loc("textures/overlay/frame/frame_lock.png");

    private static final ResourceLocation IND_1 = Mod.loc("textures/overlay/vehicle/aircraft/locking_ind1.png");
    private static final ResourceLocation IND_2 = Mod.loc("textures/overlay/vehicle/aircraft/locking_ind2.png");
    private static final ResourceLocation IND_3 = Mod.loc("textures/overlay/vehicle/aircraft/locking_ind3.png");
    private static final ResourceLocation IND_4 = Mod.loc("textures/overlay/vehicle/aircraft/locking_ind4.png");

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;
        if (!(player.getVehicle() instanceof VehicleEntity vehicle)) return;
        if (ClientEventHandler.isEditing) return;

        var type = vehicle.computed().mainWeaponHudType;
        if (type.equals(EMPTY)) return;

        if (vehicle.getSeatIndex(player) != vehicle.computed().turretControllerIndex) return;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        var partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        switch (type) {
            case LandVehicleHud.ID ->
                    LandVehicleHud.render(vehicle, player, guiGraphics, partialTick, screenWidth, screenHeight);
            case HelicopterHud.ID ->
                    HelicopterHud.render(vehicle, player, guiGraphics, partialTick, screenWidth, screenHeight);
            case ArtilleryHud.ID ->
                    ArtilleryHud.render(vehicle, player, guiGraphics, partialTick, screenWidth, screenHeight);
            case AircraftHud.ID ->
                    AircraftHud.render(vehicle, player, guiGraphics, partialTick, screenWidth, screenHeight);
        }

        var gunData = vehicle.getGunData(player);
        if (gunData == null) return;
        var seekInfo = gunData.compute().seekWeaponInfo;

        if (seekInfo != null) {
            Entity targetEntity = EntityFindUtil.findEntity(player.level(), vehicle.getTargetUuid());
            List<Entity> entities = new SeekTool.Builder(vehicle)
                    .withinRange(seekInfo.seekRange)
                    .withinAngle(vehicle.getSeekVec(player, partialTick), seekInfo.seekAngle)
                    .baseFilter()
                    .onGround(seekInfo.targetHeight)
                    .sizeBiggerThan(seekInfo.minTargetSize)
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
                    boolean lockOn = vehicle.locked && nearest;

                    poseStack.pushPose();
                    float x = (float) point.x;
                    float y = (float) point.y;

                    int seekTime = seekInfo.seekTime;

                    if (lockOn) {
                        RenderHelper.preciseBlitWithColor(guiGraphics, FRAME_LOCK, x - 12, y - 12, 0, 0, 24, 24, 24, 24, 0xFFFFFFFF);
                    } else if (nearest) {
                        lerpLock = Mth.lerp(partialTick, lerpLock, vehicle.lockTime);
                        float lockTime = Mth.clamp((seekTime - lerpLock) * (20f / seekTime), 0, 20);
                        RenderHelper.preciseBlitWithColor(guiGraphics, IND_1, x - 12, y - 12 - lockTime, 0, 0, 24, 24, 24, 24, 0xFFFFFFFF);
                        RenderHelper.preciseBlitWithColor(guiGraphics, IND_2, x - 12, y - 12 + lockTime, 0, 0, 24, 24, 24, 24, 0xFFFFFFFF);
                        RenderHelper.preciseBlitWithColor(guiGraphics, IND_3, x - 12 - lockTime, y - 12, 0, 0, 24, 24, 24, 24, 0xFFFFFFFF);
                        RenderHelper.preciseBlitWithColor(guiGraphics, IND_4, x - 12 + lockTime, y - 12, 0, 0, 24, 24, 24, 24, 0xFFFFFFFF);
                        RenderHelper.preciseBlitWithColor(guiGraphics, FRAME_TARGET, x - 12, y - 12, 0, 0, 24, 24, 24, 24, 0xFFFFFFFF);
                    } else {
                        RenderHelper.preciseBlitWithColor(guiGraphics, FRAME_GREEN, x - 12, y - 12, 0, 0, 24, 24, 24, 24, 0xFFFFFFFF);
                    }
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
        if (!(vehicle instanceof WeaponVehicleEntity)) return;
        if (!vehicle.isAmphibious()) return;

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
}
