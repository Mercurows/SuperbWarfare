package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.command.MeleeDebugHooks.install
import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.world.entity.player.Player

/**
 * `/sbw melee force <index>` 的**客户端实现挂点**。
 *
 * 判定只在客户端做（§4.1），而 `/sbw` 命令是服务端注册的（仓库里没有
 * `RegisterClientCommandsEvent`），所以命令本身只能把请求转交到客户端执行。
 * 客户端在初始化时通过 [install] 装上真正的实现（见 `MeleeClientHandler.installDebugHooks`）；
 * 专用服务端上没装，命令会明确报错而不是静默失败。
 *
 * 做成"可替换的函数字段"而不是直接 import 客户端类，是为了不让服务端代码依赖客户端类。
 */
object MeleeDebugHooks {

    /** 未安装实现时为 `null`，由调用方给出提示 */
    @JvmField
    var forceSwingHandler: ((Player, GunData, Int) -> Unit)? = null

    @JvmStatic
    fun install(handler: (Player, GunData, Int) -> Unit) {
        forceSwingHandler = handler
    }

    /** 当前环境是否具备客户端实现 */
    @JvmStatic
    val isAvailable: Boolean get() = forceSwingHandler != null

    /** @return 是否真的触发了挥击（false = 当前环境没有客户端实现） */
    @JvmStatic
    fun forceSwing(player: Player, data: GunData, index: Int): Boolean {
        val handler = forceSwingHandler ?: return false
        handler(player, data, index)
        return true
    }
}
