package com.atsuishio.superbwarfare.client.renderer.entity

import net.minecraft.client.renderer.entity.EntityRendererProvider

class Mle1934Renderer(manager: EntityRendererProvider.Context) : BasicArtilleryRenderer(manager) {
    override fun hideForTurretControllerWhileZooming(): Boolean {
        return true
    }
}
