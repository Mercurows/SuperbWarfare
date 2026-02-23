package com.atsuishio.superbwarfare.client.decorator

import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.item.container.LuckyContainerBlockItem
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.IItemDecorator

@OnlyIn(Dist.CLIENT)
class LuckyContainerItemDecorator : IItemDecorator {
    override fun render(guiGraphics: GuiGraphics, font: Font?, stack: ItemStack, xOffset: Int, yOffset: Int): Boolean {
        if (stack.item !is LuckyContainerBlockItem) return false
        val tag = BlockItem.getBlockEntityData(stack) ?: return false
        if (!tag.contains("Icon")) return false
        val iconTag = tag.getString("Icon")
        val icon = ResourceLocation.tryParse(iconTag) ?: return false

        val pose = guiGraphics.pose()
        pose.pushPose()
        RenderHelper.preciseBlit(guiGraphics, icon, xOffset.toFloat(), yOffset.toFloat(), 200f, 0f, 0f, 8f, 8f, 8f, 8f)
        pose.popPose()

        return true
    }
}
