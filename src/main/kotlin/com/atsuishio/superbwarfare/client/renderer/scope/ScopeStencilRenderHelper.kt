package com.atsuishio.superbwarfare.client.renderer.scope

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.lwjgl.opengl.GL11

@OnlyIn(Dist.CLIENT)
object ScopeStencilRenderHelper {

    fun enableItemEntityStencilTest() {
        RenderSystem.assertOnRenderThread()
        Minecraft.getInstance().mainRenderTarget.enableStencil()
        GL11.glEnable(GL11.GL_STENCIL_TEST)
    }

    fun disableItemEntityStencilTest() {
        RenderSystem.assertOnRenderThread()
        GL11.glDisable(GL11.GL_STENCIL_TEST)
    }
}
