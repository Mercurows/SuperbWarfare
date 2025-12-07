package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.gun.GunItem
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.client.player.LocalPlayer
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemDisplayContext
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import javax.annotation.ParametersAreNonnullByDefault

/**
 * 这个类的作用是在看不见的地方渲染一个第三人称的武器模型，别管为啥这么干
 * 反正删了这个绝对会出事
 */
@OnlyIn(Dist.CLIENT)
object ItemRendererFixOverlay : LayeredDraw.Layer {
    @JvmField
    val ID: ResourceLocation = loc("item_renderer_fix")

    @ParametersAreNonnullByDefault
    override fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
        val mc = Minecraft.getInstance()
        val player: LocalPlayer = mc.player ?: return
        val stack = player.mainHandItem
        if (stack.item !is GunItem) return

        guiGraphics.pose().pushPose()
        guiGraphics.pose().translate(-1145f, 0f, 0f)
        mc.gameRenderer.itemInHandRenderer.renderItem(
            player, stack,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, guiGraphics.pose(), guiGraphics.bufferSource(), 0
        )
        guiGraphics.pose().popPose()
    }
}
