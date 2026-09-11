package com.atsuishio.superbwarfare.capability.player

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.init.ModDataAttachments
import com.atsuishio.superbwarfare.network.decodeFrom
import com.atsuishio.superbwarfare.network.encodeTo
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.attachment.AttachmentSyncHandler
import net.neoforged.neoforge.attachment.IAttachmentHolder
import net.neoforged.neoforge.common.util.INBTSerializable
import net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone
import java.util.*
import java.util.function.Consumer

class PlayerVariable : INBTSerializable<CompoundTag> {

    @JvmField
    var ammo: MutableMap<Ammo, Int> = EnumMap(Ammo::class.java)
    var activeThermalImaging: Boolean = false

    /**
     * 该变量在服务端是就地修改的可变对象，改完之后需要主动通知 NeoForge 重新下发全量数据。
     */
    fun sync(entity: Entity) {
        entity.syncData(ModDataAttachments.PLAYER_VARIABLE)
    }

    /** 用于网络同步的完整快照，-1 表示 [activeThermalImaging]，其余为 [Ammo] 的 ordinal。 */
    fun snapshot(): Map<Byte, Int> {
        val map = hashMapOf<Byte, Int>()

        for (type in Ammo.entries) {
            map[type.ordinal.toByte()] = type.get(this)
        }

        map[(-1).toByte()] = if (this.activeThermalImaging) 1 else 0

        return map
    }

    fun applySnapshot(snapshot: Map<Byte, Int>) {
        for ((key, value) in snapshot) {
            if (key == (-1).toByte()) {
                this.activeThermalImaging = value == 1
            } else {
                Ammo.entries.getOrNull(key.toInt())?.set(this, value)
            }
        }
    }

    fun writeToNBT(): CompoundTag {
        val nbt = CompoundTag()

        for (type in Ammo.entries) {
            type.set(nbt, type.get(this))
        }

        nbt.putBoolean("ActiveThermalImaging", activeThermalImaging)

        return nbt
    }

    fun readFromNBT(tag: CompoundTag) {
        for (type in Ammo.entries) {
            type.set(this, type.get(tag))
        }

        activeThermalImaging = tag.getBoolean("ActiveThermalImaging")
    }

    fun copy(): PlayerVariable {
        val clone = PlayerVariable()

        for (type in Ammo.entries) {
            type.set(clone, type.get(this))
        }

        clone.activeThermalImaging = this.activeThermalImaging

        return clone
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PlayerVariable) return false

        for (type in Ammo.entries) {
            if (type.get(this) != type.get(other)) return false
        }

        return activeThermalImaging == other.activeThermalImaging
    }

    override fun serializeNBT(provider: HolderLookup.Provider): CompoundTag {
        return writeToNBT()
    }

    override fun deserializeNBT(provider: HolderLookup.Provider, nbt: CompoundTag) {
        readFromNBT(nbt)
    }

    /**
     * 该数据只对玩家本人有意义，因此更新只下发给持有者自己。
     *
     * 初始同步（其他玩家开始追踪该玩家时）不受此限制，见 [ModDataAttachments.PLAYER_VARIABLE] 的注释。
     */
    object SyncHandler : AttachmentSyncHandler<PlayerVariable> {
        override fun sendToPlayer(holder: IAttachmentHolder, to: ServerPlayer): Boolean {
            return holder === to
        }

        override fun write(buf: RegistryFriendlyByteBuf, attachment: PlayerVariable, initialSync: Boolean) {
            encodeTo(buf, attachment.snapshot())
        }

        override fun read(
            holder: IAttachmentHolder,
            buf: RegistryFriendlyByteBuf,
            previousValue: PlayerVariable?
        ): PlayerVariable {
            val variable = previousValue ?: PlayerVariable()
            variable.applySnapshot(decodeFrom(buf))
            return variable
        }
    }

    @EventBusSubscriber(modid = Mod.MODID)
    companion object {
        @JvmStatic
        fun modify(player: Player, consumer: Consumer<PlayerVariable>) {
            val cap = player.getData(ModDataAttachments.PLAYER_VARIABLE)
            consumer.accept(cap)
            cap.sync(player)
        }

        @JvmStatic
        fun getOrDefault(entity: Entity): PlayerVariable {
            return entity.getData(ModDataAttachments.PLAYER_VARIABLE)
        }

        @SubscribeEvent
        fun clonePlayer(event: Clone) {
            event.original.revive()
            val original = event.original.getData(ModDataAttachments.PLAYER_VARIABLE)
            if (event.entity.level().isClientSide()) return
            // 复制发生在 PlayerList#respawn 的初始同步之前，客户端会收到复制后的数据
            event.entity.setData(ModDataAttachments.PLAYER_VARIABLE, original.copy())
        }
    }

    override fun hashCode(): Int {
        var result = activeThermalImaging.hashCode()
        result = 31 * result + ammo.hashCode()
        return result
    }
}
