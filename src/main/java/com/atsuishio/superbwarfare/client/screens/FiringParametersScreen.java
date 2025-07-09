package com.atsuishio.superbwarfare.client.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// TODO 完成这个screen
@OnlyIn(Dist.CLIENT)
public class FiringParametersScreen extends Screen {

    public static final Component TITLE = Component.translatable("item.superbwarfare.firing_parameters");
    private final ItemStack stack;

    public FiringParametersScreen(ItemStack stack) {
        super(TITLE);
        this.stack = stack;
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }
}
