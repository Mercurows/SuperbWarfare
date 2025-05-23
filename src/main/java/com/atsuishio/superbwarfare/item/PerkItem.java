package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class PerkItem<T extends Perk> extends Item {
    private final DeferredHolder<Perk, T> perk;

    public PerkItem(DeferredHolder<Perk, T> perk) {
        super(new Properties());
        this.perk = perk;
    }

    public PerkItem(DeferredHolder<Perk, T> perk, Rarity rarity) {
        super(new Properties().rarity(rarity));
        this.perk = perk;
    }

    public Perk getPerk() {
        return this.perk.get();
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        ChatFormatting chatFormatting = switch (this.getPerk().type) {
            case AMMO -> ChatFormatting.YELLOW;
            case FUNCTIONAL -> ChatFormatting.GREEN;
            case DAMAGE -> ChatFormatting.RED;
        };

        tooltipComponents.add(Component.translatable("des.superbwarfare." + this.getPerk().descriptionId).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("perk.superbwarfare.slot").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable("perk.superbwarfare.slot_" + this.getPerk().type.getName()).withStyle(chatFormatting)));
        if (this.getPerk() instanceof AmmoPerk ammoPerk) {
            if (ammoPerk.damageRate < 1) {
                tooltipComponents.add(Component.translatable("des.superbwarfare.perk_damage_reduce").withStyle(ChatFormatting.RED));
            } else if (ammoPerk.damageRate > 1) {
                tooltipComponents.add(Component.translatable("des.superbwarfare.perk_damage_plus").withStyle(ChatFormatting.GREEN));
            }

            if (ammoPerk.speedRate < 1) {
                tooltipComponents.add(Component.translatable("des.superbwarfare.perk_speed_reduce").withStyle(ChatFormatting.RED));
            } else if (ammoPerk.speedRate > 1) {
                tooltipComponents.add(Component.translatable("des.superbwarfare.perk_speed_plus").withStyle(ChatFormatting.GREEN));
            }

            if (ammoPerk.slug) {
                tooltipComponents.add(Component.translatable("des.superbwarfare.perk_slug").withStyle(ChatFormatting.YELLOW));
            }
        }
    }

}
