package com.atsuishio.superbwarfare.resource.vehicle

import com.atsuishio.superbwarfare.data.IDBasedData
import com.atsuishio.superbwarfare.data.ObjectToList
import com.atsuishio.superbwarfare.resource.ModelResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class DefaultVehicleResource : IDBasedData<DefaultVehicleResource> {
    @Transient
    @kotlin.jvm.Transient
    private var id = ""

    override fun getId(): String {
        return this.id
    }

    override fun setId(id: String) {
        this.id = id
    }

    @SerialName("Model")
    private val model: ModelResource? = null

    fun getModel() = this.model ?: ModelResource()

    @SerialName("LODDistance")
    var lodDistance: ObjectToList<Double> = ObjectToList(48.0, 96.0)
}
