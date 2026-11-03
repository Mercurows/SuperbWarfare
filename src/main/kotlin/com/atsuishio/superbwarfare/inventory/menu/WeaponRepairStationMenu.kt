package com.atsuishio.superbwarfare.inventory.menu

import com.atsuishio.superbwarfare.block.entity.WeaponRepairStationBlockEntity
import com.atsuishio.superbwarfare.init.ModMenuTypes
import com.sighs.apricityui.container.SlotLayout
import com.sighs.apricityui.container.bind.ContainerBindType
import com.sighs.apricityui.container.datasource.ContainerDataSource
import com.sighs.apricityui.container.filter.FilterUtil
import com.sighs.apricityui.screen.ApricityContainerMenu
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import java.util.function.Supplier

open class WeaponRepairStationMenu(
    id: Int, inventory: Inventory,
    private val station: Container = SimpleContainer(1),
    private val stationData: ContainerData = SimpleContainerData(DATA_COUNT)
) : ApricityContainerMenu(
    id,
    inventory,
    LAYOUT,
    mapOf("station" to source(station), "equipment" to equipmentSource(inventory)),
    emptyMap(),
    inventory.player as? ServerPlayer
) {
    init {
        checkContainerSize(station, 1); checkContainerDataCount(stationData, DATA_COUNT); addDataSlots(stationData)
    }

    // AUI's base constructor uses its generic menu type; opening must select our registered screen.
    override fun getType() = ModMenuTypes.WEAPON_REPAIR_STATION_MENU.get()
    override fun stillValid(player: Player) = station.stillValid(player)
    override fun clickMenuButton(player: Player, id: Int): Boolean {
        if (id != 0 || !stillValid(player)) return false
        (station as? WeaponRepairStationBlockEntity)?.toggleEnabled() ?: return false
        return true
    }

    fun value(field: Int): Int = (stationData[field * 2] and 65535) or ((stationData[field * 2 + 1] and 65535) shl 16)
    val energy get() = value(0)
    val capacity get() = value(1)
    val energyCost get() = value(2)
    val interval get() = value(3)
    val progress get() = value(4)
    val enabled get() = value(5) != 0
    val fe get() = value(6)
    val feCapacity get() = value(7)
    val fePerRepairEnergy get() = value(8)

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots.getOrNull(index) ?: return ItemStack.EMPTY
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY
        val stack = slot.item
        val original = stack.copy()
        val moved = when {
            index == 0 -> moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)
            WeaponRepairStationBlockEntity.isRepairTarget(stack) -> moveItemStackTo(stack, 0, 1, false)
            index < INVENTORY_START -> moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, false)
            index < HOTBAR_START -> moveItemStackTo(stack, HOTBAR_START, INVENTORY_END, false)
            else -> moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false)
        }
        if (!moved) return ItemStack.EMPTY
        if (stack.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
        if (stack.count == original.count) return ItemStack.EMPTY
        slot.onTake(player, stack)
        return original
    }

    companion object {
        const val DATA_COUNT = 18
        const val DOCUMENT = "superbwarfare/screens/weapon_repair_station.html"
        private const val INVENTORY_START = 6
        private const val HOTBAR_START = INVENTORY_START + 27
        private const val INVENTORY_END = INVENTORY_START + 36
        private val EQUIPMENT_SLOTS = listOf(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND
        )
        private val LAYOUT = SlotLayout(
            DOCUMENT, listOf(
                SlotLayout.ContainerEntry("station", ContainerBindType.BLOCK_ENTITY, 0, 1, true),
                SlotLayout.ContainerEntry("equipment", ContainerBindType.ENTITY, 1, 5, false),
                SlotLayout.ContainerEntry("inventory", ContainerBindType.PLAYER, INVENTORY_START, 36, false)
            )
        )

        private fun equipmentSource(inventory: Inventory) = object : ContainerDataSource {
            override fun bindType() = ContainerBindType.ENTITY
            override fun capacity() = EQUIPMENT_SLOTS.size
            override fun stillValid(player: ServerPlayer) = inventory.player === player

            override fun createSlot(index: Int, x: Int, y: Int, filter: Supplier<FilterUtil>): Slot {
                val equipmentSlot = EQUIPMENT_SLOTS[index]
                val armor = equipmentSlot.type == EquipmentSlot.Type.ARMOR
                val inventoryIndex = if (armor) 36 + equipmentSlot.index else 40
                return object : Slot(inventory, inventoryIndex, x, y) {
                    override fun getMaxStackSize() = if (armor) 1 else super.getMaxStackSize()
                    override fun mayPlace(stack: ItemStack) = !armor || stack.canEquip(equipmentSlot, inventory.player)
                    override fun mayPickup(player: Player) =
                        (!armor || player.isCreative || !EnchantmentHelper.hasBindingCurse(item)) && super.mayPickup(player)

                    override fun setByPlayer(stack: ItemStack) {
                        inventory.player.onEquipItem(equipmentSlot, item, stack)
                        super.setByPlayer(stack)
                    }
                }
            }
        }

        private fun source(container: Container) = object : ContainerDataSource {
            override fun bindType() = ContainerBindType.BLOCK_ENTITY
            override fun capacity() = 1
            override fun stillValid(player: ServerPlayer) = container.stillValid(player)

            // Slot restrictions are server-owned and cannot be replaced by DOM filter packets.
            override fun createSlot(index: Int, x: Int, y: Int, filter: Supplier<FilterUtil>): Slot =
                object : Slot(container, index, x, y) {
                    override fun mayPlace(stack: ItemStack) = WeaponRepairStationBlockEntity.isRepairTarget(stack)

                    override fun getMaxStackSize() = 1
                }
        }
    }
}
