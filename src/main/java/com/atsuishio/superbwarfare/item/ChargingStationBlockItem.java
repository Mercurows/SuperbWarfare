package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.client.tooltip.component.ChargingStationImageComponent;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.init.ModBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ChargingStationBlockItem extends BlockItem {

    public ChargingStationBlockItem() {
        super(ModBlocks.CHARGING_STATION.get(), new Item.Properties().stacksTo(1));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack pStack) {
        CompoundTag tag = BlockItem.getBlockEntityData(pStack);
        int energy = tag == null ? 0 : tag.getInt("Energy");
        return energy != MiscConfig.CHARGING_STATION_MAX_ENERGY.get() && energy != 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack pStack) {
        CompoundTag tag = BlockItem.getBlockEntityData(pStack);
        int energy = tag == null ? 0 : tag.getInt("Energy");
        return Math.round(energy * 13F / Math.max(1, MiscConfig.CHARGING_STATION_MAX_ENERGY.get()));
    }

    @Override
    public int getBarColor(@NotNull ItemStack pStack) {
        return 0xFFFF00;
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new ChargingStationImageComponent(pStack));
    }
}
