package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.init.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;

public class CreativeChargingStationBlockItem extends BlockItem {

    public CreativeChargingStationBlockItem() {
        super(ModBlocks.CREATIVE_CHARGING_STATION.get(), new Properties().rarity(Rarity.EPIC).stacksTo(1));
    }

    // TODO capability
//    @Override
//    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag tag) {
//        return new ICapabilityProvider() {
//            @Override
//            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
//                return ForgeCapabilities.ENERGY.orEmpty(cap, LazyOptional.of(InfinityEnergyStorage::new));
//            }
//        };
//    }
}
