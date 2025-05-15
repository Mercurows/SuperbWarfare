package com.atsuishio.superbwarfare.item.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

@EventBusSubscriber(modid = Mod.MODID)
public class GunEvents {
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        var stack = event.getItemEntity().getItem();
        if (stack.getItem() instanceof GunItem) {
            var data = GunData.from(stack);
            data.draw.set(true);
            data.save();
        }
    }
}
