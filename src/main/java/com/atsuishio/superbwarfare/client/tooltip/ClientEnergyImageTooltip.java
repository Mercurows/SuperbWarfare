package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.NotNull;

public class ClientEnergyImageTooltip extends ClientGunImageTooltip {

    public ClientEnergyImageTooltip(GunImageComponent tooltip) {
        super(tooltip);
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, GuiGraphics guiGraphics) {
        guiGraphics.pose().pushPose();

        renderDamageAndRpmTooltip(font, guiGraphics, x, y);
        renderLevelAndUpgradePointTooltip(font, guiGraphics, x, y + 10);

        int yo = 20;
        if (shouldRenderBypassAndHeadshotTooltip(stack)) {
            renderBypassAndHeadshotTooltip(font, guiGraphics, x, y + yo);
            yo += 10;
        }

        if (shouldRenderEnergyTooltip()) {
            yo += 10;
            renderEnergyTooltip(font, guiGraphics, x, y + yo);
            yo += 10;
        }

        if (shouldRenderEditTooltip()) {
            renderWeaponEditTooltip(font, guiGraphics, x, y + yo);
            yo += 20;
        }

        if (shouldRenderPerks()) {
            if (!Screen.hasShiftDown()) {
                renderPerksShortcut(font, guiGraphics, x, y + yo);
            } else {
                renderPerks(font, guiGraphics, x, y + yo);
            }
        }

        guiGraphics.pose().popPose();
    }

    protected boolean shouldRenderEnergyTooltip() {
        return stack.getCapability(ForgeCapabilities.ENERGY).isPresent() && stack.getCapability(ForgeCapabilities.ENERGY).resolve().isPresent();
    }

    protected void renderEnergyTooltip(Font font, GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawString(font, getEnergyComponent(), x, y, 0xFFFFFF);
    }

    protected Component getEnergyComponent() {
        assert stack.getCapability(ForgeCapabilities.ENERGY).resolve().isPresent();
        var storage = stack.getCapability(ForgeCapabilities.ENERGY).resolve().get();
        int energy = storage.getEnergyStored();
        int maxEnergy = storage.getMaxEnergyStored();
        float percentage = Mth.clamp((float) energy / maxEnergy, 0, 1);
        MutableComponent component = Component.empty();

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
        component.append(Component.empty().withStyle(ChatFormatting.RESET));
        for (int i = 0; i < 50 - count; i++) {
            component.append(Component.literal("|").withStyle(ChatFormatting.GRAY));
        }

        component.append(Component.literal(" " + energy + "/" + maxEnergy + " FE").withStyle(ChatFormatting.GRAY));

        return component;
    }

    @Override
    public int getHeight() {
        int height = super.getHeight();
        if (shouldRenderEnergyTooltip()) height += 20;
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
