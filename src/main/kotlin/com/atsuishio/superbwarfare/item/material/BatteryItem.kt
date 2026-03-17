package com.atsuishio.superbwarfare.item.material

import com.atsuishio.superbwarfare.client.tooltip.component.CellImageComponent
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.neoforged.neoforge.capabilities.Capabilities
import java.util.*
import kotlin.math.roundToInt

open class BatteryItem(var maxEnergy: Int, properties: Properties) : Item(properties.stacksTo(1)) {
    override fun isBarVisible(pStack: ItemStack): Boolean {
        val cap = pStack.getCapability(Capabilities.EnergyStorage.ITEM) ?: return false
        return cap.energyStored != cap.maxEnergyStored
    }

    override fun getBarWidth(pStack: ItemStack): Int {
        var energy = 0
        val cap = pStack.getCapability(Capabilities.EnergyStorage.ITEM)
        if (cap != null) {
            energy = cap.energyStored
        }

        return (energy * 13f / maxEnergy).roundToInt()
    }

    override fun getBarColor(pStack: ItemStack) = 0xFFFF00

    override fun getTooltipImage(pStack: ItemStack): Optional<TooltipComponent> {
        return Optional.of(CellImageComponent(pStack))
    }

    fun makeFullEnergyStack(): ItemStack {
        val stack = ItemStack(this)
        val cap = stack.getCapability(Capabilities.EnergyStorage.ITEM) ?: return stack

        cap.receiveEnergy(maxEnergy, false)
        return stack
    }

    override fun inventoryTick(pStack: ItemStack, pLevel: Level, entity: Entity, pSlotId: Int, pIsSelected: Boolean) {
        super.inventoryTick(pStack, pLevel, entity, pSlotId, pIsSelected)
        // TODO 完成寻找能充电的物品进行充电（包括饰品栏里的？）
        if (entity is Player) {
            for (chargeable in entity.inventory.items) {
                if (chargeable.item is BatteryItem) {
//                    val stackStorage = chargeable.getCapability(Capabilities.EnergyStorage.ITEM);
//
//                    assert(stack.getCapability<IEnergyStorage>(ForgeCapabilities.ENERGY).resolve().isPresent())
//                    val stackStorage: IEnergyStorage =
//                        stack.getCapability<IEnergyStorage>(ForgeCapabilities.ENERGY).resolve().get()
//                    val stackMaxEnergy = stackStorage.maxEnergyStored
//                    val stackEnergy = stackStorage.energyStored
//
//                    assert(cell.getCapability<IEnergyStorage>(ForgeCapabilities.ENERGY).resolve().isPresent)
//                    val cellStorage = cell.getCapability<IEnergyStorage>(ForgeCapabilities.ENERGY).resolve().get()
//                    val cellEnergy = cellStorage.energyStored
//
//                    val stackEnergyNeed =
//                        min(cellEnergy.toDouble(), (stackMaxEnergy - stackEnergy).toDouble()).toInt()
//
//                    if (cellEnergy > 0) {
//                        stack.getCapability<IEnergyStorage>(ForgeCapabilities.ENERGY).ifPresent(
//                            NonNullConsumer<IEnergyStorage> { iEnergyStorage: IEnergyStorage ->
//                                iEnergyStorage.receiveEnergy(
//                                    stackEnergyNeed,
//                                    false
//                                )
//                            }
//                        )
//                    }
//                    cell.getCapability<IEnergyStorage>(ForgeCapabilities.ENERGY).ifPresent { cEnergy: IEnergyStorage ->
//                        cEnergy.extractEnergy(
//                            stackEnergyNeed,
//                            false
//                        )
//                    }
                }
            }
        }
    }
}