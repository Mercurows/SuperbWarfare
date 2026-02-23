package com.atsuishio.superbwarfare.item.blockitem

import com.atsuishio.superbwarfare.capability.energy.InfinityEnergyStorage
import com.atsuishio.superbwarfare.init.ModBlocks
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.LazyOptional

class CreativeChargingStationBlockItem :
    BlockItem(ModBlocks.CREATIVE_CHARGING_STATION.get(), Properties().rarity(Rarity.EPIC).stacksTo(1)) {
    override fun initCapabilities(stack: ItemStack?, tag: CompoundTag?): ICapabilityProvider {
        return object : ICapabilityProvider {
            override fun <T> getCapability(cap: Capability<T?>, side: Direction?): LazyOptional<T?> {
                return ForgeCapabilities.ENERGY.orEmpty<T?>(
                    cap,
                    LazyOptional.of { InfinityEnergyStorage() }
                )
            }
        }
    }
}
