package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * 这个类的作用是在看不见的地方渲染一个第三人称的武器模型，别管为啥这么干
 * 反正删了这个绝对会出事
 */
@OnlyIn(Dist.CLIENT)
public class ItemRendererFixOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_item_renderer_fix";

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Player player = gui.getMinecraft().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(-1145, 0, 0);
        gui.getMinecraft().gameRenderer.itemInHandRenderer.renderItem(player, stack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, guiGraphics.pose(), guiGraphics.bufferSource(), 0);
        guiGraphics.pose().popPose();
    }
}
