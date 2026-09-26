package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.debug.DataDump
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Serializable
import net.minecraft.Util

/**
 * `/sbw data dump` 导出完成后，自动用系统文件管理器打开导出目录。
 *
 * 路径由服务端给出（只有服务端知道自己写到了哪），客户端拿到后**先确认这个目录在本机真的存在**：
 * 集成服务器里客户端与服务端共用同一个 gameDir，所以打得开；专用服务端上写的是服务器那台机器的路径，
 * 客户端本地没有，跳过 —— 不会去开一个不存在的目录。
 *
 * 「点击打开」那边见 [com.atsuishio.superbwarfare.client.DataDumpChatHandler]，
 * 它负责把聊天栏里的路径变成可点击的链接（`OPEN_FILE` 不允许从服务端发，只能在客户端挂）。
 */
@Serializable
@RegisterPacket
data class OpenFolderMessage(val path: String) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        // 只认自己算出来的那个目录，不信任消息里的路径：谁知道对面发过来的是什么东西
        val directory = DataDump.dumpDirectory()
        if (!directory.isDirectory || directory.absolutePath != path) return

        try {
            // 交给系统默认程序：Windows 资源管理器 / macOS 访达 / Linux xdg-open
            Util.getPlatform().openFile(directory)
        } catch (exception: Exception) {
            // 没有桌面环境（无头客户端）之类，记日志就行，不要在客户端抛出去
            Mod.LOGGER.warn("[DataDump] failed to open folder {}", directory, exception)
        }
    }
}
