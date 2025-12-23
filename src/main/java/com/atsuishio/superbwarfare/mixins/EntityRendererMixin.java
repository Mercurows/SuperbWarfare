package com.atsuishio.superbwarfare.mixins;

import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    //TODO 正确mixin

//    @Inject(
//        method = "getTextureLocation",
//        at = @At("HEAD")
//    )
//    private ResourceLocation onGetTextureLocation(ResourceLocation original) {
//        // 返回我们想要的贴图
//        return new ResourceLocation("your_mod_id", "path/to/your/texture.png");
//    }
}