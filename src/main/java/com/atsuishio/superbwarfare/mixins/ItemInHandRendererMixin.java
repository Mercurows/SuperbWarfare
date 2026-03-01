package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Shadow
    private ItemStack mainHandItem;

    @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true, index = 5)
    private float renderArmWithItem(float value) {
        if (mainHandItem.getItem() instanceof GunItem) return 0f;
        return value;
    }
}
