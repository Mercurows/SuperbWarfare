package com.atsuishio.superbwarfare.event

import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.ModMismatchEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.apache.maven.artifact.versioning.ArtifactVersion

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
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
