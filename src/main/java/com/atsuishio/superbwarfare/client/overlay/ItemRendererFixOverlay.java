package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
public class ItemRendererFixOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("item_renderer_fix");

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player entity = mc.player;

        if (entity != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(-1145.0D, 0.0D, 0.0D);
            mc.gameRenderer.itemInHandRenderer.renderItem(entity, entity.getMainHandItem(),
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, guiGraphics.pose(), guiGraphics.bufferSource(), 0);
            guiGraphics.pose().popPose();
        }
    }
}
