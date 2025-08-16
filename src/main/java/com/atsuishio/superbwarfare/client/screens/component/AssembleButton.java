package com.atsuishio.superbwarfare.client.screens.component;

import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class AssembleButton extends Button {

    public AssembleButton(int x, int y, OnPress onPress) {
        super(x, y, 45, 13, Component.empty(), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.pose().pushPose();
        RenderSystem.enableDepthTest();

        if (this.isHovered()) {
            pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 306, 196, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
        } else {
            pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 306, 182, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
        }

        Component name = Component.translatable("container.superbwarfare.vehicle_assembling_table.assemble");
        pGuiGraphics.drawString(Minecraft.getInstance().font, name, this.getX() + this.width / 2 - Minecraft.getInstance().font.width(name) / 2, this.getY() + 3, -1);

        pGuiGraphics.pose().popPose();
    }
}
