package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage.SyncedEntity
import com.atsuishio.superbwarfare.network.message.receive.PlayerInfoSyncMessage
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ClientSyncedEntityHandler {
    @JvmField
    val SYNCED_ENTITIES = ConcurrentHashMap<SyncedKey, ClientSyncedEntity>()

    @JvmField
    val SYNCED_PLAYERS = ConcurrentHashMap<UUID, ClientSyncedPlayerInfo>()

    data class SyncedKey(val dim: ResourceLocation, val id: Int, val friendly: Boolean)

    data class ClientSyncedEntity(val entity: Entity, val timeStamp: Int)

    data class ClientSyncedPlayerInfo(
        val pos: Vec3,
        val name: String,
        val timeStamp: Int,
        val onVehicle: Boolean,
        val isDriver: Boolean
    )

    data class ClientSyncedPlayer(
        val uuid: UUID,
        val pos: Vec3,
        val name: String,
        val onVehicle: Boolean,
        val isDriver: Boolean
    )

    fun sync(dim: ResourceLocation, list: List<SyncedEntity>, friendly: Boolean) {
        val player = localPlayer ?: return
        val level = mc.level ?: return
        val tick = player.tickCount
        for (syncedEntity in list) {
            val key = SyncedKey(dim, syncedEntity.id, friendly)
            val existedEntity = SYNCED_ENTITIES[key]
            var entity: Entity
            if (existedEntity != null) {
                entity = existedEntity.entity
            } else {
                val type = BuiltInRegistries.ENTITY_TYPE.get(syncedEntity.type)
                entity = type.create(level) ?: continue
                val tag = syncedEntity.tag as? CompoundTag ?: continue
                entity.load(tag)
                entity.id = syncedEntity.id
            }

            val pos = syncedEntity.pos
            entity.xo = pos.x
            entity.yo = pos.y
            entity.zo = pos.z
            entity.setPos(syncedEntity.pos)
            entity.deltaMovement = syncedEntity.motion
            SYNCED_ENTITIES[key] = ClientSyncedEntity(entity, tick)
        }
    }

    fun syncPlayerInfo(dim: ResourceLocation, list: List<PlayerInfoSyncMessage.SyncedPlayerInfo>) {
        val player = localPlayer ?: return
        val level = mc.level ?: return
        if (dim != level.dimension().location()) return

        val tick = player.tickCount
        for (info in list) {
            val uuid = info.uuid
            SYNCED_PLAYERS[uuid] = ClientSyncedPlayerInfo(info.pos, info.name, tick, info.onVehicle, info.isDriver)
        }
    }

    fun clean(tick: Int) {
        SYNCED_ENTITIES.values.removeIf { tick - it.timeStamp > MiscConfig.CLIENT_SYNC_EXPIRE_TIME.get() }
        SYNCED_PLAYERS.values.removeIf { tick - it.timeStamp > MiscConfig.CLIENT_SYNC_EXPIRE_TIME.get() }
        // 测试的时候用这个，把上面的注释掉
//        val toRemove = SYNCED_ENTITIES.filter {
//            tick - it.value.timeStamp > MiscConfig.CLIENT_SYNC_EXPIRE_TIME.get()
//        }
//        toRemove.forEach {
//            localPlayer?.displayClientMessage(
//                Component.literal(
//                    "${it.value.entity.displayName.string} was expired with time ${it.value.timeStamp}, id ${it.key.id}"
//                ),
//                false
//            )
//            SYNCED_ENTITIES.remove(it.key)
//        }
    }

    @JvmStatic
    fun getSyncedFriendlyEntities(level: Level): List<Entity> {
        return SYNCED_ENTITIES.filterKeys { it.dim == level.dimension().location() && it.friendly }
            .map { it.value.entity }
    }

    @JvmStatic
    fun getSyncedHostileEntities(level: Level): List<Entity> {
        return SYNCED_ENTITIES.filterKeys { it.dim == level.dimension().location() && !it.friendly }
            .map { it.value.entity }
    }

    @JvmStatic
    fun getSyncedEntities(level: Level): List<Entity> {
        return SYNCED_ENTITIES.filterKeys { it.dim == level.dimension().location() }.map { it.value.entity }
    }

    @JvmStatic
    fun getSyncedPlayerInfo(): List<ClientSyncedPlayer> {
        return SYNCED_PLAYERS.entries.map {
            ClientSyncedPlayer(
                it.key,
                it.value.pos,
                it.value.name,
                it.value.onVehicle,
                it.value.isDriver
            )
        }
    }
}