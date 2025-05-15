package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.api.event.ReloadEvent;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.Perk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class ReloadEventHandler {

    @SubscribeEvent
    public static void onPreReload(ReloadEvent.Pre event) {
        Player player = event.player;
        ItemStack stack = event.stack;
        if (player == null
                || !(stack.getItem() instanceof GunItem)
                || player.level().isClientSide
        ) return;

        GunData data = GunData.from(stack);
        for (Perk.Type type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                instance.perk().preReload(data, instance, player);
            }
        }
    }

    @SubscribeEvent
    public static void onPostReload(ReloadEvent.Post event) {
        Player player = event.player;
        ItemStack stack = event.stack;
        if (player == null || !(stack.getItem() instanceof GunItem)) {
            return;
        }

        if (player.level().isClientSide) {
            return;
        }

        GunData data = GunData.from(stack);
        for (Perk.Type type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                instance.perk().postReload(data, instance, player);
            }
        }
    }
}
