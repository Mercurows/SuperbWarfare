package com.atsuishio.superbwarfare.world.saveddata

import com.atsuishio.superbwarfare.network.message.receive.RemoteBindingSyncMessage
import com.atsuishio.superbwarfare.tools.sendPacket
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.saveddata.SavedData
import java.util.*

open class RemoteBindingData protected constructor() : SavedData() {
    private val vehiclesByPlayer = mutableMapOf<UUID, UUID>()
    private val playersByVehicle = mutableMapOf<UUID, UUID>()

    open fun getVehicle(playerId: UUID): UUID? = vehiclesByPlayer[playerId]

    open fun getOperator(vehicleId: UUID): UUID? = playersByVehicle[vehicleId]

    open fun bind(player: ServerPlayer, vehicleId: UUID): Boolean {
        val operator = getOperator(vehicleId)
        if (operator != null && operator != player.uuid) return false
        val previous = vehiclesByPlayer.put(player.uuid, vehicleId)
        if (previous != vehicleId) {
            if (previous != null) playersByVehicle.remove(previous)
            playersByVehicle[vehicleId] = player.uuid
            setDirty()
        }
        syncTo(player)
        return true
    }

    open fun unbind(player: ServerPlayer): Boolean {
        val vehicleId = vehiclesByPlayer.remove(player.uuid) ?: return false
        playersByVehicle.remove(vehicleId)
        setDirty()
        syncTo(player)
        return true
    }

    open fun removeVehicle(server: MinecraftServer, vehicleId: UUID) {
        val operator = playersByVehicle.remove(vehicleId) ?: return
        vehiclesByPlayer.remove(operator)
        setDirty()
        server.playerList.getPlayer(operator)?.let { player ->
            syncTo(player)
            player.displayClientMessage(Component.translatable("tips.superbwarfare.monitor.link_lost"), true)
        }
    }

    open fun syncTo(player: ServerPlayer) {
        player.sendPacket(RemoteBindingSyncMessage(getVehicle(player.uuid) != null))
    }

    override fun save(tag: CompoundTag): CompoundTag {
        val bindings = ListTag()
        for ((player, vehicle) in vehiclesByPlayer) {
            bindings.add(CompoundTag().apply {
                putUUID("Player", player)
                putUUID("Vehicle", vehicle)
            })
        }
        tag.put("Bindings", bindings)
        return tag
    }

    companion object {
        private const val ID = "superbwarfare_remote_bindings"

        @JvmStatic
        fun get(server: MinecraftServer): RemoteBindingData =
            server.overworld().dataStorage.computeIfAbsent(::load, ::RemoteBindingData, ID)

        private fun load(tag: CompoundTag): RemoteBindingData = RemoteBindingData().apply {
            val bindings = tag.getList("Bindings", Tag.TAG_COMPOUND.toInt())
            for (index in bindings.indices) {
                val binding = bindings.getCompound(index)
                if (!binding.hasUUID("Player") || !binding.hasUUID("Vehicle")) continue
                val player = binding.getUUID("Player")
                val vehicle = binding.getUUID("Vehicle")
                if (player in vehiclesByPlayer || vehicle in playersByVehicle) continue
                vehiclesByPlayer[player] = vehicle
                playersByVehicle[vehicle] = player
            }
        }
    }
}
