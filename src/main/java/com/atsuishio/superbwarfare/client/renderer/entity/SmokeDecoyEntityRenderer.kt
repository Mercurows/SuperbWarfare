package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation

class SmokeDecoyEntityRenderer(pContext: EntityRendererProvider.Context) : EntityRenderer<com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity>(pContext) {
    override fun getTextureLocation(flareDecoy: com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity): ResourceLocation {
        return TEXTURE
    }

    companion object {
        val TEXTURE = loc("textures/entity/empty.png")
    }
}
