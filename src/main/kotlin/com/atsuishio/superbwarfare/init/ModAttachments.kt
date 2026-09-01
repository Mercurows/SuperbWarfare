package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.item.attachment.AttachmentItem
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModAttachments {
    @JvmField
    val REGISTRY: DeferredRegister<Item> = DeferredRegister.create(BuiltInRegistries.ITEM, Mod.MODID)

    @JvmField
    val OEM_STOCK_STANDARD: DeferredHolder<Item, out Item> = register("oem_stock_standard")

    @JvmField
    val MAGAZINE_EXTEND: DeferredHolder<Item, out Item> = register("magazine_extend")

    @JvmField
    val MAGAZINE_EXTEND_PRO: DeferredHolder<Item, out Item> = register("magazine_extend_pro")

    private fun register(id: String): DeferredHolder<Item, out Item> {
        return REGISTRY.register(id, Supplier { AttachmentItem("${Mod.MODID}:$id") })
    }

    fun register(bus: IEventBus) {
        REGISTRY.register(bus)
    }
}
