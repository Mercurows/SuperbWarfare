package com.atsuishio.superbwarfare.capability.sync

import com.atsuishio.superbwarfare.serialization.ByteBufDecoder
import com.atsuishio.superbwarfare.serialization.ByteBufEncoder

/**
 * 可被 [CapabilitySync] 自动同步的 Capability。
 *
 * 实现本接口并在 [CapabilitySync.register] 中登记之后，就**不需要再手写同步包和收件人**：
 * 只要在写入点调用一次 [CapabilitySync.markDirty]，框架会负责
 * "何时发包"（服务端 tick 结束时合批）与"发给谁"（由 [syncTarget] 决定）。
 *
 * 数据直接写进 mod 自带的 [ByteBufEncoder] / [ByteBufDecoder]（底层是 `FriendlyByteBuf`），
 * 不经过 NBT，也不经过 kotlinx 序列化。
 *
 * 示例见 `InfiniteAmmoCapability`、`PhosphorusFireCapability` 与 `PlayerVariable`。
 */
interface SyncedCapability {

    /**
     * 把需要同步的数据按顺序写进 [encoder]。
     *
     * 实现必须与 [readSync] 严格对称：写多少个值、按什么顺序写，读的时候就要一一对应。
     * 每个 capability 的数据在包内是独立的字节块，因此不需要自己写长度或分隔符。
     *
     * @param full 为 `true` 时接收方没有任何缓存（实体刚进入视野、玩家登录/重生/换维度），
     *   需要把完整状态写出去；为 `false` 时可以只写发生变化的字段。
     */
    fun writeSync(encoder: ByteBufEncoder, full: Boolean)

    /**
     * 按 [writeSync] 相同的顺序读出数据并应用。
     *
     * 注意：本方法在**客户端主线程**被调用，不要在这里再次调用 [CapabilitySync.markDirty]，
     * 也不要触发其它副作用。
     *
     * @param full 含义同 [writeSync]
     */
    fun readSync(decoder: ByteBufDecoder, full: Boolean)

    /** 该 capability 的同步目标，默认跟随实体的可见性 */
    val syncTarget: SyncTarget
        get() = SyncTarget.TRACKING
}
