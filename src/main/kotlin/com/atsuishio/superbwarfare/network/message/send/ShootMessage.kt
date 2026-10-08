package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.ActiveGun
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVector3f
import com.atsuishio.superbwarfare.tools.toVec3
import kotlinx.serialization.Serializable

@RegisterPacket
@Serializable
data class ShootMessage @JvmOverloads constructor(
    val spread: Double,
    val zoom: Boolean,
    val uuid: SerializedUUID?,
    val targetPos: SerializedVector3f?,
    val power: Double = 1.0,
    val direction: SerializedVector3f? = null
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val stack = ActiveGun.stackOf(player)
        if (stack.item !is GunItem) return

        // 客户端采到的"枪管此刻指向哪儿"（`ClientRenderHandler.muzzleDirection`，
        // 由 `GeoGunRenderer.submitMuzzleSample` 每帧刷新）。拿不到就是 null，
        // `GunItem.shoot` 会退回 `player.lookAngle` —— 服务端历来用的就是它。
        from(stack).shoot(player, spread, zoom, uuid, targetPos?.toVec3(), power, direction?.toVec3())
    }
}
