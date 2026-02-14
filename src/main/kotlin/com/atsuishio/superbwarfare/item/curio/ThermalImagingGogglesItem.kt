package com.atsuishio.superbwarfare.item.curio

import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
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

    override fun curioTick(slotContext: SlotContext, stack: ItemStack?) {
        val living = slotContext.entity()
        if (!living.level().isClientSide) {
            living.addEffect(MobEffectInstance(MobEffects.NIGHT_VISION, 3, 0, false, false))
        }
    }
}
