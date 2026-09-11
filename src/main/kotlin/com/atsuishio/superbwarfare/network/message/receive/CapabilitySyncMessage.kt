package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.capability.sync.CapabilitySync
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.clientLevel
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation

/**
 * 通用 Capability 同步包，由 [CapabilitySync] 统一发送。
 *
 * 一个包承载同一实体、同一同步目标下的所有 capability，因此新增自动同步的 capability
 * 不需要再新增包类型。
 *
 * 数据由各个 capability 自己用 mod 自带的 `ByteBufEncoder` / `ByteBufDecoder` 编解码，
 * 因此本包不使用 kotlinx 序列化，也就无法由 `@RegisterPacket` 自动注册，
 * 需要在 `initializeNetwork()` 中手动注册（见 `NetworkRegistry.kt`）。
 *
 * 线格式：
 * ```
 * varInt  实体 id
 * boolean 是否全量
 * varInt  条目数量
 * 每个条目：
 *   ResourceLocation capability id
 *   byte[]           该 capability 的数据（varInt 长度 + 内容）
 * ```
 */
class CapabilitySyncMessage(
    private val entity: Int,
    private val full: Boolean,
    private val entries: List<Entry>,
) : ClientPacketPayload() {

    /** 单个 capability 的同步条目：id 与已经编码好的数据 */
    class Entry(val id: ResourceLocation, val data: ByteArray)

    fun writeTo(buf: FriendlyByteBuf) {
        buf.writeVarInt(entity)
        buf.writeBoolean(full)
        buf.writeVarInt(entries.size)

        for (entry in entries) {
            buf.writeResourceLocation(entry.id)
            buf.writeByteArray(entry.data)
        }
    }

    override fun PayloadContext.handler() {
        val target = clientLevel?.getEntity(entity) ?: return

        for (entry in entries) {
            CapabilitySync.apply(target, entry.id, entry.data, full)
        }
    }

    companion object {
        /**
         * 解码在 netty 线程上执行，此时不能访问客户端世界，因此这里只做纯粹的缓冲读取，
         * 真正的应用留到 [handler] 里在客户端主线程完成。
         */
        @JvmStatic
        fun readFrom(buf: FriendlyByteBuf): CapabilitySyncMessage {
            val entity = buf.readVarInt()
            val full = buf.readBoolean()
            val size = buf.readVarInt()

            val entries = ArrayList<Entry>(size)
            for (i in 0 until size) {
                val id = buf.readResourceLocation()
                entries.add(Entry(id, buf.readByteArray()))
            }

            return CapabilitySyncMessage(entity, full, entries)
        }
    }
}
