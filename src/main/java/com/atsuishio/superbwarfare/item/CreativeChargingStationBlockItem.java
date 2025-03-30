package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.capability.energy.InfinityEnergyStorage;
import com.atsuishio.superbwarfare.init.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class CreativeChargingStationBlockItem extends BlockItem {

    public CreativeChargingStationBlockItem() {
        super(ModBlocks.CREATIVE_CHARGING_STATION.get(), new Properties().rarity(Rarity.EPIC).stacksTo(1));
    }

    private final IEnergyStorage energy = new InfinityEnergyStorage();

    public @Nullable IEnergyStorage getEnergyStorage() {
        return energy;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.creative_charging_station").withStyle(ChatFormatting.GRAY));
    }
}
