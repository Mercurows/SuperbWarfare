package com.atsuishio.superbwarfare.resource.model

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.model.BedrockArmorModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller

object ArmorModelReloadListener : BedrockModelReloadListener<BedrockArmorModel>(
    "models/bedrock/armor",
    "animations/bedrock/armor"
) {
    override fun apply(
        map: Map<ResourceLocation, BedrockModelPOJO>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        super.apply(map, resourceManager, profiler)
        map.forEach { (location, pojo) ->
            this.models[location] = BedrockArmorModel(pojo)
        }
        this.animFiles.forEach { (location, file) ->
            val model = this.models[location] ?: return@forEach
            this.animations[location] = BedrockAnimation.createAnimation(file, model)
        }
        this.animFiles.clear()
    }
}