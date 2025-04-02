package com.atsuishio.superbwarfare.item.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

@EventBusSubscriber(modid = Mod.MODID)
public class GunEvents {
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        var stack = event.getItemEntity().getItem();
        if (stack.is(ModTags.Items.GUN)) {
            final var tag = NBTTool.getTag(stack);
            tag.putBoolean("draw", true);
            tag.putBoolean("init", false);
            NBTTool.saveTag(stack, tag);
        }
    }
}
