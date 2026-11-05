package com.atsuishio.superbwarfare.config

import com.atsuishio.superbwarfare.config.server.GunConfig
import com.atsuishio.superbwarfare.network.message.receive.UnrestrictedModificationSyncMessage
import com.atsuishio.superbwarfare.tools.sendPacketTo
import com.atsuishio.superbwarfare.tools.sendPacketToAll
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.event.config.ModConfigEvent
import net.minecraftforge.server.ServerLifecycleHooks

/** 把 `unrestricted_modification` 运行期的改动推给在线玩家（含指令切换与配置文件重载）。 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.atsuishio.superbwarfare.Mod.MODID)
object UnrestrictedModificationSync {

    fun broadcast() {
        if (ServerLifecycleHooks.getCurrentServer() == null || !SERVER_CONFIG.isLoaded) return
        sendPacketToAll(UnrestrictedModificationSyncMessage(GunConfig.UNRESTRICTED_MODIFICATION.get()))
    }

    @SubscribeEvent
    fun onPlayerLoggedIn(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return
        if (!SERVER_CONFIG.isLoaded) return
        // 握手后再发一次当前值，覆盖握手期间管理员切换过配置的情况
        sendPacketTo(player, UnrestrictedModificationSyncMessage(GunConfig.UNRESTRICTED_MODIFICATION.get()))
    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(
        modid = com.atsuishio.superbwarfare.Mod.MODID,
        bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD
    )
    object ConfigEvents {
        @SubscribeEvent
        fun onReload(event: ModConfigEvent.Reloading) {
            if (event.config.getSpec<ForgeConfigSpec>() !== SERVER_CONFIG) return
            val server = ServerLifecycleHooks.getCurrentServer() ?: return
            // 文件监听器可能从其它线程触发重载，发包与取值都交回服务端线程
            server.execute { broadcast() }
        }
    }
}
