package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.debug.DataDump
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/**
 * 把 `/sbw data dump` 输出里的导出目录变成**可点击打开**的链接。
 *
 * 为什么要绕到客户端来做：`ClickEvent.Action.OPEN_FILE` 的 `allowFromServer` 是 **false**
 * （见 `net.minecraft.network.chat.ClickEvent.Action` 的枚举构造参数），服务端一序列化就会被
 * `filterForSerialization` 丢掉 —— 服务端压根发不出这个点击事件。
 * 而 [ClientChatReceivedEvent] 在消息进入聊天栏**之前**触发，`setMessage` 能直接换掉最终显示的内容，
 * 于是这里能干净地做到"一行绿字 + 可点击路径"，不必事后去改聊天记录。
 *
 * 目录**由客户端自己算**（集成服务器里客户端与服务端共用同一个 gameDir），并要求它真的存在：
 * 专用服务端上写的是服务器那台机器的路径，客户端本地没有这个目录就不挂点击事件 —— 不给假链接。
 */
@Mod.EventBusSubscriber(Dist.CLIENT)
object DataDumpChatHandler {

    private const val PATH_KEY = "commands.superbwarfare.data.path"
    private const val PATH_HOVER_KEY = "commands.superbwarfare.data.path.hover"

    @SubscribeEvent
    fun onSystemChat(event: ClientChatReceivedEvent.System) {
        // 本地没有导出目录（专用服务端的路径）就什么都不做
        val directory = DataDump.dumpDirectory()
        if (!directory.isDirectory) return

        val path = directory.absolutePath
        if (!event.message.string.contains(path)) return

        val click = ClickEvent(ClickEvent.Action.OPEN_FILE, path)
        val hover = HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(PATH_HOVER_KEY))

        event.message = attach(event.message, path, click, hover)
    }

    /**
     * 递归找到路径所在的那个组件，把点击/悬停挂上去，其余节点原样保留。
     *
     * 只挂在**那一段**上（而不是整行）的原因：整行是 `empty + 绿色成功提示 + " " + §7[路径]`，
     * 在根节点上挂点击会被子节点自己的 style 遮掉 —— 绿色那段点了没反应、只有路径那段能用。
     * 挂到路径节点上，既保留它原来的灰色，点击范围也正好是看得见的那段。
     */
    private fun attach(
        component: Component,
        path: String,
        click: ClickEvent,
        hover: HoverEvent
    ): Component {
        if (isPathText(component, path)) {
            return component.copy().withStyle { style ->
                style.withClickEvent(click).withHoverEvent(hover)
            }
        }

        if (component.siblings.isEmpty()) return component

        return component.copy().also { root ->
            root.siblings.replaceAll { sibling -> attach(sibling, path, click, hover) }
        }
    }

    /** 这个组件是不是那条路径文本本身 */
    private fun isPathText(component: Component, path: String): Boolean =
        (component.contents as? TranslatableContents)?.key == PATH_KEY && component.string.contains(path)
}
