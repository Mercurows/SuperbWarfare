package com.atsuishio.superbwarfare.event

import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.ModMismatchEvent
import org.apache.maven.artifact.versioning.ArtifactVersion

@EventBusSubscriber(Dist.CLIENT)
object ModVersionEventHandler {
    @JvmField
    var previousVersion: ArtifactVersion? = null

    @JvmField
    var currentVersion: ArtifactVersion? = null

    @SubscribeEvent
    fun onModMismatch(event: ModMismatchEvent) {
        previousVersion = event.getPreviousVersion(com.atsuishio.superbwarfare.Mod.MODID)
        currentVersion = event.getCurrentVersion(com.atsuishio.superbwarfare.Mod.MODID)
    }
}
