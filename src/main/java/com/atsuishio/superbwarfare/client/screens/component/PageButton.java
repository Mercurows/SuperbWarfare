package com.atsuishio.superbwarfare.client.screens.component;

import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class PageButton extends Button {

    private final boolean left;

    public PageButton(int x, int y, boolean left, OnPress onPress) {
        super(x, y, 10, 15, Component.empty(), onPress, DEFAULT_NARRATION);
        this.left = left;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.pose().pushPose();
        RenderSystem.enableDepthTest();

        int vOffset = this.left ? 212 : 196;

        if (!this.active) {
            pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 109, vOffset, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
        } else {
            if (this.isHoveredOrFocused()) {
                pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 98, vOffset, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
            } else {
                pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 87, vOffset, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
            }
        }

        pGuiGraphics.pose().popPose();
    }
}
