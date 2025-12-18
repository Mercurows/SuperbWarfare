package com.atsuishio.superbwarfare.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.ModMismatchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.maven.artifact.versioning.ArtifactVersion;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModVersionEventHandler {

    public static ArtifactVersion previousVersion;
    public static ArtifactVersion currentVersion;

    @SubscribeEvent
    public static void onModMismatch(ModMismatchEvent event) {
        previousVersion = event.getPreviousVersion(com.atsuishio.superbwarfare.Mod.MODID);
        currentVersion = event.getCurrentVersion(com.atsuishio.superbwarfare.Mod.MODID);
    }
}
