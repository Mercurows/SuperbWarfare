package com.atsuishio.superbwarfare.client.language

import net.minecraft.client.resources.language.ClientLanguage
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
object ClientLanguageGetter {
    @JvmStatic
    lateinit var EN_US: ClientLanguage

    @SubscribeEvent
    fun onResourcePackReload(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(object : SimplePreparableReloadListener<ClientLanguage>() {
            override fun prepare(
                pResourceManager: ResourceManager,
                pProfiler: ProfilerFiller
            ): ClientLanguage {
                return ClientLanguage.loadFrom(pResourceManager, listOf("en_us"), false)
            }

            override fun apply(
                pObject: ClientLanguage,
                pResourceManager: ResourceManager,
                pProfiler: ProfilerFiller
            ) {
                EN_US = pObject
            }
        })
    }
}
