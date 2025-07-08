package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
public class VehicleTeamOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("vehicle_team");

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!DisplayConfig.VEHICLE_INFO.get()) return;

        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 viewVec = new Vec3(camera.getLookVector());
        PoseStack poseStack = guiGraphics.pose();
        if (player == null) return;

        ItemStack stack = player.getMainHandItem();
        var tag = NBTTool.getTag(stack);
        if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) return;

        boolean lookAtEntity = false;

        double entityRange = 0;
        Entity lookingEntity = TraceTool.camerafFindLookingEntity(player, cameraPos, viewVec, 512);

        if (lookingEntity != null) {
            lookAtEntity = true;
            entityRange = player.distanceTo(lookingEntity);
        }

        if (lookAtEntity && lookingEntity instanceof VehicleEntity vehicle) {

            Vec3 pos = lookingEntity.getBoundingBox().getCenter().add(new Vec3(0, lookingEntity.getBbHeight() / 2 + 1, 0));
            Vec3 point = VectorUtil.worldToScreen(pos, cameraPos);
            if (point == null) return;

            float x = (float) point.x;
            float y = (float) point.y;

            poseStack.pushPose();
            poseStack.translate(x, y - 12, 0);

            float size = (float) Mth.clamp((50 / VectorUtil.fov) * 0.9f * Math.max((512 - entityRange) / 512, 0.1), 0.1, 1);
            poseStack.scale(size, size, size);
            var font = Minecraft.getInstance().font;

            int color = -1;

            if (lookingEntity.getFirstPassenger() instanceof Player player1) {
                color = player1.getTeamColor();
                String info = player1.getDisplayName().getString() + (player1.getTeam() == null ? "" : " <" + (player1.getTeam().getName()) + ">");
                int width = Minecraft.getInstance().font.width(info);
                guiGraphics.drawString(font, Component.literal(info), -width / 2, -13, color, false);
            } else {
                String info = lookingEntity.getDisplayName().getString();
                int width = Minecraft.getInstance().font.width(info);
                guiGraphics.drawString(font, Component.literal(info), -width / 2, -13, color, false);
            }

            String range = FormatTool.format1D(entityRange, "M");
            int width2 = Minecraft.getInstance().font.width(range);
            guiGraphics.drawString(font, Component.literal(range), -width2 / 2, 7, color, false);

            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -40, -2, 40, 2, 0, -16777216);
            RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -40, -2, -40 + 80 * (vehicle.getHealth() / vehicle.getMaxHealth()), 2, 0, -1);

            poseStack.popPose();
        }
    }
}
