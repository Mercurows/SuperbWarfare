package com.atsuishio.superbwarfare.compat.kubejs

import com.atsuishio.superbwarfare.compat.kubejs.event.SbwKJSEventHandler
import dev.latvian.mods.kubejs.event.EventGroupRegistry
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS

class SbwKubeJSPlugin : KubeJSPlugin {
    override fun registerEvents(registry: EventGroupRegistry) {
        registry.register(SbwKJSEventHandler.GROUP)
        FORGE_BUS.register(SbwKJSEventHandler)
    }
}