package com.atsuishio.superbwarfare.resource

import com.atsuishio.superbwarfare.data.ObjectToList
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import kotlin.math.min

@Serializable
class ModelResource {
    @JvmField
    @SerialName("Animation")
    var animation: SerializedResourceLocation? = null

    @JvmField
    @SerialName("Model")
    var model: SerializedResourceLocation? = null

    @SerialName("LODModel")
    private var lodModel: ObjectToList<SerializedResourceLocation>? = ObjectToList()

    fun hasLOD(): Boolean {
        return lodModel != null && !lodModel!!.list.isEmpty()
    }

    // LOD的最小等级为1
    fun getLODModel(level: Int): ResourceLocation? {
        if (level < 1 || lodModel == null || lodModel!!.list.isEmpty()) return model

        val availableLevel = min(level - 1, lodModel!!.list.size - 1)
        return lodModel!!.list.get(availableLevel)
    }

    @JvmField
    @SerialName("Texture")
    var texture: SerializedResourceLocation? = null

    @SerialName("LODTexture")
    private var lodTexture: ObjectToList<SerializedResourceLocation>? = null

    // LOD的最小等级为1
    fun getLODTexture(level: Int): ResourceLocation? {
        if (level < 1 || lodTexture == null || lodTexture!!.list.isEmpty()) return texture
        val availableLevel = min(level - 1, lodTexture!!.list.size - 1)
        return lodTexture!!.list[availableLevel]
    }
}
