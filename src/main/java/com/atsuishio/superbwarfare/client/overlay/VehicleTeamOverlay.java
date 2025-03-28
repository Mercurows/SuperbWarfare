package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class VehicleTeamOverlay {

    @SubscribeEvent
    public static void eventHandler(RenderGuiEvent.Pre event) {
        int w = event.getGuiGraphics().guiWidth();
        int h = event.getGuiGraphics().guiHeight();
        Player player = Minecraft.getInstance().player;
        PoseStack poseStack = event.getGuiGraphics().pose();
        if (player == null) return;

        boolean lookAtEntity = false;

        double entityRange = 0;
        Entity lookingEntity = TraceTool.findLookingEntity(player, 520);

        if (lookingEntity instanceof VehicleEntity) {
            lookAtEntity = true;
            entityRange = player.distanceTo(lookingEntity);
        }

        if (lookAtEntity) {
            poseStack.pushPose();
            poseStack.scale(0.8f, 0.8f, 1);
            if (lookingEntity.getFirstPassenger() instanceof Player passenger) {
                event.getGuiGraphics().drawString(Minecraft.getInstance().font,
                        Component.literal(passenger.getDisplayName().getString() + (passenger.getTeam() == null ? "" : " <" + (passenger.getTeam().getName()) + ">")),
                        w / 2 + 90, h / 2 - 4, passenger.getTeamColor(), false);
                event.getGuiGraphics().drawString(Minecraft.getInstance().font,
                        Component.literal(lookingEntity.getDisplayName().getString() + " " + FormatTool.format1D(entityRange, "m")),
                        w / 2 + 90, h / 2 + 5, passenger.getTeamColor(), false);
            } else {
                event.getGuiGraphics().drawString(Minecraft.getInstance().font,
                        Component.literal(lookingEntity.getDisplayName().getString() + " " + FormatTool.format1D(entityRange, "M")),
                        w / 2 + 90, h / 2 + 5, -1, false);
            }
            poseStack.popPose();
        }
    }
}
