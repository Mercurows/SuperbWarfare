package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.api.event.ReloadEvent;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.GunsTool;
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

        handleKillingTallyPre(stack);
        handleDesperadoPre(stack);
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

        handleDesperadoPost(stack);
    }

    private static void handleKillingTallyPre(ItemStack stack) {
        var data = GunData.from(stack);
        final var tag = data.tag();
        int level = data.perk.getLevel(ModPerks.KILLING_TALLY);
        if (level == 0) return;

        GunsTool.setPerkIntTag(tag, "KillingTally", 0);
        data.save();
    }

    private static void handleDesperadoPre(ItemStack stack) {
        var data = GunData.from(stack);
        final var tag = data.tag();
        int time = GunsTool.getPerkIntTag(tag, "DesperadoTime");
        if (time > 0) {
            GunsTool.setPerkIntTag(tag, "DesperadoTime", 0);
            GunsTool.setPerkBooleanTag(tag, "Desperado", true);
        } else {
            GunsTool.setPerkBooleanTag(tag, "Desperado", false);
        }
        data.save();
    }

    private static void handleDesperadoPost(ItemStack stack) {
        var data = GunData.from(stack);
        final var tag = data.tag();
        if (!GunsTool.getPerkBooleanTag(tag, "Desperado")) return;

        int level = data.perk.getLevel(ModPerks.DESPERADO);
        GunsTool.setPerkIntTag(tag, "DesperadoTimePost", 110 + level * 10);
        data.save();
    }
}
