package com.atsuishio.superbwarfare.client.screens.component;

import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ScaleButton extends Button {

    private final int uOffset;
    private final int vOffset;

    public ScaleButton(int x, int y, int uOffset, int vOffset, OnPress onPress) {
        super(x, y, 9, 9, Component.empty(), onPress, DEFAULT_NARRATION);
        this.uOffset = uOffset;
        this.vOffset = vOffset;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.isHovered()) {
            pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), uOffset, vOffset + 10, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
        } else {
            pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), uOffset, vOffset, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
        }
    }
}
