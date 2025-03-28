package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.client.gui.RangeHelper;
import com.atsuishio.superbwarfare.entity.MortarEntity;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class MortarInfoOverlay {

    @SubscribeEvent
    public static void eventHandler(RenderGuiEvent.Pre event) {
        int w = event.getGuiGraphics().guiWidth();
        int h = event.getGuiGraphics().guiHeight();
        Player player = Minecraft.getInstance().player;
        Entity lookingEntity = null;
        if (player != null) {
            lookingEntity = TraceTool.findLookingEntity(player, 6);
        }
        if (lookingEntity instanceof MortarEntity mortar) {
            event.getGuiGraphics().drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.pitch")
                            .append(Component.literal(FormatTool.format1D(-mortar.getXRot(), "°"))),
                    w / 2 - 90, h / 2 - 26, -1, false);
            event.getGuiGraphics().drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.yaw")
                            .append(Component.literal(FormatTool.format1D(mortar.getYRot(), "°"))),
                    w / 2 - 90, h / 2 - 16, -1, false);
            event.getGuiGraphics().drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.range")
                            .append(Component.literal(FormatTool.format1D((int) RangeHelper.getRange(-mortar.getXRot()), "m"))),
                    w / 2 - 90, h / 2 - 6, -1, false);
        }
    }
}
