package com.atsuishio.superbwarfare.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModMismatchEvent;
import org.apache.maven.artifact.versioning.ArtifactVersion;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModVersionEventHandler {

    public static ArtifactVersion previousVersion;
    public static ArtifactVersion currentVersion;

    @SubscribeEvent
    public static void onModMismatch(ModMismatchEvent event) {
        previousVersion = event.getPreviousVersion(com.atsuishio.superbwarfare.Mod.MODID);
        currentVersion = event.getCurrentVersion(com.atsuishio.superbwarfare.Mod.MODID);
    }
}
