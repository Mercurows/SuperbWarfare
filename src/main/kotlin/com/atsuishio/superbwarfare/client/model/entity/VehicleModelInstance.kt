package com.atsuishio.superbwarfare.client.model.entity

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState

/**
 * A [com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance] subclass that pre-computes bone groups at construction time,
 * so the renderer can access them without any cache or per-frame regex matching.
 */
open class VehicleModelInstance(base: BakedBedrockModel) : BakedModelInstance(base) {
    val boneGroups: VehicleModelBoneGroups = VehicleModelBoneGroups.compute(this)

    private val numberedMountBones = mutableMapOf<String, List<BoneState>>()

    open fun getMountBones(name: String, multiple: Boolean): List<BoneState> {
        val base = getBone(name)
        if (!multiple && base != null) return listOf(base)

        val numbered = numberedMountBones.getOrPut(name) {
            boneIndexes.asSequence()
                .filter { it.name().startsWith(name) }
                .mapNotNull { bone ->
                    val index = bone.name().substring(name.length).toIntOrNull() ?: return@mapNotNull null
                    if (index > 0) index to bone else null
                }
                .sortedBy { it.first }
                .map { it.second }
                .toList()
        }

        if (numbered.isEmpty()) return listOfNotNull(base)
        return if (multiple) numbered else numbered.take(1)
    }
}
