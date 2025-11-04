package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
public class ArmorPlateOverlay implements LayeredDraw.Layer {
    public static final ResourceLocation ID = Mod.loc("armor_plate");

    private static final ResourceLocation ICON = Mod.loc("textures/overlay/armor_plate/icon.png");
    private static final ResourceLocation BAR_1 = Mod.loc("textures/overlay/armor_plate/bar_1.png");
    private static final ResourceLocation BAR_2 = Mod.loc("textures/overlay/armor_plate/bar_2.png");
    private static final ResourceLocation BAR_3 = Mod.loc("textures/overlay/armor_plate/bar_3.png");
    private static final ResourceLocation BAR_FRAME_1 = Mod.loc("textures/overlay/armor_plate/bar_frame_1.png");
    private static final ResourceLocation BAR_FRAME_2 = Mod.loc("textures/overlay/armor_plate/bar_frame_2.png");
    private static final ResourceLocation BAR_FRAME_3 = Mod.loc("textures/overlay/armor_plate/bar_frame_3.png");

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!DisplayConfig.ARMOR_PLATE_HUD.get()) return;

        int h = guiGraphics.guiHeight();

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.isSpectator()) return;

        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (stack == ItemStack.EMPTY) return;
        var tag = NBTTool.getTag(stack);
        if (!tag.contains("ArmorPlate")) return;

        int armorLevel = MiscConfig.DEFAULT_ARMOR_LEVEL.get();
        if (stack.is(ModTags.Items.MILITARY_ARMOR)) {
            armorLevel = MiscConfig.MILITARY_ARMOR_LEVEL.get();
        } else if (stack.is(ModTags.Items.MILITARY_ARMOR_HEAVY)) {
            armorLevel = MiscConfig.HEAVY_MILITARY_ARMOR_LEVEL.get();
        }

        var max = armorLevel * MiscConfig.ARMOR_POINT_PER_LEVEL.get();
        double amount = 60 * (NBTTool.getTag(stack).getDouble("ArmorPlate") / max);

        ResourceLocation texture = switch (armorLevel) {
            case 2 -> BAR_2;
            case 3 -> BAR_3;
            default -> BAR_1;
        };
        ResourceLocation frame = switch (armorLevel) {
            case 2 -> BAR_FRAME_2;
            case 3 -> BAR_FRAME_3;
            default -> BAR_FRAME_1;
        };

        guiGraphics.pose().pushPose();
        // 渲染图标
        guiGraphics.blit(ICON, 10, h - 13, 0, 0, 8, 8, 8, 8);

        // 渲染框架
        guiGraphics.blit(frame, 20, h - 12, 0, 0, 60, 6, 60, 6);

        // 渲染盔甲值
        guiGraphics.blit(texture, 20, h - 12, 0, 0, (int) amount, 6, 60, 6);

        guiGraphics.pose().popPose();
    }
}
