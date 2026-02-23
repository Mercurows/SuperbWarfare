package com.atsuishio.superbwarfare.item.blockitem

import com.atsuishio.superbwarfare.capability.energy.InfinityEnergyStorage
import com.atsuishio.superbwarfare.init.ModBlocks
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.neoforged.neoforge.energy.IEnergyStorage
import javax.annotation.ParametersAreNonnullByDefault

class CreativeChargingStationBlockItem :
    BlockItem(ModBlocks.CREATIVE_CHARGING_STATION.get(), Properties().rarity(Rarity.EPIC).stacksTo(1)) {
    private val energy = InfinityEnergyStorage()

    val energyStorage: IEnergyStorage
        get() = energy

    @ParametersAreNonnullByDefault
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.creative_charging_station").withStyle(ChatFormatting.GRAY)
        )
    }
}
