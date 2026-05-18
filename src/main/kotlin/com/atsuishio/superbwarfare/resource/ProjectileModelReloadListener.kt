package com.atsuishio.superbwarfare.resource

import com.atsuishio.superbwarfare.Mod
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
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
object ProjectileModelReloadListener : BedrockModelReloadListener<BedrockModel>(
    "models/bedrock/projectile",
    "animations/projectile"
) {
    override fun apply(
        map: Map<ResourceLocation, BedrockModelPOJO>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        super.apply(map, resourceManager, profiler)
        map.forEach { (location, pojo) ->
            this.models[location] = BedrockModel(pojo)
        }
        this.animFiles.forEach { (location, file) ->
            val model = this.models[location] ?: return@forEach
            this.animations[location] = BedrockAnimation.createAnimation(file, model)
        }
        this.animFiles.clear()
    }

    @SubscribeEvent
    fun onAddClientResourceListener(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(ProjectileModelReloadListener)
    }
}