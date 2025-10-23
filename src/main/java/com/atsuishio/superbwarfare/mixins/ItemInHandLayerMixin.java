package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin {

    @SuppressWarnings({"ConstantConditions"})
    @Inject(method = "renderArmWithItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "HEAD"), cancellable = true)
    private void renderArmWithItemHead(LivingEntity entity, ItemStack stack, ItemDisplayContext display, HumanoidArm arm, PoseStack poseStack, MultiBufferSource source, int light, CallbackInfo ci) {
        if (entity.getType() == EntityType.PLAYER) {
            if (arm == HumanoidArm.LEFT || arm == HumanoidArm.RIGHT) {
                if (entity instanceof Player player && player.getVehicle() instanceof VehicleEntity vehicle && vehicle.banHand(player)) {
                    ci.cancel();
                }
            }
            if (arm == HumanoidArm.LEFT) {
                ItemStack mainHand = entity.getMainHandItem();
                if (mainHand.getItem() instanceof GunItem || mainHand.is(ModItems.LUNGE_MINE.get())) {
                    ci.cancel();
                }
            }
        }
    }
}
