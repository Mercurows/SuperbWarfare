package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.network.message.receive.ClientMotionSyncMessage
import com.atsuishio.superbwarfare.tools.sendPacketToTrackingThis
import net.minecraft.world.entity.Entity

/**
 * 高速移动同步接口 — 标记一个投射物实体能够突破原版速度上限，
 * 并通过网络包将自身动量同步到客户端。
 *
 * 替换原来的 CustomSyncMotionEntity 薄接口，
 * 提供 [syncMotion] 的默认实现。
 */
interface IFastMotionSync {
    /**
     * 是否应当同步动量到客户端
     */
    fun shouldSyncMotion(): Boolean = true

    /**
     * 同步动量的间隔（tick），-1 表示每 tick 都同步，0 表示不同步
     */
    fun syncMotionInterval(): Int = -1

    /**
     * 是否为高速移动状态
     */
    fun isFastMoving(): Boolean = true

    /**
     * 将当前动量通过网络包同步到所有追踪此实体的客户端。
     * 默认实现：服务端每 [syncMotionInterval] tick 发送一次 [ClientMotionSyncMessage]。
     */
    fun syncMotion() {
        val self = this as? Entity ?: return
        if (self.level().isClientSide) return
        if (!shouldSyncMotion()) return

        val interval = syncMotionInterval()
        if (interval > 0 && self.tickCount % interval == 0) {
            sendPacketToTrackingThis(ClientMotionSyncMessage(self))
        } else if (interval < 0) {
            sendPacketToTrackingThis(ClientMotionSyncMessage(self))
        }
    }
}
