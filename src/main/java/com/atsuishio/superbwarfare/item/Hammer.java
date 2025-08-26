package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.client.TooltipTool;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Hammer extends SwordItem {

    public Hammer(Tier tier, int attackDamage, float attackSpeed, Item.Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        TooltipTool.addHideText(pTooltipComponents, Component.translatable("des.superbwarfare.hammer", pStack.getOrCreateTag().getInt("CraftCount")).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack itemstack) {
        ItemStack stack = itemstack.copy();
        stack.hurt(1, RandomSource.create(), null);
        stack.getOrCreateTag().putInt("CraftCount", stack.getOrCreateTag().getInt("CraftCount") + 1);
        if (stack.isEmpty() || stack.getDamageValue() >= stack.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public boolean isRepairable(ItemStack itemstack) {
        return true;
    }

    @Override
    public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        pAttacker.level().playSound(null, pTarget.getOnPos(), ModSounds.MELEE_HIT.get(), SoundSource.PLAYERS, 1, (float) ((2 * org.joml.Math.random() - 1) * 0.1f + 1.0f));
        return super.hurtEnemy(pStack, pTarget, pAttacker);
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        var item = event.getCrafting();
        var container = event.getInventory();
        var player = event.getEntity();
        if (player == null) return;

        if (player.level().isClientSide) return;

        if (item.is(ModTags.Items.HAMMER)) {
            int count = 0;
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (container.getItem(i).is(ModTags.Items.HAMMER)) count++;
            }
            if (count == 2) {
                container.clearContent();
            }
        }
    }
}
