package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.tools.FormatTool;
import com.atsuishio.superbwarfare.tools.RangeTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
public class MortarInfoOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("mortar_info");

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        int w = guiGraphics.guiWidth();
        int h = guiGraphics.guiHeight();
        Player player = Minecraft.getInstance().player;
        Entity lookingEntity = null;
        if (player != null) {
            lookingEntity = TraceTool.findLookingEntity(player, 6);
        }
        if (lookingEntity instanceof MortarEntity mortar) {
            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.pitch")
                            .append(Component.literal(FormatTool.format1D(-mortar.getXRot(), "°"))),
                    w / 2 - 90, h / 2 - 26, -1, false);
            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.yaw")
                            .append(Component.literal(FormatTool.format1D(mortar.getYRot(), "°"))),
                    w / 2 - 90, h / 2 - 16, -1, false);
            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.range")
                            .append(Component.literal(FormatTool.format1D((int) RangeTool.getRange(-mortar.getXRot(), 11.4, 0.146), "m"))),
                    w / 2 - 90, h / 2 - 6, -1, false);
        }
    }
}
