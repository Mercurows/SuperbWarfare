package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
        PoseStack poseStack = guiGraphics.pose();
        if (player == null) return;

        ItemStack stack = player.getMainHandItem();
        var tag = NBTTool.getTag(stack);
        if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) return;

        boolean lookAtEntity = false;

        double entityRange = 0;
        Entity lookingEntity = TraceTool.camerafFindLookingEntity(player, cameraPos, 512, deltaTracker.getRealtimeDeltaTicks());

        if (lookingEntity instanceof VehicleEntity) {
            lookAtEntity = true;
            entityRange = player.distanceTo(lookingEntity);
        }

        if (lookAtEntity) {
            poseStack.pushPose();
            poseStack.scale(0.8f, 0.8f, 1);
            if (lookingEntity.getFirstPassenger() instanceof Player passenger) {
                guiGraphics.drawString(Minecraft.getInstance().font,
                        Component.literal(passenger.getDisplayName().getString() + (passenger.getTeam() == null ? "" : " <" + (passenger.getTeam().getName()) + ">")),
                        w / 2 + 90, h / 2 - 4, passenger.getTeamColor(), false);
                guiGraphics.drawString(Minecraft.getInstance().font,
                        Component.literal(lookingEntity.getDisplayName().getString() + " " + FormatTool.format1D(entityRange, "m")),
                        w / 2 + 90, h / 2 + 5, passenger.getTeamColor(), false);
            } else {
                guiGraphics.drawString(Minecraft.getInstance().font,
                        Component.literal(lookingEntity.getDisplayName().getString() + " " + FormatTool.format1D(entityRange, "M")),
                        w / 2 + 90, h / 2 + 5, -1, false);
            }
            poseStack.popPose();
        }
    }
}
