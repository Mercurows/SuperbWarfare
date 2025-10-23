package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class OnceLogger implements ResourceManagerReloadListener {
    private static final OnceLogger INSTANCE = new OnceLogger();
    private static final List<OnceLogger> LOGGERS = new ArrayList<>();
    private final Set<Object> logged = new HashSet<>();

    public OnceLogger() {
        LOGGERS.add(this);
    }

    public void log(Object obj, Consumer<Logger> logger) {
        if (logged.contains(obj)) {
            return;
        }
        logged.add(obj);
        logger.accept(Mod.LOGGER);
    }

    @SubscribeEvent
    static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        LOGGERS.forEach(l -> l.logged.clear());
    }

}
