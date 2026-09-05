package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.capability.living.InfinityAmmoCapability
import com.atsuishio.superbwarfare.capability.living.PhosphorusFireCapability
import com.atsuishio.superbwarfare.capability.player.PlayerVariable
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

    @JvmField
    val PLAYER_VARIABLE = register<PlayerVariable>("player_variable")

    @JvmField
    val PHOSPHORUS_FIRE = register<PhosphorusFireCapability>("phosphorus_fire")

    @JvmField
    val INFINITY_AMMO = register<InfinityAmmoCapability>("infinity_ammo")

    private inline fun <reified T : INBTSerializable<CompoundTag>> register(
        name: String,
        noinline supplier: () -> T = { T::class.createInstance() }
    ): DeferredHolder<AttachmentType<*>, AttachmentType<T>> {
        return ATTACHMENT_TYPES.register(name, Supplier { AttachmentType.serializable(supplier).build() })
    }
}
