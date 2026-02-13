package com.atsuishio.superbwarfare.data.container

import com.atsuishio.superbwarfare.Mod
import com.google.gson.Gson
import com.google.gson.JsonElement
import it.unimi.dsi.fastutil.Pair
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber

@EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE)
object ContainerDataManager : SimpleJsonResourceReloadListener(Gson(), "sbw/containers") {
    private val containerData: MutableMap<ResourceLocation, MutableList<Pair<String, Int>>> = hashMapOf()

    override fun apply(
        pObject: MutableMap<ResourceLocation, JsonElement>,
        manager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        containerData.clear()
        pObject.forEach { (id, json) ->
            try {
                val obj = json.getAsJsonObject()
                val list: MutableList<Pair<String, Int>> = mutableListOf()
                val array = obj.getAsJsonArray("List")
                for (arr in array) {
                    if (arr.isJsonObject) {
                        val obj2 = arr.getAsJsonObject()
                        val type = obj2.get("Type").asString
                        val weight = obj2.get("Weight").asInt
                        list.add(Pair.of(type, weight))
                    } else {
                        list.add(Pair.of(arr.asString, 1))
                    }
                }
                containerData[id] = list
            } catch (_: Exception) {
                Mod.LOGGER.error("Failed to load container data for {}", id)
            }
        }
    }

    fun getEntityTypes(id: ResourceLocation): MutableList<Pair<String, Int>> {
        return containerData[id] ?: mutableListOf()
    }

    @SubscribeEvent
    fun onAddReloadListeners(event: AddReloadListenerEvent) {
        event.addListener(this)
    }
}
