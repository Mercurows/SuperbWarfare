package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.capability.living.PhosphorusFireCapability
import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.function.Supplier

object ModAttachments {
    val ATTACHMENT_TYPES: DeferredRegister<AttachmentType<*>> =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Mod.MODID)

    @JvmField
    val PLAYER_VARIABLE: Supplier<AttachmentType<PlayerVariable>> =
        ATTACHMENT_TYPES.register(
            "player_variable",
            Supplier { AttachmentType.serializable(::PlayerVariable).build() }
        )

    @JvmField
    val PHOSPHORUS_FIRE: Supplier<AttachmentType<PhosphorusFireCapability>> =
        ATTACHMENT_TYPES.register(
            "phosphorus_fire",
            Supplier { AttachmentType.serializable(::PhosphorusFireCapability).build() }
        )
}
