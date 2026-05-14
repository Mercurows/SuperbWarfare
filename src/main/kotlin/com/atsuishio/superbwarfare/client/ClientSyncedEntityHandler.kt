package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage.SyncedEntity
import com.atsuishio.superbwarfare.network.message.receive.PlayerInfoSyncMessage.SyncedPlayerInfo
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
    val SYNCED_PLAYERS = ConcurrentHashMap<SyncedPlayerKey, ClientSyncedPlayer>()

    data class SyncedKey(val dim: ResourceLocation, val id: Int, val friendly: Boolean)

    data class ClientSyncedEntity(val entity: Entity, val timeStamp: Long)

    data class SyncedPlayerKey(val dim: ResourceLocation, val uuid: UUID)

    data class ClientSyncedPlayer(
        val timeStamp: Long,
        val uuid: UUID,
        val pos: Vec3,
        val name: String,
        val onVehicle: Boolean,
        val isDriver: Boolean
    )

    fun sync(dim: ResourceLocation, list: List<SyncedEntity>, friendly: Boolean) {
        val level = mc.level ?: return
        val time = System.currentTimeMillis()
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
            SYNCED_ENTITIES[key] = ClientSyncedEntity(entity, time)
        }
    }

    fun syncPlayerInfo(dim: ResourceLocation, list: List<SyncedPlayerInfo>) {
        if (mc.level == null) return
        val time = System.currentTimeMillis()
        for (info in list) {
            val key = SyncedPlayerKey(dim, info.uuid)
            SYNCED_PLAYERS[key] =
                ClientSyncedPlayer(time, info.uuid, info.pos, info.name, info.onVehicle, info.isDriver)
        }
    }

    fun clean() {
        val tick = System.currentTimeMillis()
        SYNCED_ENTITIES.values.removeIf { tick - it.timeStamp > MiscConfig.CLIENT_SYNC_EXPIRE_TIME.get() }
        SYNCED_PLAYERS.values.removeIf { tick - it.timeStamp > MiscConfig.CLIENT_SYNC_EXPIRE_TIME.get() }
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
    fun getSyncedPlayerInfo(level: Level): List<ClientSyncedPlayer> {
        return SYNCED_PLAYERS.filterKeys { it.dim == level.dimension().location() }.map { it.value }
    }
}