package com.atsuishio.superbwarfare.resource.model

import com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.tree.TreeBedrockModel
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller

/**
 * 配件模型 + 配件动画的加载器。
 *
 * `animPath` 是**四期加上去的**（§9.8.7）：在此之前配件只有模型、没有动画，
 * 所以副武器的换弹动画只能做在宿主枪的动画文件里（"左手换弹"那条路）。
 * 现在副武器是一把独立的枪（有自己的 `GunData` / `GunResource`），
 * 它的换弹动画就该由**它自己的模型**播 —— 于是给它一个动画目录即可。
 *
 * **配对完全靠文件名 id**（基类 `prepare` 的 `animPathToIds` / `idToModelPaths`）：
 * `animations/bedrock/attachment/sub_weapon_gp_25.animation.json`
 * 自动绑到 `models/bedrock/attachment/sub_weapon_gp_25.geo.json`。
 * 两边缺一个就只是"那一半为空"，不会报错 —— 所以**动画还没做出来时这段是安全的空转**。
 */
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

        // 动画必须绑在**它自己那个模型的骨骼**上（`BedrockAnimation.createAnimation` 会按模型解析骨骼名），
        // 所以这里与 `GunModelReloadListener` 一样按文件名 id 找模型。
        this.animFiles.forEach { (location, file) ->
            val id = this.animPathToIds[location] ?: return@forEach
            val path = this.idToModelPaths[id] ?: return@forEach
            val model = this.models[path]?.baseModel ?: return@forEach
            this.animations[id] = BedrockAnimation.createAnimation(file, model)
        }
        this.animFiles.clear()
    }

    /**
     * 按 **clip 名**找配件动画。
     *
     * clip 名形如 `animation.sub_weapon_gp_25.reload`，它的**文件名 id** 就是
     * `sub_weapon_gp_25`（基类加载时把 `.animation` 后缀去掉了）—— 所以这里先剥掉
     * `animation.` 前缀、再取第一个 `.` 之前的那一段。
     *
     * 找不到就返回 `null`（**动画还没做出来时的正常路径**，调用方静默回退）。
     */
    fun findAnimation(clipName: String): BedrockAnimation? {
        val path = clipName.removePrefix("animation.")
        val fileId = path.substringBefore('.')
        val namespace = if (fileId.contains(':')) fileId.substringBefore(':') else "superbwarfare"
        val idPath = fileId.substringAfter(':')
        val id = ResourceLocation.tryParse("$namespace:$idPath") ?: return null

        return this.animations[id]?.firstOrNull { it.name == clipName }
    }
}
