package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.config.SERVER_CONFIG
import com.atsuishio.superbwarfare.config.server.GunConfig
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Serializable

/**
 * 把服务端的 `unrestricted_modification` 同步到客户端。
 *
 * 与 [RealisticShootModeSyncMessage] 同一套路：Forge 的 SERVER 配置只在登录握手时同步一份快照，
 * 运行期用指令改过的值不会自动传到已经在线（或本地配置不同）的客户端，而改装界面需要读它来决定
 * 哪些配件能装、模组槽位是否全开。这里只覆盖 Forge 已加载的配置副本，不写入客户端磁盘。
 */
@RegisterPacket
@Serializable
data class UnrestrictedModificationSyncMessage(val enabled: Boolean) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        // 单人游戏客户端与内置服务端共享同一份配置对象，旧包不应覆盖服务端的新值
        if (get().networkManager.isMemoryConnection || !SERVER_CONFIG.isLoaded) return

        GunConfig.UNRESTRICTED_MODIFICATION.set(enabled)
    }
}
