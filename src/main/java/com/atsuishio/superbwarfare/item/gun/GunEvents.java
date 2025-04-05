package com.atsuishio.superbwarfare.item.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

@EventBusSubscriber(modid = Mod.MODID)
public class GunEvents {
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        var stack = event.getItemEntity().getItem();
        if (stack.is(ModTags.Items.GUN)) {
            var data = GunData.from(stack);
            final var tag = data.tag();
            tag.putBoolean("draw", true);
            data.save();
        }
    }
}
