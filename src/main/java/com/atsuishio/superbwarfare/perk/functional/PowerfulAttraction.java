package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.DamageTypeTool;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PowerfulAttraction extends Perk {

    public PowerfulAttraction() {
        super("powerful_attraction", Perk.Type.FUNCTIONAL);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        DamageSource source = event.getSource();
        if (source == null) return;
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof Player player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        int level = GunData.from(stack).perk.getLevel(ModPerks.POWERFUL_ATTRACTION);
        if (level > 0 && (DamageTypeTool.isGunDamage(source) || DamageTypeTool.isExplosionDamage(source))) {
            var drops = event.getDrops();
            drops.forEach(itemEntity -> {
                ItemStack item = itemEntity.getItem();
                if (!player.addItem(item.copy())) {
                    player.drop(item, false);
                }
            });
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        Player player = event.getAttackingPlayer();
        if (player == null) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        int level = GunData.from(stack).perk.getLevel(ModPerks.POWERFUL_ATTRACTION);
        if (level > 0) {
            player.giveExperiencePoints((int) (event.getDroppedExperience() * (0.8f + 0.2f * level)));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLootingLevel(LootingLevelEvent event) {
        DamageSource source = event.getDamageSource();
        if (source == null) return;
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof Player player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        int level = GunData.from(stack).perk.getLevel(ModPerks.POWERFUL_ATTRACTION);
        if (level > 0 && (DamageTypeTool.isGunDamage(source) || DamageTypeTool.isExplosionDamage(source))) {
            event.setLootingLevel(level / 4);
        }
    }
}
