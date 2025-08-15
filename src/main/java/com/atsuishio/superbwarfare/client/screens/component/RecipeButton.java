package com.atsuishio.superbwarfare.client.screens.component;

import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RecipeButton extends Button {

    private final ItemStack stack;
    private boolean isSelected = false;

    public RecipeButton(int x, int y, ItemStack stack, OnPress onPress) {
        super(x, y, 80, 18, Component.literal("114"), onPress, DEFAULT_NARRATION);
        this.stack = stack;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.isSelected) {
            pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 6, 220, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
        } else {
            if (this.isHovered) {
                pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 6, 201, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
            } else {
                pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 6, 182, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
            }
        }

        pGuiGraphics.renderFakeItem(this.stack, this.getX() + 1, this.getY() + 1);
        Component hoverName = this.stack.getHoverName();
        renderScrollingString(pGuiGraphics, Minecraft.getInstance().font, hoverName, this.getX() + 20, this.getY() + 4, this.getX() + 92, this.getY() + 13, 16777215);
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }
}
