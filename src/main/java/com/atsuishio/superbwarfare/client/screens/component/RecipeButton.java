package com.atsuishio.superbwarfare.client.screens.component;

import com.atsuishio.superbwarfare.block.ContainerBlock;
import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen;
import com.atsuishio.superbwarfare.init.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class RecipeButton extends Button {

    private final ItemStack stack;
    private boolean isSelected = false;

    public RecipeButton(int x, int y, ItemStack stack, OnPress onPress) {
        super(x, y, 80, 18, Component.empty(), onPress, DEFAULT_NARRATION);
        this.stack = stack;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        pGuiGraphics.pose().pushPose();
        RenderSystem.enableDepthTest();

        if (this.isSelected) {
            pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 6, 220, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
        } else {
            if (this.isHoveredOrFocused()) {
                pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 6, 201, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
            } else {
                pGuiGraphics.blit(VehicleAssemblingScreen.TEXTURE, this.getX(), this.getY(), 6, 182, this.width, this.height, VehicleAssemblingScreen.IMAGE_SIZE, VehicleAssemblingScreen.IMAGE_SIZE);
            }
        }

        pGuiGraphics.renderFakeItem(this.stack, this.getX() + 1, this.getY() + 1);
        Component hoverName;
        if (this.stack.is(ModItems.CONTAINER.get())) {
            CompoundTag tag = BlockItem.getBlockEntityData(this.stack);
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
}
