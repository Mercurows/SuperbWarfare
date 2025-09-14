package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantedCountIncreaseFunction.class)
public abstract class EnchantedCountIncreaseFunctionMixin {

    @Final
    @Shadow
    private NumberProvider value;

    @Final
    @Shadow
    private int limit;

    @Final
    @Shadow
    private Holder<Enchantment> enchantment;

    @Shadow
    protected abstract boolean hasLimit();

    @Inject(method = "run(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/storage/loot/LootContext;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), cancellable = true)
    private void run(ItemStack stack, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
        Entity entity = context.getParamOrNull(LootContextParams.ATTACKING_ENTITY);
        if (entity instanceof LivingEntity living && this.enchantment.is(Enchantments.LOOTING)) {
            ItemStack mainHandItem = living.getMainHandItem();
            if (!(mainHandItem.getItem() instanceof GunItem)) return;

            int level = GunData.from(mainHandItem).perk.getLevel(ModPerks.POWERFUL_ATTRACTION);
            if (level > 0) {
                float f = (float) level * this.value.getFloat(context);
                stack.grow(Math.round(f));
                if (this.hasLimit()) {
                    stack.limitSize(this.limit);
                }
                cir.setReturnValue(stack);
            }
        }
    }
}
