package com.atsuishio.superbwarfare.capability.sync

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.capability.sync.CapabilitySync.flush
import com.atsuishio.superbwarfare.capability.sync.CapabilitySync.markDirty
import com.atsuishio.superbwarfare.network.message.receive.CapabilitySyncMessage
import com.atsuishio.superbwarfare.serialization.ByteBufDecoder
import com.atsuishio.superbwarfare.serialization.ByteBufEncoder
import com.atsuishio.superbwarfare.tools.sendPacketTo
import io.netty.buffer.Unpooled
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.network.PacketDistributor

/**
 * Capability 自动同步框架。
 *
 * Forge 的 Capability 系统只负责持久化（`ICapabilitySerializable` + `AttachCapabilitiesEvent`）
 * 与玩家重生时的数据搬运，**不会**把数据发给客户端，官方要求各 mod 自己用包同步，
 * 并且明确列出三个必须自己处理同步的时机：实体生成、数据变化、新客户端开始观察。
 *
 * 本对象把这三件事一次性地接过来：
 *
 * 1. **何时同步**：[markDirty] 只打脏标记，真正的发包统一在服务端 tick 结束时由 [flush] 完成，
 *    同一 tick 内的多次修改会合并成一个包，同一实体上的多个 capability 也会合并。
 * 2. **向谁同步**：由 [SyncedCapability.syncTarget] 决定，收件人交给 `PacketDistributor`
 *    与实体 tracking 集合解析，不再在每个调用点手写 `sendPacketToTrackingThis`。
 * 3. **全量补发**：`StartTracking`（新观察者）、玩家登录/重生/换维度时自动推送完整数据。
 *    玩家自己的实体不在自己的 tracking 集合里，且客户端在重生/换维度时不会重建本地玩家实体，
 *    因此这几处必须显式补发，否则客户端会一直留着旧值。
 *
 * 新增一个自动同步的 capability 只需要三步：实现 [SyncedCapability]、在
 * [ModSyncedCapabilities] 里登记、在写入点调用 [markDirty]。数据由 capability 自己用
 * `ByteBufEncoder` / `ByteBufDecoder` 编解码，框架只负责"什么时候发、发给谁、怎么打包"。
 *
 * 线程约束：注册表与脏集合只在服务端主线程读写（tick、命令、物品交互、效果事件都在主线程）。
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE)
object CapabilitySync {

    private class Entry(
        val id: ResourceLocation,
        val getter: (Entity) -> SyncedCapability?,
    )

    /** 已登记的自动同步 capability，key 为其 AttachCapabilitiesEvent 中使用的 id */
    private val registry = LinkedHashMap<ResourceLocation, Entry>()

    /** 维度 -> 实体 id -> 待同步的 capability id */
    private var dirty: HashMap<ResourceLocation, HashMap<Int, MutableSet<ResourceLocation>>> = HashMap()

    /** 已就"未登记的 id"警告过的 capability，避免每 tick 刷屏 */
    private val warned = HashSet<ResourceLocation>()

    /** 跟随实体可见性，新观察者出现时需要补发 */
    private val TRACKING_TARGETS = setOf(SyncTarget.TRACKING)

    /** 玩家自己的实体：tracking 集合不包含本人，因此两类策略都要推给本人 */
    private val SELF_TARGETS = setOf(SyncTarget.TRACKING, SyncTarget.SELF)

    /**
     * 登记一个自动同步的 capability。
     *
     * @param id 与 `AttachCapabilitiesEvent#addCapability` 中使用的 id 一致
     * @param getter 从实体上取得该 capability 的**已挂载**实例，没挂载则返回 `null`
     */
    fun register(id: ResourceLocation, getter: (Entity) -> SyncedCapability?): ResourceLocation {
        registry[id] = Entry(id, getter)
        return id
    }

    /**
     * 标记实体上的某个 capability 已发生变化，将在本 tick 结束时自动同步。
     *
     * 客户端调用无副作用（客户端不负责发包），因此可以在 setter 里无条件调用。
     */
    fun markDirty(entity: Entity?, id: ResourceLocation) {
        if (entity == null) return

        val level = entity.level() ?: return
        if (level.isClientSide) return

        if (id !in registry) {
            if (warned.add(id)) {
                Mod.LOGGER.warn("capability {} 未在 ModSyncedCapabilities 中登记，改动不会被同步", id)
            }
            return
        }

        dirty.getOrPut(level.dimension().location()) { HashMap() }
            .getOrPut(entity.id) { HashSet() }
            .add(id)
    }

    /** 客户端收到同步数据后写入本地 capability */
    fun apply(entity: Entity, id: ResourceLocation, data: ByteArray, full: Boolean) {
        val cap = registry[id]?.getter?.invoke(entity) ?: return
        val buf = FriendlyByteBuf(Unpooled.wrappedBuffer(data))

        try {
            cap.readSync(ByteBufDecoder(buf), full)
        } finally {
            buf.release()
        }
    }

    /** 把一个 capability 当前需要同步的数据编码成独立的字节块 */
    private fun encode(cap: SyncedCapability, full: Boolean): ByteArray {
        val buf = FriendlyByteBuf(Unpooled.buffer())

        return try {
            cap.writeSync(ByteBufEncoder(buf), full)

            ByteArray(buf.readableBytes()).also { buf.readBytes(it) }
        } finally {
            buf.release()
        }
    }

    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return

        flush(event.server)
    }

    /** 把本 tick 累积的改动合批发出去 */
    private fun flush(server: MinecraftServer) {
        if (dirty.isEmpty()) return

        val snapshot = dirty
        dirty = HashMap()

        for ((dim, perEntity) in snapshot) {
            val level = server.allLevels.firstOrNull { it.dimension().location() == dim } ?: continue

            for ((entityId, capIds) in perEntity) {
                val entity = level.getEntity(entityId) ?: continue

                send(entity, capIds, full = false)
            }
        }
    }

    /**
     * 把给定的一组 capability 发给该实体的同步目标。
     *
     * 同一实体的多个 capability 如果目标策略相同，会合并进同一个 [CapabilitySyncMessage]。
     */
    private fun send(entity: Entity, capIds: Collection<ResourceLocation>, full: Boolean) {
        val grouped = LinkedHashMap<SyncTarget, MutableList<CapabilitySyncMessage.Entry>>()

        for (capId in capIds) {
            val entry = registry[capId] ?: continue
            val cap = entry.getter(entity) ?: continue

            grouped.getOrPut(cap.syncTarget) { ArrayList() }
                .add(CapabilitySyncMessage.Entry(entry.id, encode(cap, full)))
        }

        if (grouped.isEmpty()) return

        for ((target, entries) in grouped) {
            val message = CapabilitySyncMessage(entity.id, full, entries)

            when (target) {
                // 跟踪该实体的所有玩家；实体本身是玩家时额外发给其本人
                SyncTarget.TRACKING ->
                    sendPacketTo(PacketDistributor.TRACKING_ENTITY_AND_SELF.with { entity }, message)

                SyncTarget.SELF ->
                    (entity as? ServerPlayer)?.let { sendPacketTo(it, message) }
            }
        }
    }

    /** 把实体上所有匹配 [targets] 的 capability 全量推给某个玩家 */
    private fun pushAll(entity: Entity, player: ServerPlayer, targets: Set<SyncTarget>) {
        val entries = ArrayList<CapabilitySyncMessage.Entry>(registry.size)

        for (entry in registry.values) {
            val cap = entry.getter(entity) ?: continue
            if (cap.syncTarget !in targets) continue

            entries.add(CapabilitySyncMessage.Entry(entry.id, encode(cap, full = true)))
        }

        if (entries.isEmpty()) return

        sendPacketTo(player, CapabilitySyncMessage(entity.id, true, entries))
    }

    /** 新观察者进入视野：补发所有"跟随可见性"的数据 */
    @SubscribeEvent
    fun onStartTracking(event: PlayerEvent.StartTracking) {
        val player = event.entity as? ServerPlayer ?: return

        pushAll(event.target, player, TRACKING_TARGETS)
    }

    /** 登录时补发玩家自己实体的数据 */
    @SubscribeEvent
    fun onPlayerLoggedIn(event: PlayerLoggedInEvent) {
        pushSelf(event.entity)
    }

    /** 重生后是新的实体（默认值），而客户端不会重建本地玩家实体，必须重推 */
    @SubscribeEvent
    fun onPlayerRespawn(event: PlayerEvent.PlayerRespawnEvent) {
        pushSelf(event.entity)
    }

    /** 换维度后 tracking 关系重建，客户端也留着旧维度里的值 */
    @SubscribeEvent
    fun onPlayerChangedDimension(event: PlayerChangedDimensionEvent) {
        pushSelf(event.entity)
    }

    private fun pushSelf(entity: Entity) {
        val player = entity as? ServerPlayer ?: return

        pushAll(player, player, SELF_TARGETS)
    }
}
