package com.atsuishio.superbwarfare.resource

import com.atsuishio.superbwarfare.Mod
import com.google.gson.Gson
import com.google.gson.JsonElement
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.GsonHelper
import net.minecraft.util.profiling.ProfilerFiller

class JsonObjectReader<T>(val clazz: Class<T>, val gson: Gson, val converter: FileToIdConverter) :
    SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>>() {
    val jsonData = hashMapOf<ResourceLocation, T>()

    constructor(clazz: Class<T>, gson: Gson, path: String) : this(
        clazz,
        gson,
        FileToIdConverter.json(path)
    )

    override fun prepare(
        pResourceManager: ResourceManager,
        pProfiler: ProfilerFiller
    ): Map<ResourceLocation, JsonElement> {
        val output = hashMapOf<ResourceLocation, JsonElement>()
        for (entry in converter.listMatchingResources(pResourceManager).entries) {
            val name = entry.key
            val path = converter.fileToId(name)

            try {
                entry.value.openAsReader().use { reader ->
                    val json = GsonHelper.fromJson(gson, reader, JsonElement::class.java, true)
                    val json1 = output.put(path, json)
                    check(json1 == null) { "Duplicate file with ID $path" }
                }
            } catch (e: Exception) {
                Mod.LOGGER.error(
                    "Couldn't parse data file $path from $name",
                    e
                )
            }
        }
        return output
    }

    override fun apply(
        pObject: Map<ResourceLocation, JsonElement>,
        pResourceManager: ResourceManager,
        pProfiler: ProfilerFiller
    ) {
        jsonData.clear()

        for (entry in pObject.entries) {
            val key = entry.key
            val value = entry.value

            try {
                val data = this.gson.fromJson(value, this.clazz)
                if (data != null) {
                    this.jsonData[key] = data
                }
            } catch (e: Exception) {
                Mod.LOGGER.error("Error parsing json", e)
            }
        }
    }
}