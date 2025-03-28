package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ArmorPlateOverlay {

    private static final ResourceLocation ICON = Mod.loc("textures/screens/armor_plate_icon.png");
    private static final ResourceLocation LEVEL1 = Mod.loc("textures/screens/armor_plate_level1.png");
    private static final ResourceLocation LEVEL2 = Mod.loc("textures/screens/armor_plate_level2.png");
    private static final ResourceLocation LEVEL3 = Mod.loc("textures/screens/armor_plate_level3.png");
    private static final ResourceLocation LEVEL1_FRAME = Mod.loc("textures/screens/armor_plate_level1_frame.png");
    private static final ResourceLocation LEVEL2_FRAME = Mod.loc("textures/screens/armor_plate_level2_frame.png");
    private static final ResourceLocation LEVEL3_FRAME = Mod.loc("textures/screens/armor_plate_level3_frame.png");

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (!DisplayConfig.ARMOR_PLATE_HUD.get()) return;

        var gui = event.getGuiGraphics();
        int h = gui.guiHeight();

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.isSpectator()) return;

        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (stack == ItemStack.EMPTY) return;
        var tag = NBTTool.getTag(stack);
        if (!tag.contains("ArmorPlate")) return;

        double amount = 2 * tag.getDouble("ArmorPlate");

        int armorLevel = 1;
        if (stack.is(ModTags.Items.MILITARY_ARMOR)) {
            armorLevel = 2;
        } else if (stack.is(ModTags.Items.MILITARY_ARMOR_HEAVY)) {
            armorLevel = 3;
        }

        ResourceLocation texture = switch (armorLevel) {
            case 2 -> LEVEL2;
            case 3 -> LEVEL3;
            default -> LEVEL1;
        };
        ResourceLocation frame = switch (armorLevel) {
            case 2 -> LEVEL2_FRAME;
            case 3 -> LEVEL3_FRAME;
            default -> LEVEL1_FRAME;
        };

        int length = armorLevel * 30;

        gui.pose().pushPose();
        // 渲染图标
        gui.blit(ICON, 10, h - 13, 0, 0, 8, 8, 8, 8);

        // 渲染框架
        gui.blit(frame, 20, h - 12, 0, 0, length, 6, length, 6);

        // 渲染盔甲值
        gui.blit(texture, 20, h - 12, 0, 0, (int) amount, 6, length, 6);

        gui.pose().popPose();
    }
}
