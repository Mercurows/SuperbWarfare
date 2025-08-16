package com.atsuishio.superbwarfare.client.screens.component;

import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class CategoryButton extends Button {

    public VehicleAssemblingRecipe.Category category;
    private boolean isSelected = false;

    public CategoryButton(int x, int y, VehicleAssemblingRecipe.Category category, OnPress onPress) {
        super(x, y, 20, 22, Component.empty(), onPress, DEFAULT_NARRATION);
        this.category = category;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.pose().pushPose();
        RenderSystem.enableDepthTest();

        if (this.isSelected) {
            pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 179, 182, 23, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
        } else {
            pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 179, 205, 20, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
        }

        int vOffset = switch (this.category) {
            case LAND -> 182;
            case DEFENSE -> 198;
            case AIRCRAFT -> 214;
            case WATER -> 230;
            case CIVILIAN -> 246;
            default -> 262;
        };
        pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX() + 3, this.getY() + 3, 203, vOffset, 16, 16, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);

        pGuiGraphics.pose().popPose();
    }

    @Override
    public void onPress() {
        this.isSelected = true;
        this.onPress.onPress(this);
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    public void renderTooltips(GuiGraphics pGuiGraphics, int mouseX, int mouseY) {
        if (this.isHovered()) {
            pGuiGraphics.renderTooltip(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.category." + this.category.getName()), mouseX, mouseY);
        }
    }
}
