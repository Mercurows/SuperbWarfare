package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Hammer extends SwordItem {

    public Hammer() {
        super(new Tier() {
            public int getUses() {
                return 400;
            }

            public float getSpeed() {
                return 4f;
            }

            public float getAttackDamageBonus() {
                return 8f;
            }

            public int getLevel() {
                return 1;
            }

            public int getEnchantmentValue() {
                return 9;
            }

            public Ingredient getRepairIngredient() {
                return Ingredient.of(new ItemStack(Items.IRON_INGOT));
            }
        }, 71, -3.2f, new Item.Properties());
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack itemstack) {
        ItemStack stack = new ItemStack(this);
        stack.setDamageValue(itemstack.getDamageValue() + 1);
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public boolean isRepairable(ItemStack itemstack) {
        return true;
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        var item = event.getCrafting();
        var container = event.getInventory();
        var player = event.getEntity();
        if (player == null) return;

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
