package com.atsuishio.superbwarfare.world.saveddata

import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.TicketType
import net.minecraft.util.Unit
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.saveddata.SavedData
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

class ChunkPosSavedData : SavedData() {
    val chunkPositions = mutableSetOf<ChunkPos>()

    override fun save(tag: CompoundTag): CompoundTag {
        tag.put("Pos", this.savePos())
        return tag
    }

    fun savePos(): ListTag {
        val tag = ListTag()
        for (pos in chunkPositions) {
            tag.add(CompoundTag().also {
                it.putInt("X", pos.x)
                it.putInt("Z", pos.z)
            })
        }
        return tag
    }

    fun loadPos(tag: ListTag) {
        val list = mutableListOf<ChunkPos>()
        for (t in tag.indices) {
            val pos = tag[t] as? CompoundTag ?: continue
            list.add(ChunkPos(pos.getInt("X"), pos.getInt("Z")))
        }
        this.chunkPositions.addAll(list)
    }

    fun clearPos() {
        this.chunkPositions.clear()
    }

    @Mod.EventBusSubscriber
    companion object {
        const val FILE_ID: String = "superbwarfare_chunk_pos"

        fun load(tag: CompoundTag): ChunkPosSavedData {
            val savedData = ChunkPosSavedData()
            if (tag.contains("Pos", Tag.TAG_LIST.toInt())) {
                savedData.loadPos(tag.getList("Pos", Tag.TAG_COMPOUND.toInt()))
            }
            return savedData
        }

        @SubscribeEvent
        fun posSavedDataOnServerStarted(event: ServerStartedEvent) {
            val server = event.server
            if (!VehicleConfig.VEHICLE_CHUNK_LOADING.get()) return

            for (level in server.allLevels) {
                val data = level.dataStorage.get({ load(it) }, FILE_ID) ?: continue
                val posSet = data.chunkPositions
                if (posSet.isEmpty()) continue

                for (pos in posSet) {
                    level.chunkSource.addRegionTicket(TicketType.START, pos, 3, Unit.INSTANCE)
                }

                data.clearPos()
                data.setDirty()
            }
        }

        @SubscribeEvent
        fun posSavedDataOnServerStopping(event: ServerStoppingEvent) {
            val server = event.server
            if (!VehicleConfig.VEHICLE_CHUNK_LOADING.get()) return

            for (level in server.allLevels) {
                val data = level.dataStorage.computeIfAbsent(
                    { load(it) },
                    { ChunkPosSavedData() },
                    FILE_ID
                ) ?: continue

                val list = level.allEntities
                    .asSequence()
                    .filter { it is VehicleEntity && it.computed().keepChunkLoaded }
                    .map { it.chunkPosition() }
                    .toList()
                if (list.isEmpty()) continue

                data.chunkPositions.addAll(list)
                data.setDirty()
            }
        }
    }
}