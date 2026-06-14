package com.atsuishio.superbwarfare.item.curio

import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import top.theillusivec4.curios.api.CuriosApi
import top.theillusivec4.curios.api.SlotContext
import top.theillusivec4.curios.api.type.capability.ICurioItem

class ThermalImagingGogglesItem : Item(Properties().stacksTo(1)), ICurioItem {
    override fun canEquip(slotContext: SlotContext, stack: ItemStack?): Boolean {
        return CuriosApi.getCuriosInventory(slotContext.entity())
            .flatMap { c -> c.findFirstCurio(this) }
            .isEmpty
    }
}
