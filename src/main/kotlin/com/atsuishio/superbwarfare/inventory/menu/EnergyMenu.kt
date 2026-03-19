package com.atsuishio.superbwarfare.inventory.menu

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyDataSlot
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyDataSlot.Companion.forContainer
import com.atsuishio.superbwarfare.network.message.receive.ContainerDataMessage
import com.atsuishio.superbwarfare.tools.sendPacket
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraftforge.event.entity.player.PlayerContainerEvent.Close
import net.minecraftforge.event.entity.player.PlayerContainerEvent.Open
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber

abstract class EnergyMenu : AbstractContainerMenu {
    private val containerEnergyDataSlots: MutableList<ContainerEnergyDataSlot> = arrayListOf()
    private val usingPlayers: MutableList<ServerPlayer> = arrayListOf()

    constructor(pMenuType: MenuType<*>, pContainerId: Int) : super(pMenuType, pContainerId)

    constructor(pMenuType: MenuType<*>, id: Int, containerData: ContainerEnergyData) : super(pMenuType, id) {
        for (i in 0..<containerData.getCount()) {
            this.containerEnergyDataSlots.add(forContainer(containerData, i))
        }
    }

    override fun broadcastChanges() {
        val pairs: MutableList<ContainerDataMessage.Pair> = arrayListOf()
        for (i in this.containerEnergyDataSlots.indices) {
            val dataSlot = this.containerEnergyDataSlots[i]
            if (dataSlot.checkAndClearUpdateFlag()) pairs.add(ContainerDataMessage.Pair(i, dataSlot.get()))
        }

        if (!pairs.isEmpty()) {
            this.usingPlayers.forEach { p ->
                p.sendPacket(ContainerDataMessage(this.containerId, pairs))
            }
        }

        super.broadcastChanges()
    }

    override fun setData(id: Int, data: Int) {
        if (id < 0 || id >= this.containerEnergyDataSlots.size) {
            Mod.LOGGER.error("EnergyMenu.setData(Int) id out of bounds: {}", id)
            return
        }
        this.containerEnergyDataSlots[id].set(data.toLong())
    }

    fun setData(id: Int, data: Long) {
        if (id < 0 || id >= this.containerEnergyDataSlots.size) {
            Mod.LOGGER.error("EnergyMenu.setData(Long) id out of bounds: {}", id)
            return
        }
        this.containerEnergyDataSlots[id].set(data)
    }

    @EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE)
    companion object {
        @SubscribeEvent
        fun onEnergyMenuOpened(event: Open) {
            val menu = event.container as? EnergyMenu ?: return
            val player = event.entity as? ServerPlayer ?: return

            menu.usingPlayers.add(player)

            val toSync: MutableList<ContainerDataMessage.Pair> = arrayListOf()
            for (i in menu.containerEnergyDataSlots.indices) {
                toSync.add(ContainerDataMessage.Pair(i, menu.containerEnergyDataSlots[i].get()))
            }
            sendPacketTo(player, ContainerDataMessage(menu.containerId, toSync))
        }

        @SubscribeEvent
        fun onEnergyMenuClosed(event: Close) {
            val menu = event.container as? EnergyMenu ?: return
            val player = event.entity as? ServerPlayer ?: return

            menu.usingPlayers.remove(player)
        }
    }
}
