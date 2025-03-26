package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.NotNull;

public class ClientCellImageTooltip extends ClientGunImageTooltip {

    public ClientCellImageTooltip(GunImageComponent tooltip) {
        super(tooltip);
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, GuiGraphics guiGraphics) {
        guiGraphics.pose().pushPose();
        if (shouldRenderEnergyTooltip()) {
            renderEnergyTooltip(font, guiGraphics, x, y);
        }

        guiGraphics.pose().popPose();
    }

    protected boolean shouldRenderEnergyTooltip() {
        return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
    }

    protected void renderEnergyTooltip(Font font, GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawString(font, getEnergyComponent(), x, y, 0xFFFFFF);
    }

    protected Component getEnergyComponent() {
        var storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        assert storage != null;
        int energy = storage.getEnergyStored();
        int maxEnergy = storage.getMaxEnergyStored();
        float percentage = Mth.clamp((float) energy / maxEnergy, 0, 1);
        MutableComponent component = Component.literal("");

        ChatFormatting format;
        if (percentage <= .2f) {
            format = ChatFormatting.RED;
        } else if (percentage <= .6f) {
            format = ChatFormatting.YELLOW;
        } else {
            format = ChatFormatting.GREEN;
        }

        int count = (int) (percentage * 50);
        for (int i = 0; i < count; i++) {
            component.append(Component.literal("|").withStyle(format));
        }
        component.append(Component.literal("").withStyle(ChatFormatting.RESET));
        for (int i = 0; i < 50 - count; i++) {
            component.append(Component.literal("|").withStyle(ChatFormatting.GRAY));
        }

        component.append(Component.literal(" " + energy + "/" + maxEnergy + " FE").withStyle(ChatFormatting.GRAY));

        return component;
    }

    @Override
    public int getHeight() {
        int height = super.getHeight();
        if (shouldRenderEnergyTooltip()) height -= 10;
        return height;
    }

    @Override
    public int getWidth(@NotNull Font font) {
        int width = super.getWidth(font);
        if (shouldRenderEnergyTooltip())
            width = Math.max(width, font.width(getEnergyComponent().getVisualOrderText()) + 10);
        return width;
    }
}
