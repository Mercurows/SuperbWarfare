package com.atsuishio.superbwarfare.resource.model

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller

open class BasicModelReloadListener(path: String) : BedrockModelReloadListener<BedrockModel>(
    "models/bedrock/$path",
    "animations/bedrock/$path"
) {
    override fun apply(
        map: Map<ResourceLocation, BedrockModelPOJO>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        super.apply(map, resourceManager, profiler)
        map.forEach { (location, pojo) ->
            this.models[location] = BedrockModel(pojo)
        }
        this.animFiles.forEach { (location, file) ->
            val id = this.animPathToIds[location] ?: return@forEach
            val path = this.idToModelPaths[id] ?: return@forEach
            val model = this.models[path] ?: return@forEach
            this.animations[location] = BedrockAnimation.createAnimation(file, model)
        }
        this.animFiles.clear()
    }
}