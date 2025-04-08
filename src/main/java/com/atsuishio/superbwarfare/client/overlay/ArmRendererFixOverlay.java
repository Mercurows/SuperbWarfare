package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
public class ArmRendererFixOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("arm_renderer_fix");

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();
        Player entity = Minecraft.getInstance().player;
        if (entity != null) {
            // TODO what is this?
//            InventoryScreen.renderEntityInInventoryFollowsAngle(
//                    guiGraphics,
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
