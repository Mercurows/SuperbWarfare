package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.item.ChargingStationBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ClientChargingStationImageTooltip implements ClientTooltipComponent {

    protected final int width;
    protected final int height;
    protected final ItemStack stack;

    public ClientChargingStationImageTooltip(GunImageComponent tooltip) {
        this.width = tooltip.width;
        this.height = tooltip.height;
        this.stack = tooltip.stack;
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, GuiGraphics guiGraphics) {
        guiGraphics.pose().pushPose();
        renderEnergyTooltip(font, guiGraphics, x, y);
        guiGraphics.pose().popPose();
    }

    protected void renderEnergyTooltip(Font font, GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawString(font, getEnergyComponent(), x, y, 0xFFFFFF);
    }

    protected Component getEnergyComponent() {
        int energy = stack.getOrDefault(ModDataComponents.ENERGY.get(), 0);
        int maxEnergy = ChargingStationBlockItem.MAX_ENERGY;
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
        return Math.max(20, this.height) - 10;
    }

    @Override
    public int getWidth(@NotNull Font font) {
        if (Screen.hasShiftDown()) {
            return Math.max(this.width, 20);
        }
        return 20;
    }
}
