package com.atsuishio.superbwarfare.client.language;

import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = com.atsuishio.superbwarfare.Mod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientLanguageGetter {

    public static ClientLanguage EN_US;

    @SubscribeEvent
    public static void onResourcePackReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<ClientLanguage>() {
            @Override
            @ParametersAreNonnullByDefault
            protected @NotNull ClientLanguage prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                return ClientLanguage.loadFrom(pResourceManager, List.of("en_us"), false);
            }

            @Override
            @ParametersAreNonnullByDefault
            protected void apply(ClientLanguage pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                EN_US = pObject;
            }
        });
    }
}
