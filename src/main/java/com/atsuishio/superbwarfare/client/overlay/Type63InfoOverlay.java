package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity;
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
import org.jetbrains.annotations.NotNull;

import static com.atsuishio.superbwarfare.entity.vehicle.Type63Entity.SHOOT_PITCH;
import static com.atsuishio.superbwarfare.entity.vehicle.Type63Entity.SHOOT_YAW;

public class Type63InfoOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("type_63_info");

    @Override
    public void render(GuiGraphics guiGraphics, @NotNull DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        var screenWidth = guiGraphics.guiWidth();
        var screenHeight = guiGraphics.guiHeight();

        Entity lookingEntity = null;
        if (player != null) {
            lookingEntity = TraceTool.findLookingEntity(player, 6);
        }
        if (lookingEntity instanceof Type63Entity type63Entity) {
            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.pitch")
                            .append(Component.literal(FormatTool.format2D(type63Entity.getEntityData().get(SHOOT_PITCH), "°"))),
                    screenWidth / 2 - 90, screenHeight / 2 - 26, -1, false);
            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.yaw")
                            .append(Component.literal(FormatTool.format2D(type63Entity.getEntityData().get(SHOOT_YAW), "°"))),
                    screenWidth / 2 - 90, screenHeight / 2 - 16, -1, false);
            guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.range")
                            .append(Component.literal(FormatTool.format1D((int) RangeTool.getRange(type63Entity.getEntityData().get(SHOOT_PITCH), 10, 0.05), "m"))),
                    screenWidth / 2 - 90, screenHeight / 2 - 6, -1, false);
        }
    }
}
