package com.atsuishio.superbwarfare.resource

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.model.HandedBedrockModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller

object ModResources : SimplePreparableReloadListener<Unit>() {
    val TASER_MODEL = loc("item/taser")
    val TASER_ANIMATION = loc("item/taser.animation")
    lateinit var taserItemModel: HandedBedrockModel
    lateinit var taserItemAnimation: Map<String, BedrockAnimation>

    override fun prepare(
        pResourceManager: ResourceManager,
        pProfiler: ProfilerFiller
    ) {
    }

    override fun apply(
        pObject: Unit,
        pResourceManager: ResourceManager,
        pProfiler: ProfilerFiller
    ) {
        this.taserItemModel = ModResourceManager.getModel(TASER_MODEL).map {
            HandedBedrockModel(it, null)
        }.orElseThrow { IllegalStateException("Mod resource not found") }
        this.taserItemAnimation = ModResourceManager.getAnimation(TASER_ANIMATION).map { pojo ->
            BedrockAnimation.createAnimation(pojo, this.taserItemModel).associateBy { animation -> animation.name }
        }.orElseThrow { IllegalStateException("Mod resource not found") }
    }
}