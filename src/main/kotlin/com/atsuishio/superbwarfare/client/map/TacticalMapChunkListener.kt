package com.atsuishio.superbwarfare.client.map

import com.atsuishio.superbwarfare.config.server.MapConfig
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.level.ChunkEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(value = [Dist.CLIENT])
object TacticalMapChunkListener {

    fun isEnabled(): Boolean {
        return try {
            MapConfig.ENABLE_TACTICAL_MAP.get()
        } catch (_: Exception) {
            false
        }
    }

    @SubscribeEvent
    fun onChunkLoad(event: ChunkEvent.Load) {
        if (!isEnabled()) return
        if (event.level !is ClientLevel) return
        val chunk = event.chunk
        if (chunk is LevelChunk) {
            TacticalMapCache.queueChunkUpdate(chunk)
        }
    }

    @SubscribeEvent
    fun onLevelLoad(event: LevelEvent.Load) {
        if (!isEnabled()) return
        if (event.level is ClientLevel) {
            val worldId = TacticalMapCache.getWorldIdentifier()
            val dim = (event.level as ClientLevel).dimension().location().toString()
            TacticalMapCache.initForDimension(dim, worldId)
        }
    }

    @SubscribeEvent
    fun onLevelUnload(event: LevelEvent.Unload) {
        // Always clear — config may already be inaccessible during world
        // teardown, and stale data bleeds into the next world otherwise.
        if (event.level is ClientLevel) {
            TacticalMapCache.clear()
        }
    }
}
