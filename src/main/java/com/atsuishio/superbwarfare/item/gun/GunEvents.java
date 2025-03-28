package com.atsuishio.superbwarfare.item.gun;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

@EventBusSubscriber(modid = ModUtils.MODID)
public class GunEvents {
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        var stack = event.getItemEntity().getItem();
        if (stack.is(ModTags.Items.GUN)) {
            NBTTool.getTag(stack).putBoolean("draw", true);
            NBTTool.getTag(stack).putBoolean("init", true);
        }
    }
}
