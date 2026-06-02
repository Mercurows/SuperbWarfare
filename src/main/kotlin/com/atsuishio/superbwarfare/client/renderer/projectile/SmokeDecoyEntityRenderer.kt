package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation

class SmokeDecoyEntityRenderer(pContext: EntityRendererProvider.Context) : EntityRenderer<SmokeDecoyEntity>(pContext) {
    override fun getTextureLocation(flareDecoy: SmokeDecoyEntity): ResourceLocation {
        return TEXTURE
    }

    companion object {
        val TEXTURE = loc("textures/entity/empty.png")
    }
}
