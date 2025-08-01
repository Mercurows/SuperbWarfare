package com.atsuishio.superbwarfare.client.language;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
