package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.client.renderer.special.OBBRenderer;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(method = "renderHitbox",
            at = @At("RETURN"))
    private static void renderHitbox(PoseStack poseStack, VertexConsumer buffer, Entity entity, float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (entity instanceof OBBEntity obbEntity) {
            OBBRenderer.INSTANCE.render(entity, obbEntity.getOBB(), poseStack, buffer, 1, 1, 1, 1);
        }
    }
}
