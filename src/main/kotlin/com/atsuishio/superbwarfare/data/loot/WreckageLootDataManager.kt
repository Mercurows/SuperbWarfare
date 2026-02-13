package com.atsuishio.superbwarfare.data.loot

import com.atsuishio.superbwarfare.Mod
import com.google.gson.Gson
import com.google.gson.JsonElement
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.AddReloadListenerEvent

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
object WreckageLootDataManager : SimpleJsonResourceReloadListener(Gson(), "sbw/loot") {
    private val data: MutableMap<ResourceLocation, WreckageLootData> = mutableMapOf()

    override fun apply(
        pObject: Map<ResourceLocation, JsonElement>,
        pResourceManager: ResourceManager,
        pProfiler: ProfilerFiller
    ) {
        data.clear()
        pObject.forEach { (id, json) ->
            try {
                val obj = json.asJsonObject
                val poolsList = mutableListOf<WreckageLootData.Pool>()
                val pools = obj.getAsJsonArray("Pools")
                for (pool in pools) {
                    val poolObj = pool.asJsonObject
                    val rolls = poolObj.get("Rolls").asInt
                    val source = poolObj.get("Source").asString
                    val entriesList = mutableListOf<WreckageLootData.Entry>()
                    val entries = poolObj.getAsJsonArray("Entries")
                    for (entry in entries) {
                        val entryObj = entry.asJsonObject
                        val name = entryObj.get("Name").asString
                        val count = entryObj.get("Count").asInt
                        val chance = entryObj.get("Chance").asDouble
                        entriesList.add(WreckageLootData.Entry(name, count, chance))
                    }
                    poolsList.add(WreckageLootData.Pool(entriesList, rolls, source))
                }
                data[id] = WreckageLootData(poolsList)
            } catch (_: Exception) {
                Mod.LOGGER.error("Failed to load wreckage loot data for {}", id)
            }
        }
    }

    fun getLootData(id: ResourceLocation): WreckageLootData? {
        return data[id]
    }

    @SubscribeEvent
    fun onAddReloadListeners(event: AddReloadListenerEvent) {
        event.addListener(this)
    }
}