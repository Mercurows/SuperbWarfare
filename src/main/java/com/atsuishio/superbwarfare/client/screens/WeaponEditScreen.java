package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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

    @Override
    protected void init() {
        super.init();
        ClientEventHandler.onOpenEditScreen();
    }

    @Override
    public void onClose() {
        super.onClose();
        ClientEventHandler.onCloseEditScreen();
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == ModKeyMappings.EDIT_MODE.getKey().getValue()) {
            this.onClose();
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }
}
