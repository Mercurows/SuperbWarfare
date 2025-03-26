package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.tooltip.component.GunImageComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

public class ClientChargingStationImageTooltip extends ClientGunImageTooltip {

    public ClientChargingStationImageTooltip(GunImageComponent tooltip) {
        super(tooltip);
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
        // TODO 电量渲染
//        CompoundTag tag = BlockItem.getBlockEntityData(stack);
//        int energy = tag == null ? 0 : tag.getInt("Energy");
//        int maxEnergy = ChargingStationBlockItem.MAX_ENERGY;
//        float percentage = Mth.clamp((float) energy / maxEnergy, 0, 1);
        MutableComponent component = Component.literal("");

        ChatFormatting format;
//        if (percentage <= .2f) {
//            format = ChatFormatting.RED;
//        } else if (percentage <= .6f) {
//            format = ChatFormatting.YELLOW;
//        } else {
//            format = ChatFormatting.GREEN;
//        }
//
//        int count = (int) (percentage * 50);
//        for (int i = 0; i < count; i++) {
//            component.append(Component.literal("|").withStyle(format));
//        }
//        component.append(Component.literal("").withStyle(ChatFormatting.RESET));
//        for (int i = 0; i < 50 - count; i++) {
//            component.append(Component.literal("|").withStyle(ChatFormatting.GRAY));
//        }
//
//        component.append(Component.literal(" " + energy + "/" + maxEnergy + " FE").withStyle(ChatFormatting.GRAY));

        return component;
    }

    @Override
    public int getHeight() {
        return super.getHeight() - 10;
    }

    @Override
    public int getWidth(@NotNull Font font) {
        return Math.max(super.getWidth(font), font.width(getEnergyComponent().getVisualOrderText()) + 10);
    }
}
