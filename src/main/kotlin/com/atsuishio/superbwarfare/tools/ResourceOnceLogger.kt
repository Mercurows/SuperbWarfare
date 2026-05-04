package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.Mod
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent
import org.apache.logging.log4j.Logger
import java.util.function.Consumer

// 仅在客户端资源重载时记录一次的Logger
class ResourceOnceLogger {
    private val logged = HashSet<Any>()

    init {
        LOGGERS.add(this)
    }

    fun log(obj: Any, logger: Consumer<Logger>) {
        if (logged.contains(obj)) {
            return
        }
        logged.add(obj)
        logger.accept(Mod.LOGGER)
    }

    internal class ReloadListener : ResourceManagerReloadListener {
        override fun onResourceManagerReload(resourceManager: ResourceManager) {
            LOGGERS.forEach { it?.logged?.clear() }
        }
    }

    @EventBusSubscriber(modid = Mod.MODID)
    companion object {
        private val INSTANCE = ReloadListener()
        private val LOGGERS = ArrayList<ResourceOnceLogger?>()

        @SubscribeEvent
        fun onRegisterReloadListeners(event: RegisterClientReloadListenersEvent) {
            event.registerReloadListener(INSTANCE)
        }
    }
}