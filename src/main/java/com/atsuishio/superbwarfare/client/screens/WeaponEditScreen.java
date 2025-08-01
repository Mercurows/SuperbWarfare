package com.atsuishio.superbwarfare.client.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WeaponEditScreen extends Screen {

    private final ItemStack stack;

    public WeaponEditScreen(ItemStack stack) {
        super(Component.empty());
        this.stack = stack;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.drawString(this.font, this.stack.getHoverName().getString(), 10, 10, 4210752, false);
    }
}
