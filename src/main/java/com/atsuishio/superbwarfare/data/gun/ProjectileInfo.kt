package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.DeserializeFromString
import com.atsuishio.superbwarfare.data.IDBasedData
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedGsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ProjectileInfo : IDBasedData<ProjectileInfo>, DeserializeFromString {
    @SerializedName("Type")
    @SerialName("Type")
    var itemId: String = "superbwarfare:projectile"

    override fun getId() = itemId
    override fun setId(id: String) {
        this.itemId = id
    }

    @JvmField
    @SerializedName("Data")
    @SerialName("Data")
    var data: SerializedGsonObject? = null

    override fun deserializeFromString(str: String) {
        this.itemId = str
    }
}
