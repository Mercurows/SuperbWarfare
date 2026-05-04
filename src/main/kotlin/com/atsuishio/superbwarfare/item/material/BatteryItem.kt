package com.atsuishio.superbwarfare.item.material

import com.atsuishio.superbwarfare.client.tooltip.component.CellImageComponent
import com.atsuishio.superbwarfare.item.EnergyStorageItem
import com.atsuishio.superbwarfare.tools.tag
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.SlotAccess
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ClickAction
import net.minecraft.world.inventory.Slot
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.neoforged.neoforge.capabilities.Capabilities
import top.theillusivec4.curios.api.CuriosApi
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt

open class BatteryItem(var maxEnergy: Int, properties: Properties) : Item(properties.stacksTo(1)), EnergyStorageItem {

    companion object {
        const val TAG_ENABLED = "Enabled"
    }

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

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        val flag = stack.tag == null || !stack.tag!!.getBoolean(TAG_ENABLED)
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.battery.${if (flag) "disable" else "enable"}").withStyle(
                if (flag) ChatFormatting.GRAY else ChatFormatting.GREEN
            )
        )
    }

    fun makeFullEnergyStack(): ItemStack {
        val stack = ItemStack(this)
        val cap = stack.getCapability(Capabilities.EnergyStorage.ITEM) ?: return stack

        cap.receiveEnergy(maxEnergy, false)
        return stack
    }

    override fun inventoryTick(pStack: ItemStack, pLevel: Level, entity: Entity, pSlotId: Int, pIsSelected: Boolean) {
        super.inventoryTick(pStack, pLevel, entity, pSlotId, pIsSelected)
        if (pStack.tag == null || !pStack.tag!!.getBoolean(TAG_ENABLED)) return
        if (entity !is Player) return
        val energyStorage = pStack.getCapability(Capabilities.EnergyStorage.ITEM) ?: return

        for (stack in entity.inventory.items) {
            if (stack.item is BatteryItem) continue
            val toCharge = stack.getCapability(Capabilities.EnergyStorage.ITEM) ?: continue
            if (!toCharge.canReceive()) continue

            val cellEnergy = energyStorage.energyStored
            if (cellEnergy <= 0) break

            val stackEnergyNeed =
                min(cellEnergy.toDouble(), (toCharge.maxEnergyStored - toCharge.energyStored).toDouble()).toInt()

            toCharge.receiveEnergy(stackEnergyNeed, false)
            energyStorage.extractEnergy(stackEnergyNeed, false)
        }

        CuriosApi.getCuriosInventory(entity).ifPresent { s ->
            (0..<s.slots).forEach {
                val stack = s.equippedCurios.getStackInSlot(it)
                if (stack.isEmpty) return@forEach
                if (stack.item is BatteryItem) return@forEach
                val toCharge = stack.getCapability(Capabilities.EnergyStorage.ITEM) ?: return@forEach
                if (!toCharge.canReceive()) return@forEach

                val cellEnergy = energyStorage.energyStored
                if (cellEnergy <= 0) return@forEach

                val stackEnergyNeed =
                    min(cellEnergy.toDouble(), (toCharge.maxEnergyStored - toCharge.energyStored).toDouble()).toInt()

                toCharge.receiveEnergy(stackEnergyNeed, false)
                energyStorage.extractEnergy(stackEnergyNeed, false)
            }
        }
    }

    override fun overrideOtherStackedOnMe(
        stack: ItemStack,
        other: ItemStack,
        slot: Slot,
        action: ClickAction,
        player: Player,
        access: SlotAccess
    ): Boolean {
        if (other.isEmpty && action == ClickAction.SECONDARY) {
            val tag = stack.tag ?: CompoundTag()
            tag.putBoolean(TAG_ENABLED, !tag.getBoolean(TAG_ENABLED))
            stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag))
            return true
        }
        return super.overrideOtherStackedOnMe(stack, other, slot, action, player, access)
    }

    override fun getMaxEnergy(stack: ItemStack) = this.maxEnergy
}