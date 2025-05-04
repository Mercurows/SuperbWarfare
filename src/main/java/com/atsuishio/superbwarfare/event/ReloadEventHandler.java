package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.api.event.ReloadEvent;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.tools.GunsTool;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

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

        handleHealClipPre(stack);
        handleKillClipPre(stack);
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

        handleHealClipPost(player, stack);
        handleKillClipPost(stack);
        handleDesperadoPost(stack);
    }

    private static void handleHealClipPre(ItemStack stack) {
        var data = GunData.from(stack);
        final var tag = data.tag();
        int time = GunsTool.getPerkIntTag(tag, "HealClipTime");
        if (time > 0) {
            GunsTool.setPerkIntTag(tag, "HealClipTime", 0);
            GunsTool.setPerkBooleanTag(tag, "HealClip", true);
        } else {
            GunsTool.setPerkBooleanTag(tag, "HealClip", false);
        }
        data.save();
    }

    private static void handleHealClipPost(Player player, ItemStack stack) {
        var data = GunData.from(stack);
        final var tag = data.tag();
        if (!GunsTool.getPerkBooleanTag(tag, "HealClip")) return;

        int healClipLevel = data.perk.getLevel(ModPerks.HEAL_CLIP);
        if (healClipLevel == 0) {
            healClipLevel = 1;
        }

        player.heal(12.0f * (0.8f + 0.2f * healClipLevel));
        List<Player> players = player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(5))
                .stream().filter(p -> p.isAlliedTo(player)).toList();
        int finalHealClipLevel = healClipLevel;
        players.forEach(p -> p.heal(6.0f * (0.8f + 0.2f * finalHealClipLevel)));
    }

    private static void handleKillClipPre(ItemStack stack) {
        var data = GunData.from(stack);
        final var tag = data.tag();
        int time = GunsTool.getPerkIntTag(tag, "KillClipReloadTime");
        if (time > 0) {
            GunsTool.setPerkIntTag(tag, "KillClipReloadTime", 0);
            GunsTool.setPerkBooleanTag(tag, "KillClip", true);
        } else {
            GunsTool.setPerkBooleanTag(tag, "KillClip", false);
        }
        data.save();
    }

    private static void handleKillClipPost(ItemStack stack) {
        var data = GunData.from(stack);
        final var tag = data.tag();
        if (!GunsTool.getPerkBooleanTag(tag, "KillClip")) return;

        int level = data.perk.getLevel(ModPerks.KILL_CLIP);
        GunsTool.setPerkIntTag(tag, "KillClipTime", 90 + 10 * level);
        data.save();
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
