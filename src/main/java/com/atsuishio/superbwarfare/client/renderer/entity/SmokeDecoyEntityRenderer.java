package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SmokeDecoyEntityRenderer extends EntityRenderer<SmokeDecoyEntity> {
    public SmokeDecoyEntityRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SmokeDecoyEntity flareDecoy) {
        return null;
    }
}
