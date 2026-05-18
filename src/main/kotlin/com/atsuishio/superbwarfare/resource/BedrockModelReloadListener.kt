package com.atsuishio.superbwarfare.resource

import com.atsuishio.superbwarfare.Mod
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.GsonUtil
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockAnimationFile
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO
import com.google.gson.Gson
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.GsonHelper
import net.minecraft.util.profiling.ProfilerFiller

abstract class BedrockModelReloadListener<T> @JvmOverloads constructor(
    val modelPath: String,
    val animPath: String = ""
) : SimplePreparableReloadListener<Map<ResourceLocation, BedrockModelPOJO>>() {
    val gson: Gson = GsonUtil.CLIENT_GSON
    val models: MutableMap<ResourceLocation, T> = hashMapOf()
    val animFiles: MutableMap<ResourceLocation, BedrockAnimationFile> = hashMapOf()
    val animations: MutableMap<ResourceLocation, List<BedrockAnimation>> = hashMapOf()

    override fun prepare(
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ): Map<ResourceLocation, BedrockModelPOJO> {
        val map = hashMapOf<ResourceLocation, BedrockModelPOJO>()
        val modelConverter = FileToIdConverter.json(this.modelPath)

        for ((location, resource) in modelConverter.listMatchingResources(resourceManager).entries) {
            var id = modelConverter.fileToId(location)
            id = ResourceLocation(id.namespace, id.path.removeSuffix(".geo"))

            try {
                resource.openAsReader().use {
                    val pojo = GsonHelper.fromJson(this.gson, it, BedrockModelPOJO::class.java)
                    val existed = map.put(id, pojo)
                    if (existed != null) {
                        throw IllegalStateException("Duplicate model resource $resource")
                    }
                }
            } catch (e: Exception) {
                Mod.LOGGER.error("Error while reading model $resource", e)
            }
        }

        if (this.animPath.isNotEmpty()) {
            val animConverter = FileToIdConverter.json(this.animPath)

            for ((location, resource) in animConverter.listMatchingResources(resourceManager).entries) {
                var id = animConverter.fileToId(location)
                id = ResourceLocation(id.namespace, id.path.removeSuffix(".animation"))

                try {
                    resource.openAsReader().use {
                        val file = GsonHelper.fromJson(this.gson, it, BedrockAnimationFile::class.java)
                        val existed = this.animFiles.put(id, file)
                        if (existed != null) {
                            throw IllegalStateException("Duplicate animation resource $resource")
                        }
                    }
                } catch (e: Exception) {
                    Mod.LOGGER.error("Error while reading animation $resource", e)
                }
            }
        }

        return map
    }

    override fun apply(
        map: Map<ResourceLocation, BedrockModelPOJO>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        this.models.clear()
        this.animations.clear()
    }

    fun getModel(path: ResourceLocation): T? = this.models[path]

    fun getAnimation(path: ResourceLocation): List<BedrockAnimation>? = this.animations[path]
}