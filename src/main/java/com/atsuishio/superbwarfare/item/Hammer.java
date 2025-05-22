package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class Hammer extends SwordItem {

    public Hammer() {
        super(Tiers.IRON, new Item.Properties()
                .durability(400)
                .attributes(SwordItem.createAttributes(Tiers.IRON, 9, -3.2f))
        );
    }

    @Override
    public boolean hasCraftingRemainingItem(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public @NotNull ItemStack getCraftingRemainingItem(ItemStack itemstack) {
        ItemStack stack = new ItemStack(this);
        stack.setDamageValue(itemstack.getDamageValue() + 1);
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public boolean isRepairable(@NotNull ItemStack itemstack) {
        return true;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        attacker.level().playSound(null, target.getOnPos(), ModSounds.MELEE_HIT.get(), SoundSource.PLAYERS, 1, (float) ((2 * org.joml.Math.random() - 1) * 0.1f + 1.0f));
        return super.hurtEnemy(stack, target, attacker);
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        var item = event.getCrafting();
        var container = event.getInventory();
        var player = event.getEntity();

        if (player.level().isClientSide) return;

        if (item.is(ModItems.HAMMER.get())) {
            int count = 0;
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (container.getItem(i).is(ModItems.HAMMER.get())) count++;
            }
            if (count == 2) {
                container.clearContent();
            }
        }
    }
}
