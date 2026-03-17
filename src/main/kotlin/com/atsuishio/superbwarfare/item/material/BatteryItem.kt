package com.atsuishio.superbwarfare.item.material

import com.atsuishio.superbwarfare.capability.energy.ItemEnergyProvider
import com.atsuishio.superbwarfare.client.tooltip.component.CellImageComponent
import net.minecraft.ChatFormatting
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
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.capabilities.ICapabilityProvider
import top.theillusivec4.curios.api.CuriosApi
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.math.min
import kotlin.math.roundToInt

open class BatteryItem(var maxEnergy: Int, properties: Properties) : Item(properties.stacksTo(1)) {
    private val energyCapacity: () -> Int = { maxEnergy }

    companion object {
        const val TAG_ENABLED = "Enabled"
    }

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

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        val flag = pStack.tag == null || !pStack.tag!!.getBoolean(TAG_ENABLED)
        pTooltipComponents.add(
            Component.translatable("des.superbwarfare.battery.${if (flag) "disable" else "enable"}").withStyle(
                if (flag) ChatFormatting.GRAY else ChatFormatting.GREEN
            )
        )
    }

    fun makeFullEnergyStack(): ItemStack {
        val stack = ItemStack(this)
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent { it.receiveEnergy(maxEnergy, false) }
        return stack
    }

    override fun inventoryTick(pStack: ItemStack, pLevel: Level, entity: Entity, pSlotId: Int, pIsSelected: Boolean) {
        super.inventoryTick(pStack, pLevel, entity, pSlotId, pIsSelected)
        if (pStack.tag == null || !pStack.tag!!.getBoolean(TAG_ENABLED)) return
        if (entity !is Player) return
        val energyStorage = pStack.getCapability(ForgeCapabilities.ENERGY).resolve().getOrNull() ?: return

        for (stack in entity.inventory.items) {
            if (stack.item is BatteryItem) continue
            if (!stack.getCapability(ForgeCapabilities.ENERGY).isPresent) continue
            val toCharge = stack.getCapability(ForgeCapabilities.ENERGY).resolve().getOrNull() ?: continue
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
                if (!stack.getCapability(ForgeCapabilities.ENERGY).isPresent) return@forEach
                val toCharge = stack.getCapability(ForgeCapabilities.ENERGY).resolve().getOrNull() ?: return@forEach
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
            stack.orCreateTag.putBoolean(TAG_ENABLED, !stack.orCreateTag.getBoolean(TAG_ENABLED))
            return true
        }
        return super.overrideOtherStackedOnMe(stack, other, slot, action, player, access)
    }
}