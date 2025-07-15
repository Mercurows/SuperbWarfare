package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.Yx100Entity;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Math;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
public class Yx100SwarmDroneHudOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("yx100_swarm_drone_hud");
    private static final ResourceLocation FRAME = Mod.loc("textures/screens/frame/frame.png");
    private static final ResourceLocation FRAME_LOCK = Mod.loc("textures/screens/frame/frame_lock.png");

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        PoseStack poseStack = guiGraphics.pose();

        if (!shouldRenderCrossHair(player)) return;

        Entity cannon = player.getVehicle();
        if (cannon == null) return;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        if (player.getVehicle() instanceof Yx100Entity yx100 && yx100.banHand(player)) {
            if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
                float fovAdjust = (float) 70 / Minecraft.getInstance().options.fov().get();

                float f = (float) Math.min(w, h);
                float f1 = Math.min((float) w / f, (float) h / f) * fovAdjust;
                int i = Mth.floor(f * f1);
                int j = Mth.floor(f * f1);
                int k = (w - i) / 2;
                int l = (h - j) / 2;
                RenderHelper.preciseBlit(guiGraphics, Mod.loc("textures/screens/land/lav_missile_cross.png"), k, l, 0, 0.0F, i, j, i, j);
                VehicleHudOverlay.renderKillIndicator(guiGraphics, w, h);
                Entity naerestEntity = SeekTool.seekLivingEntity(player, player.level(), 384, 6);

                if (naerestEntity != null) {
                    Vec3 pos = new Vec3(Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), naerestEntity.xo, naerestEntity.getX()), Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), naerestEntity.yo + naerestEntity.getEyeHeight(), naerestEntity.getEyeY()), Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true), naerestEntity.zo, naerestEntity.getZ()));

                    Vec3 point = VectorUtil.worldToScreen(pos);

                    poseStack.pushPose();
                    float x = (float) point.x;
                    float y = (float) point.y;

                    RenderHelper.preciseBlit(guiGraphics, FRAME_LOCK, x - 12, y - 12, 24, 24, 0, 0, 24, 24, 24, 24);
                    poseStack.popPose();
                }
            }
        }
    }

    private static boolean shouldRenderCrossHair(Player player) {
        if (player == null) return false;
        return !player.isSpectator()
                && player.getVehicle() instanceof Yx100Entity yx100 && yx100.getNthEntity(2) == player;
    }
}
