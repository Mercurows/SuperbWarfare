package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.item.gun.GunItem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemDisplayContext
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.gui.overlay.ForgeGui
import net.minecraftforge.client.gui.overlay.IGuiOverlay

/**
 * 这个类的作用是在看不见的地方渲染一个第三人称的武器模型，别管为啥这么干
 * 反正删了这个绝对会出事
 */
@OnlyIn(Dist.CLIENT)
object ItemRendererFixOverlay : IGuiOverlay {
    const val ID: String = Mod.MODID + "_item_renderer_fix"

    override fun render(
        gui: ForgeGui,
        guiGraphics: GuiGraphics,
        partialTick: Float,
        screenWidth: Int,
        screenHeight: Int
    ) {
        val player = gui.getMinecraft().player ?: return
        val stack = player.mainHandItem
        if (stack.item !is GunItem) return

        guiGraphics.pose().pushPose()
        guiGraphics.pose().translate(-1145f, 0f, 0f)
        gui.getMinecraft().gameRenderer.itemInHandRenderer.renderItem(
            player, stack,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, guiGraphics.pose(), guiGraphics.bufferSource(), 0
        )
        guiGraphics.pose().popPose()
    }
}
