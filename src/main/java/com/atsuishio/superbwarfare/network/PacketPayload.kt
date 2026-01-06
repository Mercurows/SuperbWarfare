package com.atsuishio.superbwarfare.network

import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

typealias PayloadContext = Supplier<NetworkEvent.Context>

abstract class PacketPayload {
    fun handleInternal(message: PacketPayload, context: PayloadContext, dist: Dist) {
        with(context.get()) {
            enqueueWork {
                // TODO 这样能不能隔离？
                DistExecutor.unsafeRunWhenOn(dist) {
                    DistExecutor.SafeRunnable { with(message) { context.handler() } }
                }
            }
            packetHandled = true
        }
    }

    abstract fun PayloadContext.handler()
}

abstract class ServerPacketPayload : PacketPayload() {
    fun PayloadContext.sender() = get().sender as ServerPlayer
}

// abstract class ClientPacketPayload : PacketPayload()