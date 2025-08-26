package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.client.TooltipTool;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class Hammer extends SwordItem {

    public Hammer(Tier tier, int attackDamage, float attackSpeed, int maxDamage) {
        this(tier, attackDamage, attackSpeed, new CustomDamageProperty(maxDamage));
    }

    protected Hammer(Tier tier, int attackDamage, float attackSpeed, Item.Properties properties) {
        super(tier, properties.attributes(SwordItem.createAttributes(tier, attackDamage, attackSpeed)));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        TooltipTool.addHideText(tooltipComponents, Component.translatable("des.superbwarfare.hammer", NBTTool.getTag(stack).getInt("CraftCount")).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean hasCraftingRemainingItem(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public @NotNull ItemStack getCraftingRemainingItem(ItemStack itemstack) {
        var stack = itemstack.copy();

        var tag = NBTTool.getTag(stack);
        tag.putInt("CraftCount", tag.getInt("CraftCount") + 1);
        NBTTool.saveTag(stack, tag);

        if (!itemstack.isDamageableItem()) return stack;

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
