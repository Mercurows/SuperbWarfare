package com.atsuishio.superbwarfare.data

import com.atsuishio.superbwarfare.Mod
import net.minecraft.resources.FileToIdConverter
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import javax.annotation.ParametersAreNonnullByDefault

class ComplexJsonResourceReloadListener(private val data: MutableMap<String, DataLoader.GeneralData<*>>) :
    SimplePreparableReloadListener<Any>() {

    @ParametersAreNonnullByDefault
    override fun prepare(resourceManager: ResourceManager, profiler: ProfilerFiller): Any {
        this.data.forEach { (name, value) ->
            val map = value.dataMap
            map.clear()

            val converter = FileToIdConverter.json(name)
            for (entry in converter.listMatchingResources(resourceManager).entries) {
                val location = entry.key
                val pathLocation = converter.fileToId(location)

                try {
                    entry.value.openAsReader().use { reader ->
                        val data = DataLoader.GSON.fromJson(reader, value.type)
                        val id = pathLocation.toString()

                        if (data is IDBasedData<*>) {
                            data.id = id
                        }
                        map.put(id, data)
                    }
                } catch (exception: Exception) {
                    Mod.LOGGER.error("Couldn't parse data file {} from {}", pathLocation, location, exception)
                }
            }

            value.onReload?.accept(map)
        }

        return NULL
    }

    @ParametersAreNonnullByDefault
    override fun apply(obj: Any, resourceManager: ResourceManager, profiler: ProfilerFiller) {
    }

    companion object {
        private val NULL = Any()
    }
}
