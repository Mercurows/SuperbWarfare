package com.atsuishio.superbwarfare.resource

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.*
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.serialize.AnimationKeyframesSerializer
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.serialize.SoundEffectKeyframesSerializer
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.serialize.Vector3fSerializer
import com.google.gson.GsonBuilder
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent
import org.joml.Vector3f
import java.util.*
import java.util.function.Consumer

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ModResourceManager {
    private val GSON = GsonBuilder()
        .setLenient()
        .registerTypeAdapter(CubesItem::class.java, CubesItem.Deserializer())
        .registerTypeAdapter(Vector3f::class.java, Vector3fSerializer())
        .registerTypeAdapter(AnimationKeyframes::class.java, AnimationKeyframesSerializer())
        .registerTypeAdapter(SoundEffectKeyframes::class.java, SoundEffectKeyframesSerializer())
        .create()

    lateinit var models: JsonObjectReader<BedrockModelPOJO>
    lateinit var animations: JsonObjectReader<BedrockAnimationFile>
    val modResources = ModResources

    fun registerListeners(consumer: Consumer<PreparableReloadListener>) {
        this.models =
            JsonObjectReader(BedrockModelPOJO::class.java, GSON, "models/bedrock")
        this.animations =
            JsonObjectReader(BedrockAnimationFile::class.java, GSON, "animations/bedrock")
        consumer.accept(this.models)
        consumer.accept(this.animations)
        consumer.accept(this.modResources)
    }

    @SubscribeEvent
    fun onRegisterClientReloadListener(event: RegisterClientReloadListenersEvent) {
        registerListeners(event::registerReloadListener)
    }

    fun getModels(): Map<ResourceLocation, BedrockModelPOJO> {
        return this.models.jsonData
    }

    fun getModel(id: ResourceLocation): Optional<BedrockModelPOJO> {
        return this.models.jsonData[id]?.let { Optional.ofNullable(it) } ?: Optional.empty()
    }

    fun getAnimations(): Map<ResourceLocation, BedrockAnimationFile> {
        return this.animations.jsonData
    }

    fun getAnimation(id: ResourceLocation): Optional<BedrockAnimationFile> {
        return this.animations.jsonData[id]?.let { Optional.ofNullable(it) } ?: Optional.empty()
    }
}