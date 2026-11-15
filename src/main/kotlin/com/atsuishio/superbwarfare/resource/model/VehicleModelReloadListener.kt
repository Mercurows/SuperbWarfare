package com.atsuishio.superbwarfare.resource.model

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakerOptions
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller

object VehicleModelReloadListener : BasicModelReloadListener("vehicle") {
    @JvmStatic
    val PATTERNS = setOf(
        "^w_.*",        // wheel bones: w_lb, w_rb, w_lr, w_rr
        "^root$",       // root bone
        "^move_.*",     // bones moved by scripts
        "^flare.*",     // flare bones
        "^laser.*",     // laser bones
        "^wheel[LR].*", // alternative wheel naming
        "^track.*",     // track bones
        "^shell.*",     // shell bones
        "^.*_dogTag_(?<w>\\d+)x(?<h>\\d+)$",
        "^dummy_.*",
        "^waterMask$",
        "^passengerWeaponStation$",
        "^base$",
        "^turret$",
        "^barrel$",
        "^passengerWeaponStationYaw$",
        "^passengerWeaponStationPitch$",
        "^.*camera.*",
    )

    override fun apply(
        map: Map<ResourceLocation, BedrockModelPOJO>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        this.models.clear()
        this.animations.clear()

        map.forEach { (location, pojo) ->
            // 从 model path 反查 id，再反查 animation path
            val id = this.idToModelPaths.entries.firstOrNull { it.value == location }?.key
            val animPath = id?.let { i -> this.animPathToIds.entries.firstOrNull { it.value == i }?.key }
            val anim = animPath?.let { this.animFiles[it] }
            val options = if (anim != null) {
                BakerOptions.ofAnimationFile(anim)
            } else {
                BakerOptions.defaults()
            }.withPreservedBoneRegexes(PATTERNS)

            this.models[location] = BakedBedrockModel.bake(pojo, options)
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
