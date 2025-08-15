package com.atsuishio.superbwarfare.client.screens.component;

import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class AssembleButton extends Button {

    public AssembleButton(int x, int y, OnPress onPress) {
        super(x, y, 35, 13, Component.empty(), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.pose().pushPose();
        RenderSystem.enableDepthTest();

        if (this.isHoveredOrFocused()) {
            pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 123, 182, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
        } else {
            pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 87, 182, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
        }

        pGuiGraphics.pose().popPose();
    }
}
