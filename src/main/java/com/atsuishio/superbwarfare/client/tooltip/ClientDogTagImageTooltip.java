package com.atsuishio.superbwarfare.client.tooltip;

import com.atsuishio.superbwarfare.client.screens.DogTagEditorScreen;
import com.atsuishio.superbwarfare.client.tooltip.component.DogTagImageComponent;
import com.atsuishio.superbwarfare.item.DogTag;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class ClientDogTagImageTooltip implements ClientTooltipComponent {

    protected final int width;
    protected final int height;
    protected final ItemStack stack;

    public ClientDogTagImageTooltip(DogTagImageComponent tooltip) {
        this.width = tooltip.width;
        this.height = tooltip.height;
        this.stack = tooltip.stack;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void renderImage(Font pFont, int pX, int pY, GuiGraphics pGuiGraphics) {
        short[][] colors = DogTag.getColors(this.stack);
        if (isAllMinusOne(colors)) return;

        pGuiGraphics.pose().pushPose();

        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                if (colors[i][j] == -1) continue;
                var color = ChatFormatting.getById(colors[i][j]);
                pGuiGraphics.fill(5 + pX + i * 4 + 4, 5 + pY + j * 4 + 4, 5 + pX + i * 4, 5 + pY + j * 4,
                        DogTagEditorScreen.getColorFromFormatting(color));
            }
        }

        pGuiGraphics.pose().popPose();
    }

    @Override
    public int getHeight() {
        return !shouldRenderIcon(this.stack) ? 0 : this.height;
    }

    @Override
    public int getWidth(@NotNull Font pFont) {
        return !shouldRenderIcon(this.stack) ? 0 : this.width;
    }

    public static boolean shouldRenderIcon(ItemStack stack) {
        short[][] colors = DogTag.getColors(stack);
        return !isAllMinusOne(colors);
    }

    public static boolean isAllMinusOne(short[][] arr) {
        if (arr == null) {
            return false;
        }

        for (short[] row : arr) {
            if (row == null) {
                return false;
            }
            for (short element : row) {
                if (element != (short) -1) {
                    return false;
                }
            }
        }

        return true;
    }
}
