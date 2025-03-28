package com.atsuishio.superbwarfare.compat.jade.elements;

import com.atsuishio.superbwarfare.Mod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.Element;
import snownee.jade.overlay.DisplayHelper;
import snownee.jade.overlay.OverlayRenderer;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;

public class WrenchHealthElement extends Element {
    private final String text;

    public WrenchHealthElement(float maxHealth, float health) {
        this.text = String.format("  %s/%s", DisplayHelper.dfCommas.format(health), DisplayHelper.dfCommas.format(maxHealth));
    }

    @Override
    public Vec2 getSize() {
        Font font = Minecraft.getInstance().font;
        return new Vec2(8F + font.width(this.text), 10.0F);
    }

    private static final ResourceLocation WRENCH_ICON = Mod.loc("textures/screens/vehicle_health.png");


    @Override
    public void render(GuiGraphics guiGraphics, float x, float y, float maxX, float maxY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, OverlayRenderer.alpha);
        RenderSystem.setShaderTexture(0, WRENCH_ICON);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 扳手图标
        preciseBlit(guiGraphics, WRENCH_ICON, x + 2, y, 0, 0, 8, 8, 8, 8);
        // 文字
        DisplayHelper.INSTANCE.drawText(guiGraphics, this.text, x + 6, y, IThemeHelper.get().getNormalColor());

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
