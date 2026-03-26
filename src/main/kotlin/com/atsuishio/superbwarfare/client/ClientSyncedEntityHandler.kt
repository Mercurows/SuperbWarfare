package com.atsuishio.superbwarfare.client

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
    val SYNCED_ENTITIES = hashMapOf<ResourceLocation, ConcurrentHashMap<Int, ClientSyncedEntity>>()

    data class ClientSyncedEntity(val entity: Entity, val timeStamp: Int)

    fun sync(dim: ResourceLocation, list: List<SyncedEntity>) {
        val player = localPlayer ?: return
        val level = mc.level ?: return
        val entities = SYNCED_ENTITIES[dim] ?: ConcurrentHashMap<Int, ClientSyncedEntity>()
        val tick = player.tickCount
        entities.entries.removeIf { tick - it.value.timeStamp > 3 }

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

        SYNCED_ENTITIES[dim] = entities
    }

    @JvmStatic
    fun getEntitiesInLevel(level: Level): List<Entity>? {
        return SYNCED_ENTITIES[level.dimension().location()]?.map { it.value.entity }?.toList()
    }
}