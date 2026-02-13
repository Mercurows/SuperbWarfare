package com.atsuishio.superbwarfare.data.loot

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.tools.toKxJson
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.entity.EntityType
import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.registries.ForgeRegistries

@EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE)
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
                val json = Json.decodeFromJsonElement<WreckageLootData>(obj.toKxJson())
                data[id] = json
            } catch (_: Exception) {
                Mod.LOGGER.error("Failed to load wreckage loot data for {}", id)
            }
        }
    }

    fun getLootData(id: ResourceLocation): WreckageLootData? {
        return data[id]
    }

    fun getLootData(type: EntityType<*>): WreckageLootData? {
        return data[ForgeRegistries.ENTITY_TYPES.getKey(type)]
    }

    @SubscribeEvent
    fun onAddReloadListeners(event: AddReloadListenerEvent) {
        event.addListener(this)
    }
}