package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.capability.energy.InfinityEnergyStorage;
import com.atsuishio.superbwarfare.init.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class CreativeChargingStationBlockItem extends BlockItem {

    public CreativeChargingStationBlockItem() {
        super(ModBlocks.CREATIVE_CHARGING_STATION.get(), new Properties().rarity(Rarity.EPIC).stacksTo(1));
    }

    private final IEnergyStorage energy = new InfinityEnergyStorage();

    public @Nullable IEnergyStorage getEnergyStorage() {
        return energy;
    }

}
