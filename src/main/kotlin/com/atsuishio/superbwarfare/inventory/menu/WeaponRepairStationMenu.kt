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
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import java.util.function.Supplier

open class WeaponRepairStationMenu(
    id: Int, inventory: Inventory,
    private val station: Container = SimpleContainer(2),
    private val stationData: ContainerData = SimpleContainerData(DATA_COUNT)
) : ApricityContainerMenu(
    id,
    inventory,
    LAYOUT,
    mapOf("station" to source(station)),
    emptyMap(),
    inventory.player as? ServerPlayer
) {
    init {
        checkContainerSize(station, 2); checkContainerDataCount(stationData, DATA_COUNT); addDataSlots(stationData)
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
    val materialEnergy get() = value(6)

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        val slot = slots.getOrNull(index) ?: return ItemStack.EMPTY
        if (!slot.hasItem()) return ItemStack.EMPTY
        val stack = slot.item
        val original = stack.copy()
        val moved = when {
            index < 2 -> moveItemStackTo(stack, 2, 38, true)
            WeaponRepairStationBlockEntity.isMaterial(stack) -> moveItemStackTo(stack, 0, 1, false)
            WeaponRepairStationBlockEntity.isRepairTarget(stack) -> moveItemStackTo(stack, 1, 2, false)
            index < 29 -> moveItemStackTo(stack, 29, 38, false)
            else -> moveItemStackTo(stack, 2, 29, false)
        }
        if (!moved) return ItemStack.EMPTY
        if (stack.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
        if (stack.count == original.count) return ItemStack.EMPTY
        slot.onTake(player, stack)
        return original
    }

    companion object {
        const val DATA_COUNT = 14
        const val DOCUMENT = "superbwarfare/screens/weapon_repair_station.html"
        private val LAYOUT = SlotLayout(
            DOCUMENT, listOf(
                SlotLayout.ContainerEntry("station", ContainerBindType.BLOCK_ENTITY, 0, 2, true),
                SlotLayout.ContainerEntry("inventory", ContainerBindType.PLAYER, 2, 36, false)
            )
        )

        private fun source(container: Container) = object : ContainerDataSource {
            override fun bindType() = ContainerBindType.BLOCK_ENTITY
            override fun capacity() = 2
            override fun stillValid(player: ServerPlayer) = container.stillValid(player)

            // Slot restrictions are server-owned and cannot be replaced by DOM filter packets.
            override fun createSlot(index: Int, x: Int, y: Int, filter: Supplier<FilterUtil>): Slot =
                object : Slot(container, index, x, y) {
                    override fun mayPlace(stack: ItemStack) =
                        if (index == 0) WeaponRepairStationBlockEntity.isMaterial(stack) else WeaponRepairStationBlockEntity.isRepairTarget(
                            stack
                        )

                    override fun getMaxStackSize() = if (index == 1) 1 else super.getMaxStackSize()
                }
        }
    }
}
