package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * 这个类的作用是在看不见的地方渲染一个第三人称的武器模型，别管为啥这么干
 * 反正删了这个绝对会出事
 */
@OnlyIn(Dist.CLIENT)
public class ItemRendererFixOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("item_renderer_fix");

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(-1145.0D, 0.0D, 0.0D);
        mc.gameRenderer.itemInHandRenderer.renderItem(player, stack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, guiGraphics.pose(), guiGraphics.bufferSource(), 0);
        guiGraphics.pose().popPose();
    }
}
