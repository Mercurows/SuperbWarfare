package com.atsuishio.superbwarfare.resource.model

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller

object VehicleLODModelReloadListener : BedrockModelReloadListener<BedrockVehicleModel>("models/bedrock/vehicle_lod") {
    override fun apply(
        map: Map<ResourceLocation, BedrockModelPOJO>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        super.apply(map, resourceManager, profiler)
        map.forEach { (location, pojo) ->
            val model = BedrockVehicleModel(pojo)
            model.init()
            this.models[location] = model
        }
    }
}