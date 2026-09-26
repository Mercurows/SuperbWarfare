package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.gun.SubWeaponClientHandler
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime
import com.atsuishio.superbwarfare.tools.localPlayer
import kotlinx.serialization.Serializable

/**
 * 副武器**真的打出了一发**（服务端 → 射手）。
 *
 * 「这一发到底打没打出去」只有服务端知道（`SubWeaponFireMessage` 里那次 `canShoot`），
 * 而副武器的表现全在客户端：开火动画、枪口焰、枪口烟。所以这里与 1P 开火音走**同一条口径** ——
 * 服务端在真的开火之后才发这个报文，客户端收到才播动画。
 *
 * 早先是客户端在按下 G 的那一刻自己播的：动画没有判定兜底，于是"副武器装填期间按 G"
 * 也会演一遍开火动画（音效早就是这样修掉的，动画漏了）。现在服务端没打出去就不会有这个报文，
 * 这一整类"空演"在结构上不可能发生。
 *
 * 槽位用 `AttachmentType` 的枚举名（`SUBWEAPON`），与 `SubWeaponFireMessage.slots` 对称。
 */
@RegisterPacket
@Serializable
data class SubWeaponFiredMessage(
    /** 打出去的那个副武器槽位 */
    val slot: String,
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        val player = localPlayer ?: return

        val stack = player.mainHandItem
        if (!GunItem.isHeldWeapon(stack)) return

        // 报文回来时手上可能已经不是那把枪了（换枪/丢枪/卸配件）：找不到这个槽位就什么都不播
        val instance = SubWeaponRuntime.find(GunData.from(stack), slot, client = true) ?: return

        SubWeaponClientHandler.playFireAnimation(player, instance)
    }
}
