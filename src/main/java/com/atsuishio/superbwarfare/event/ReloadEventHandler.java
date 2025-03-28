package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.event.events.ReloadEvent;
import com.atsuishio.superbwarfare.init.ModTags;
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
        if (player == null || !stack.is(ModTags.Items.GUN)) {
            return;
        }

        if (player.level().isClientSide) {
            return;
        }

        handleHealClipPre(stack);
        handleKillClipPre(stack);
        handleKillingTallyPre(stack);
        handleDesperadoPre(stack);
    }

    @SubscribeEvent
    public static void onPostReload(ReloadEvent.Post event) {
        Player player = event.player;
        ItemStack stack = event.stack;
        if (player == null || !stack.is(ModTags.Items.GUN)) {
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
        int time = GunsTool.getPerkIntTag(stack, "HealClipTime");
        if (time > 0) {
            GunsTool.setPerkIntTag(stack, "HealClipTime", 0);
            GunsTool.setPerkBooleanTag(stack, "HealClip", true);
        } else {
            GunsTool.setPerkBooleanTag(stack, "HealClip", false);
        }
    }

    private static void handleHealClipPost(Player player, ItemStack stack) {
        if (!GunsTool.getPerkBooleanTag(stack, "HealClip")) {
            return;
        }

        // TODO perk
//        int healClipLevel = PerkHelper.getItemPerkLevel(ModPerks.HEAL_CLIP.get(), stack);
//        if (healClipLevel == 0) {
//            healClipLevel = 1;
//        }
//
//        player.heal(12.0f * (0.8f + 0.2f * healClipLevel));
//        List<Player> players = player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(5))
//                .stream().filter(p -> p.isAlliedTo(player)).toList();
//        int finalHealClipLevel = healClipLevel;
//        players.forEach(p -> p.heal(6.0f * (0.8f + 0.2f * finalHealClipLevel)));
    }

    private static void handleKillClipPre(ItemStack stack) {
        int time = GunsTool.getPerkIntTag(stack, "KillClipReloadTime");
        if (time > 0) {
            GunsTool.setPerkIntTag(stack, "KillClipReloadTime", 0);
            GunsTool.setPerkBooleanTag(stack, "KillClip", true);
        } else {
            GunsTool.setPerkBooleanTag(stack, "KillClip", false);
        }
    }

    private static void handleKillClipPost(ItemStack stack) {
        if (!GunsTool.getPerkBooleanTag(stack, "KillClip")) {
            return;
        }

        // TODO perk
//        int level = PerkHelper.getItemPerkLevel(ModPerks.KILL_CLIP.get(), stack);
//        GunsTool.setPerkIntTag(stack, "KillClipTime", 90 + 10 * level);
    }

    private static void handleKillingTallyPre(ItemStack stack) {
        // TODO perk
//        int level = PerkHelper.getItemPerkLevel(ModPerks.KILLING_TALLY.get(), stack);
//        if (level == 0) {
//            return;
//        }
//
//        GunsTool.setPerkIntTag(stack, "KillingTally", 0);
    }

    private static void handleDesperadoPre(ItemStack stack) {
        int time = GunsTool.getPerkIntTag(stack, "DesperadoTime");
        if (time > 0) {
            GunsTool.setPerkIntTag(stack, "DesperadoTime", 0);
            GunsTool.setPerkBooleanTag(stack, "Desperado", true);
        } else {
            GunsTool.setPerkBooleanTag(stack, "Desperado", false);
        }
    }

    private static void handleDesperadoPost(ItemStack stack) {
        // TODO perk
//        if (!GunsTool.getPerkBooleanTag(stack, "Desperado")) {
//            return;
//        }
//
//        int level = PerkHelper.getItemPerkLevel(ModPerks.DESPERADO.get(), stack);
//        GunsTool.setPerkIntTag(stack, "DesperadoTimePost", 110 + level * 10);
    }
}
