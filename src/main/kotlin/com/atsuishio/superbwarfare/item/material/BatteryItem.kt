package com.atsuishio.superbwarfare.item.material

import com.atsuishio.superbwarfare.capability.energy.ItemEnergyProvider
import com.atsuishio.superbwarfare.client.tooltip.component.CellImageComponent
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.NonNullConsumer
import net.minecraftforge.energy.IEnergyStorage
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt

open class BatteryItem(var maxEnergy: Int, properties: Properties) : Item(properties.stacksTo(1)) {
    private val energyCapacity: () -> Int = { maxEnergy }

    override fun isBarVisible(pStack: ItemStack): Boolean {
        return pStack.getCapability(ForgeCapabilities.ENERGY)
            .map { it.energyStored }
            .orElse(0) != maxEnergy
    }

    override fun getBarWidth(pStack: ItemStack): Int {
        val energy = pStack.getCapability(ForgeCapabilities.ENERGY)
            .map { it.energyStored }
            .orElse(0)

        return (energy * 13f / maxEnergy).roundToInt()
    }

    override fun initCapabilities(stack: ItemStack, tag: CompoundTag?): ICapabilityProvider {
        return ItemEnergyProvider(stack, energyCapacity())
    }

    override fun getBarColor(pStack: ItemStack): Int {
        return 0xFFFF00
    }

    override fun getTooltipImage(pStack: ItemStack): Optional<TooltipComponent> {
        return Optional.of<TooltipComponent>(CellImageComponent(pStack))
    }

    fun makeFullEnergyStack(): ItemStack {
        val stack = ItemStack(this)
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent { it.receiveEnergy(maxEnergy, false) }
        return stack
    }

    override fun inventoryTick(pStack: ItemStack, pLevel: Level, entity: Entity, pSlotId: Int, pIsSelected: Boolean) {
        super.inventoryTick(pStack, pLevel, entity, pSlotId, pIsSelected)
        // TODO 完成寻找能充电的物品进行充电（包括饰品栏里的？）
        if (entity is Player) {
            for (chargeable in entity.inventory.items) {
                if (chargeable.item is BatteryItem) {
                    assert(stack.getCapability<IEnergyStorage>(ForgeCapabilities.ENERGY).resolve().isPresent())
                    val stackStorage: IEnergyStorage =
                        stack.getCapability<IEnergyStorage>(ForgeCapabilities.ENERGY).resolve().get()
                    val stackMaxEnergy = stackStorage.maxEnergyStored
                    val stackEnergy = stackStorage.energyStored

                    assert(cell.getCapability<IEnergyStorage>(ForgeCapabilities.ENERGY).resolve().isPresent)
                    val cellStorage = cell.getCapability<IEnergyStorage>(ForgeCapabilities.ENERGY).resolve().get()
                    val cellEnergy = cellStorage.energyStored

                    val stackEnergyNeed =
                        min(cellEnergy.toDouble(), (stackMaxEnergy - stackEnergy).toDouble()).toInt()

                    if (cellEnergy > 0) {
                        stack.getCapability<IEnergyStorage>(ForgeCapabilities.ENERGY).ifPresent(
                            NonNullConsumer<IEnergyStorage> { iEnergyStorage: IEnergyStorage ->
                                iEnergyStorage.receiveEnergy(
                                    stackEnergyNeed,
                                    false
                                )
                            }
                        )
                    }
                    cell.getCapability<IEnergyStorage>(ForgeCapabilities.ENERGY).ifPresent { cEnergy: IEnergyStorage ->
                        cEnergy.extractEnergy(
                            stackEnergyNeed,
                            false
                        )
                    }
                }
            }
        }
    }
}