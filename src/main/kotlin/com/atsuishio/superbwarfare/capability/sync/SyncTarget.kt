package com.atsuishio.superbwarfare.capability.sync

/**
 * [SyncedCapability] 的同步目标策略，决定"向谁同步"。
 *
 * 具体的收件人由 [CapabilitySync] 交给 `PacketDistributor` 与实体的 tracking 集合解析，
 * 不需要在各个调用点手写收件人。
 *
 * 需要"整个维度广播"之类的策略时，在这里新增枚举值并在 [CapabilitySync] 的 `send`
 * 与 `pushSelf` 中补上对应分支（注意新玩家登录时也需要补发一次，否则他会漏掉
 * 与可见性无关的数据）。
 */
enum class SyncTarget {
    /**
     * 所有能看到该实体的玩家；实体本身是玩家时额外发给其本人。
     *
     * 与 vanilla `ServerEntity#broadcastAndSend` 的收件人语义一致。
     */
    TRACKING,

    /** 只发给实体本人（实体必须是玩家），适合只对本人有意义的私有数据 */
    SELF,
}
