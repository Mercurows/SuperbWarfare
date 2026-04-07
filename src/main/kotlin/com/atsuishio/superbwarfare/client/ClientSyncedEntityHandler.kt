package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage.SyncedEntity
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import java.util.concurrent.ConcurrentHashMap

object ClientSyncedEntityHandler {
    @JvmField
    val SYNCED_FRIENDLY_ENTITIES = hashMapOf<ResourceLocation, ConcurrentHashMap<Int, ClientSyncedEntity>>()

    @JvmField
    val SYNCED_HOSTILE_ENTITIES = hashMapOf<ResourceLocation, ConcurrentHashMap<Int, ClientSyncedEntity>>()

    data class ClientSyncedEntity(val entity: Entity, val timeStamp: Int)

    fun sync(dim: ResourceLocation, list: List<SyncedEntity>, friendly: Boolean) {
        val player = localPlayer ?: return
        val level = mc.level ?: return
        val entities = if (friendly) {
            SYNCED_FRIENDLY_ENTITIES[dim] ?: ConcurrentHashMap<Int, ClientSyncedEntity>()
        } else {
            SYNCED_HOSTILE_ENTITIES[dim] ?: ConcurrentHashMap<Int, ClientSyncedEntity>()
        }
        val tick = player.tickCount
        for (syncedEntity in list) {
            val id = syncedEntity.id
            if (entities[id] != null) continue

            val type = BuiltInRegistries.ENTITY_TYPE.get(syncedEntity.type)
            val entity = type.create(level) ?: continue
            val tag = syncedEntity.tag as? CompoundTag ?: continue
            entity.load(tag)
            entity.id = syncedEntity.id

            val pos = syncedEntity.pos
            entity.xo = pos.x
            entity.yo = pos.y
            entity.zo = pos.z
            entity.setPos(syncedEntity.pos)
            entity.deltaMovement = syncedEntity.motion

            entities[id] = ClientSyncedEntity(entity, tick)
        }

        if (friendly) {
            SYNCED_FRIENDLY_ENTITIES[dim] = entities
        } else {
            SYNCED_HOSTILE_ENTITIES[dim] = entities
        }
    }

    fun clean(tick: Int) {
        SYNCED_FRIENDLY_ENTITIES.forEach { (_, map) ->
            map.entries.removeIf { tick - it.value.timeStamp > MiscConfig.CLIENT_SYNC_EXPIRE_TIME.get() }
        }
        SYNCED_HOSTILE_ENTITIES.forEach { (_, map) ->
            map.entries.removeIf { tick - it.value.timeStamp > MiscConfig.CLIENT_SYNC_EXPIRE_TIME.get() }
        }
    }

    @JvmStatic
    fun getSyncedFriendlyEntities(level: Level): List<Entity> {
        return SYNCED_FRIENDLY_ENTITIES[level.dimension().location()]?.map { it.value.entity }?.toList() ?: listOf()
    }

    @JvmStatic
    fun getSyncedHostileEntities(level: Level): List<Entity> {
        return SYNCED_HOSTILE_ENTITIES[level.dimension().location()]?.map { it.value.entity }?.toList() ?: listOf()
    }
}