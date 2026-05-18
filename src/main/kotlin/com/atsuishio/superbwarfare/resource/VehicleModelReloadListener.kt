package com.atsuishio.superbwarfare.resource

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.GsonHelper
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

@OnlyIn(Dist.CLIENT)
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(
    bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,
    modid = Mod.MODID,
    value = [Dist.CLIENT]
)
object VehicleModelReloadListener : BedrockModelReloadListener<BedrockVehicleModel>("models/bedrock/vehicle") {
    override fun prepare(
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ): Map<ResourceLocation, BedrockModelPOJO> {
        val map = hashMapOf<ResourceLocation, BedrockModelPOJO>()
        val converter = FileToIdConverter.json(this.path)

        for ((location, resource) in converter.listMatchingResources(resourceManager).entries) {
            val id = converter.fileToId(location)

            try {
                resource.openAsReader().use {
                    val pojo = GsonHelper.fromJson(this.gson, it, BedrockModelPOJO::class.java)
                    val existed = map.put(id, pojo)
                    if (existed != null) {
                        throw IllegalStateException("Duplicate resource $resource")
                    }
                }
            } catch (e: Exception) {
                Mod.LOGGER.error("Error while reading $resource", e)
            }
        }

        return map
    }

    override fun apply(
        map: Map<ResourceLocation, BedrockModelPOJO>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        this.data.clear()
        map.forEach { (location, pojo) ->
            val model = BedrockVehicleModel(pojo)
            model.init()
            this.data[location] = model
        }
    }

    @JvmStatic
    fun getModel(path: ResourceLocation): BedrockVehicleModel? = this.data[path]

    @SubscribeEvent
    fun onAddClientResourceListener(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(VehicleModelReloadListener)
    }
}