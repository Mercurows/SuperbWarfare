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
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.event.config.ModConfigEvent
import net.minecraftforge.server.ServerLifecycleHooks
import kotlin.reflect.KProperty0

@EventBusSubscriber(modid = Mod.MODID)
object ServerConfigSync {
    private val packetFactories = linkedMapOf<KProperty0<*>, () -> ClientPacketPayload>()

    init {
        register(GunConfig::UNRESTRICTED_MODIFICATION, ::UnrestrictedModificationSyncMessage)
        register(MiscConfig::REALISTIC_SHOOT_MODE, ::RealisticShootModeSyncMessage)
    }

    private fun <T> register(config: KProperty0<ForgeConfigSpec.ConfigValue<T>>, packet: (T) -> ClientPacketPayload) {
        packetFactories[config] = { packet(config.get().get()) }
    }

    fun broadcast(config: ForgeConfigSpec.ConfigValue<*>) {
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

    @EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD)
    object ConfigEvents {
        @SubscribeEvent
        fun onReload(event: ModConfigEvent.Reloading) {
            if (event.config.getSpec<ForgeConfigSpec>() === SERVER_CONFIG) broadcastAll()
        }
    }
}
