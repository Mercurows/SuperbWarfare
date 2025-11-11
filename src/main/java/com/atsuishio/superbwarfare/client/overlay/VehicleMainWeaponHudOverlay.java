package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.overlay.weapon.ArtilleryHud;
import com.atsuishio.superbwarfare.client.overlay.weapon.HelicopterHud;
import com.atsuishio.superbwarfare.client.overlay.weapon.LandVehicleHud;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.tools.MathTool;
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
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * 控制载具主武器的玩家显示的HUD
 */
@OnlyIn(Dist.CLIENT)
public class VehicleMainWeaponHudOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("vehicle_main_weapon_hud");
    public static final String EMPTY = "@Empty";

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;
        if (!(player.getVehicle() instanceof VehicleEntity vehicle)) return;
        if (ClientEventHandler.isEditing) return;

        var type = vehicle.computed().mainWeaponHudType;
        if (type.equals(EMPTY)) return;

        if (vehicle.getSeatIndex(player) != vehicle.computed().mainWeaponControllerIndex) return;

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
