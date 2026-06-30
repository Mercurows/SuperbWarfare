package com.atsuishio.superbwarfare.world.chunk

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.TicketType
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
object ChunkLoadManager {
    object ChunkLoader {
        val CHUNKS = hashMapOf<ChunkPos, Int>()
    }

    private val LOADERS = hashMapOf<ResourceLocation, ChunkLoader>()

    @JvmStatic
    fun updateEntityChunks(entity: Entity, positions: Collection<Vec3>) {
        val level = entity.level() as? ServerLevel ?: return
        val location = level.dimension().location()

        val loader = LOADERS.getOrPut(location) { ChunkLoader }
        val id = entity.id
        for (pos in positions) {
            val chunkPos = ChunkPos(BlockPos.containing(pos))
            loader.CHUNKS.putIfAbsent(chunkPos, id)
        }
        LOADERS[location] = loader
    }

    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        val server = event.server
        if (event.phase == TickEvent.Phase.START) return
        for (level in server.allLevels) {
            val loader = LOADERS[level.dimension().location()] ?: continue
            loader.CHUNKS.entries.forEach { (pos, id) ->
                level.chunkSource.addRegionTicket(
                    TicketType.POST_TELEPORT,
                    pos,
                    2,
                    id
                )
            }
            loader.CHUNKS.clear()
        }
    }
}