package com.atsuishio.superbwarfare.config

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.server.GunConfig
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.message.receive.RealisticShootModeSyncMessage
import com.atsuishio.superbwarfare.network.message.receive.UnrestrictedModificationSyncMessage
import com.atsuishio.superbwarfare.tools.sendPacketTo
import com.atsuishio.superbwarfare.tools.sendPacketToAll
import net.minecraft.server.level.ServerPlayer
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.neoforge.common.ModConfigSpec
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.server.ServerLifecycleHooks
import kotlin.reflect.KProperty0

@EventBusSubscriber(modid = Mod.MODID)
object ServerConfigSync {
    private val packetFactories = linkedMapOf<KProperty0<*>, () -> ClientPacketPayload>()

    init {
        register(GunConfig::UNRESTRICTED_MODIFICATION, ::UnrestrictedModificationSyncMessage)
        register(MiscConfig::REALISTIC_SHOOT_MODE, ::RealisticShootModeSyncMessage)
    }

    private fun <T> register(config: KProperty0<ModConfigSpec.ConfigValue<T>>, packet: (T) -> ClientPacketPayload) {
        packetFactories[config] = { packet(config.get().get()) }
    }

    fun broadcast(config: ModConfigSpec.ConfigValue<*>) {
        val server = ServerLifecycleHooks.getCurrentServer() ?: return
        server.execute {
            if (!SERVER_CONFIG.isLoaded) return@execute
            val factory = packetFactories.entries.firstOrNull { it.key.get() === config }?.value ?: return@execute
            sendPacketToAll(factory())
        }
    }

    fun broadcastAll() {
        val server = ServerLifecycleHooks.getCurrentServer() ?: return
        server.execute {
            if (!SERVER_CONFIG.isLoaded) return@execute
            packetFactories.values.forEach { sendPacketToAll(it()) }
        }
    }

    @SubscribeEvent
    fun onPlayerLoggedIn(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return
        if (!SERVER_CONFIG.isLoaded) return
        packetFactories.values.forEach { sendPacketTo(player, it()) }
    }

    @EventBusSubscriber(modid = Mod.MODID)
    object ConfigEvents {
        @SubscribeEvent
        fun onReload(event: ModConfigEvent.Reloading) {
            if (event.config.spec === SERVER_CONFIG) broadcastAll()
        }
    }
}
