package com.atsuishio.superbwarfare.item.curio;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class ThermalImagingGogglesItem extends Item implements ICurioItem {

    public ThermalImagingGogglesItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return CuriosApi.getCuriosInventory(slotContext.entity())
                .resolve()
                .flatMap(c -> c.findFirstCurio(this))
                .isEmpty();
    }

//    @Override
//    public void curioTick(SlotContext slotContext, ItemStack stack) {
//        LivingEntity living = slotContext.entity();
//        if (!living.level().isClientSide) {
//            living.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false));
//        }
//    }
}
