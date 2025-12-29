package com.atsuishio.superbwarfare.network

import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

typealias PayloadContext = Supplier<NetworkEvent.Context>

abstract class PacketPayload<T : PacketPayload<T>> {
    fun handleInternal(message: T, context: PayloadContext, dist: Dist) {
        with(context.get()) {
            enqueueWork {
//                DistExecutor.unsafeRunWhenOn(dist) { Runnable { handler(message, context) } }

                // TODO 这样能不能隔离？
                DistExecutor.safeRunWhenOn(dist) {
                    DistExecutor.SafeRunnable { with(message) { context.handler() } }
                }
            }
            packetHandled = true
        }
    }

    abstract fun PayloadContext.handler()
}