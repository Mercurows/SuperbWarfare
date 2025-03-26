package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.capability.energy.InfinityEnergyStorage;
import com.atsuishio.superbwarfare.init.ModBlocks;
import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CreativeChargingStationBlockItem extends BlockItem {

    public CreativeChargingStationBlockItem() {
        super(ModBlocks.CREATIVE_CHARGING_STATION.get(), new Properties().rarity(Rarity.EPIC).stacksTo(1));
    }

    public static class EnergyStorageProvider implements ICapabilityProvider<ItemStack, Void, IEnergyStorage> {

        private final IEnergyStorage energy = new InfinityEnergyStorage();

        @Override
        public @Nullable IEnergyStorage getCapability(@NotNull ItemStack object, Void context) {
            if (object.getItem() != ModItems.CREATIVE_CHARGING_STATION.get()) return null;
            return energy;
        }
    }

}
