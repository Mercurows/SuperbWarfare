package com.atsuishio.superbwarfare.client.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ArmRendererFixOverlay {

    @SubscribeEvent
    public static void eventHandler(RenderGuiEvent.Pre event) {
        int w = event.getGuiGraphics().guiWidth();
        int h = event.getGuiGraphics().guiHeight();
        Player entity = Minecraft.getInstance().player;
        if (entity != null) {
            // TODO what is this?
//            InventoryScreen.renderEntityInInventoryFollowsAngle(
//                    event.getGuiGraphics(),
//                    w / 2 - 114514,
//                    h / 2 + 22,
//                    1,
//                    0f,
//                    0,
//                    entity
//            );
        }
    }
}
