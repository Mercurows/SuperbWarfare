package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class HandsomeFrameOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_handsome_frame";

    private static final ResourceLocation FRAME = Mod.loc("textures/overlay/frame/frame.png");
    private static final ResourceLocation FRAME_WEAK = Mod.loc("textures/overlay/frame/frame_weak.png");
    private static final ResourceLocation FRAME_TARGET = Mod.loc("textures/overlay/frame/frame_target_triangle.png");
    private static final ResourceLocation FRAME_LOCK = Mod.loc("textures/overlay/frame/frame_lock.png");

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Player player = gui.getMinecraft().player;
        PoseStack poseStack = guiGraphics.pose();

        if (player == null) return;
        if (player.isSpectator()) return;

        ItemStack stack = player.getMainHandItem();

        if (ClientEventHandler.isEditing)
            return;
        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.banHand(player))
            return;

        if (stack.getItem() instanceof GunItem && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            int level = GunData.from(stack).perk.getLevel(ModPerks.INTELLIGENT_CHIP);
            if (level == 0) return;

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            List<Entity> allEntities = SeekTool.seekLivingEntitiesThroughWall(player, 32 + 8 * (level - 1), 30);
            List<Entity> visibleEntities = SeekTool.seekLivingEntities(player, 32 + 8 * (level - 1), 30);

            Entity nearestEntity = SeekTool.seekLivingEntity(player, 32 + 8 * (level - 1), 30);
            Entity targetEntity = ClientEventHandler.lockedEntity;

            for (var e : allEntities) {
                Vec3 pos = new Vec3(Mth.lerp(partialTick, e.xo, e.getX()), Mth.lerp(partialTick, e.yo + e.getEyeHeight(), e.getEyeY()), Mth.lerp(partialTick, e.zo, e.getZ()));
                Vec3 point = VectorUtil.worldToScreen(pos);

                boolean lockOn = e == targetEntity;
                boolean isNearestEntity = e == nearestEntity;

                poseStack.pushPose();
                float x = (float) point.x;
                float y = (float) point.y;

                var canBeSeen = visibleEntities.contains(e);

                ResourceLocation icon;
                if (lockOn) {
                    icon = FRAME_LOCK;
                } else if (canBeSeen) {
                    if (isNearestEntity) {
                        icon = FRAME_TARGET;
                    } else {
                        icon = FRAME;
                    }
                } else {
                    icon = FRAME_WEAK;
                }

                RenderHelper.blit(poseStack, icon, x - 12, y - 12, 0, 0, 24, 24, 24, 24, 1f);
                poseStack.popPose();
            }
        }
    }
}
