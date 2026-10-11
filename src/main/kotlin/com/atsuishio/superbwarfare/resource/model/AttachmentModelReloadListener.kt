package com.atsuishio.superbwarfare.resource.model

import com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller

object AttachmentModelReloadListener : BedrockModelReloadListener<BedrockAttachmentModel>(
    "models/bedrock/attachment",
    "animations/bedrock/attachment"
) {
    override fun apply(
        map: Map<ResourceLocation, BedrockModelPOJO>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        this.models.clear()
        this.animations.clear()

        map.forEach { (location, pojo) ->
            this.models[location] = BedrockAttachmentModel(TreeBedrockModel.bake(pojo))
        }

        this.animFiles.forEach { (location, file) ->
            val id = this.animPathToIds[location] ?: return@forEach
            val path = this.idToModelPaths[id] ?: return@forEach
            val model = this.models[path]?.baseModel ?: return@forEach
            this.animations[id] = BedrockAnimation.createAnimation(file, model)
        }
        this.animFiles.clear()
    }

    @JvmStatic
    fun findAnimation(clipName: String): BedrockAnimation? {
        val path = clipName.removePrefix("animation.")
        val fileId = path.substringBefore('.')
        val namespace = if (fileId.contains(':')) fileId.substringBefore(':') else "superbwarfare"
        val idPath = fileId.substringAfter(':')
        val id = ResourceLocation.tryParse("$namespace:$idPath") ?: return null

        return this.animations[id]?.firstOrNull { it.name == clipName }
    }
}
