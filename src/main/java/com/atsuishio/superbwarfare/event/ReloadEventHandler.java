package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.api.event.ReloadEvent;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.Perk;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class ReloadEventHandler {

    @SubscribeEvent
    public static void onPreReload(ReloadEvent.Pre event) {
        var shooter = event.shooter;
        ItemStack stack = event.stack;
        if (shooter == null
                || !(stack.getItem() instanceof GunItem)
                || shooter.level().isClientSide
        ) return;

        GunData data = GunData.from(stack);
        for (Perk.Type type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                instance.perk().preReload(data, instance, shooter);
            }
        }
    }

    @SubscribeEvent
    public static void onPostReload(ReloadEvent.Post event) {
        var shooter = event.shooter;
        ItemStack stack = event.stack;
        if (shooter == null || !(stack.getItem() instanceof GunItem)) {
            return;
        }

        if (shooter.level().isClientSide) {
            return;
        }

        GunData data = GunData.from(stack);
        for (Perk.Type type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                instance.perk().postReload(data, instance, shooter);
            }
        }
    }
}
