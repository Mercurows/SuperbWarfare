package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.item.attachment.AttachmentItem
import net.minecraft.world.item.Item
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModAttachments {
    @JvmField
    val REGISTRY: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, Mod.MODID)

    @JvmField
    val OEM_STOCK_STANDARD: RegistryObject<Item> = register("oem_stock_standard")

    @JvmField
    val MAGAZINE_EXTEND: RegistryObject<Item> = register("magazine_extend")

    @JvmField
    val MAGAZINE_EXTEND_PRO: RegistryObject<Item> = register("magazine_extend_pro")

    private fun register(id: String): RegistryObject<Item> {
        return REGISTRY.register(id) { AttachmentItem("${Mod.MODID}:$id") }
    }

    fun register(bus: IEventBus) {
        REGISTRY.register(bus)
    }
}
