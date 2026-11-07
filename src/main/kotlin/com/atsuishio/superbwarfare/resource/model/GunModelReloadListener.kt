package com.atsuishio.superbwarfare.resource.model

import com.atsuishio.superbwarfare.client.model.gun.GeoGunModel
import com.atsuishio.superbwarfare.client.renderer.gun.MuzzleFlashRenderer
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller

object GunModelReloadListener : BedrockModelReloadListener<GeoGunModel>(
    "models/bedrock/gun",
    "animations/bedrock/gun"
) {
    override fun apply(
        map: Map<ResourceLocation, BedrockModelPOJO>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        MuzzleFlashRenderer.clear()
        this.models.clear()
        this.animations.clear()

        map.forEach { (location, pojo) ->
            val baseModel = TreeBedrockModel.bake(pojo)
            this.models[location] = GeoGunModel(baseModel)
        }

        this.animFiles.forEach { (location, file) ->
            val id = this.animPathToIds[location] ?: return@forEach
            val path = this.idToModelPaths[id] ?: return@forEach
            val model = this.models[path]?.baseModel ?: return@forEach
            this.animations[location] = BedrockAnimation.createAnimation(file, model)
        }
        this.animFiles.clear()
    }
}
