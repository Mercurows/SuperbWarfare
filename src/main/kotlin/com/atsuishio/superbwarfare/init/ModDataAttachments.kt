package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.capability.entity.InfiniteAmmoCapability
import com.atsuishio.superbwarfare.capability.living.PhosphorusFireCapability
import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import com.atsuishio.superbwarfare.tools.createStreamCodec
import com.atsuishio.superbwarfare.tools.generateMapCodec
import net.minecraft.nbt.CompoundTag
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.common.util.INBTSerializable
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.function.Supplier
import kotlin.reflect.full.createInstance

object ModDataAttachments {
    val ATTACHMENT_TYPES: DeferredRegister<AttachmentType<*>> =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Mod.MODID)

    /**
     * 玩家自身的弹药 / 热成像数据，只有玩家本人需要读取，同步只发给持有者自己。
     *
     * 注意：NeoForge 的初始同步不会检查 [net.neoforged.neoforge.attachment.AttachmentSyncHandler.sendToPlayer]，
     * 因此其他玩家在开始追踪该玩家时仍会收到一次该数据（客户端不会读取它）。
     */
    @JvmField
    val PLAYER_VARIABLE = register<PlayerVariable>("player_variable") {
        it.sync(PlayerVariable.SyncHandler)
    }

    /** 实体状态类数据，全量同步给所有追踪该实体的玩家（实体本身是玩家时也包含自己）。 */
    @JvmField
    val PHOSPHORUS_FIRE = registerCodec<PhosphorusFireCapability>("phosphorus_fire") {
        it.sync(createStreamCodec<PhosphorusFireCapability>())
    }

    @JvmField
    val INFINITE_AMMO = registerCodec<InfiniteAmmoCapability>("infinite_ammo") {
        it.sync(createStreamCodec<InfiniteAmmoCapability>())
    }

    private inline fun <reified T : INBTSerializable<CompoundTag>> register(
        name: String,
        noinline supplier: () -> T = { T::class.createInstance() },
        noinline sync: ((AttachmentType.Builder<T>) -> AttachmentType.Builder<T>)? = null,
    ): DeferredHolder<AttachmentType<*>, AttachmentType<T>> {
        return ATTACHMENT_TYPES.register(name, Supplier {
            val builder = AttachmentType.serializable(supplier)
            (sync?.invoke(builder) ?: builder).build()
        })
    }

    private inline fun <reified T : Any> registerCodec(
        name: String,
        noinline sync: ((AttachmentType.Builder<T>) -> AttachmentType.Builder<T>)? = null,
    ): DeferredHolder<AttachmentType<*>, AttachmentType<T>> {
        return ATTACHMENT_TYPES.register(name, Supplier {
            val builder = AttachmentType.builder(Supplier { T::class.createInstance() })
                .serialize(generateMapCodec<T>().codec())
            (sync?.invoke(builder) ?: builder).build()
        })
    }
}
