package com.atsuishio.superbwarfare.client.screens.component;

import com.atsuishio.superbwarfare.block.ContainerBlock;
import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen;
import com.atsuishio.superbwarfare.init.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RecipeButton extends Button {

    private final ItemStack stack;
    private boolean isSelected = false;

    public RecipeButton(int x, int y, ItemStack stack, OnPress onPress) {
        super(x, y, 80, 18, Component.empty(), onPress, DEFAULT_NARRATION);
        this.stack = stack;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.pose().pushPose();
        RenderSystem.enableDepthTest();

        if (this.isSelected) {
            if (this.isHoveredOrFocused()) {
                pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 6, 239, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
            } else {
                pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 6, 220, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
            }
        } else {
            if (this.isHoveredOrFocused()) {
                pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 6, 201, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
            } else {
                pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 6, 182, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
            }
        }

        pGuiGraphics.renderItem(this.stack, this.getX() + 2, this.getY() + 1);
        Component hoverName;
        if (this.stack.is(ModItems.CONTAINER.get())) {
            var data = this.stack.get(DataComponents.BLOCK_ENTITY_DATA);
            var tag = data != null ? data.copyTag() : null;
            if (tag != null && tag.contains("EntityType")) {
                String key = ContainerBlock.getEntityTranslationKey(tag.getString("EntityType"));
                hoverName = Component.translatable(key == null ? "des.superbwarfare.container.empty" : key);
            } else {
                hoverName = this.stack.getHoverName();
            }
        } else {
            hoverName = this.stack.getHoverName();
        }
        renderScrollingString(pGuiGraphics, Minecraft.getInstance().font, hoverName, this.getX() + 20, this.getY() + 4, this.getX() + 78, this.getY() + 13, 16777215);
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
        if (this.isHoveredOrFocused() && !this.stack.isEmpty()) {
            if (mouseX > this.getX() + 1 && mouseY > this.getY() + 1 && mouseX < this.getX() + this.width - 1 && mouseY < this.getY() + this.height - 1) {
                pGuiGraphics.renderTooltip(Minecraft.getInstance().font, this.stack, mouseX, mouseY);
            }
        }
    }
}
