package com.atsuishio.superbwarfare.client.decorator

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.item.misc.VehicleKeyItem
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.clientLevel
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.IItemDecorator

@OnlyIn(Dist.CLIENT)
class VehicleKeyItemDecorator : IItemDecorator {
    override fun render(guiGraphics: GuiGraphics, font: Font?, stack: ItemStack, xOffset: Int, yOffset: Int): Boolean {
        if (stack.item !is VehicleKeyItem) return false
        val tag = stack.tag ?: return false
        if (!tag.contains(VehicleKeyItem.TAG_UUID)) return false
        val uuid = tag.getString(VehicleKeyItem.TAG_UUID)
        val level = clientLevel ?: return false
        val entity = EntityFindUtil.findEntity(level, uuid) as? Player ?: return false
        val team = entity.team ?: return false
        val color = team.color.color ?: return false

        val pose = guiGraphics.pose()
        pose.pushPose()

        val colorInt = (0xFF shl 24) or (color and 0xFFFFFF)

        RenderHelper.blit(
            pose,
            TEXTURE,
            xOffset.toFloat(),
            yOffset.toFloat(),
            0f,
            0f,
            16f,
            16f,
            16f,
            16f,
            colorInt
        )

        pose.popPose()

        return true
    }

    companion object {
        val TEXTURE = loc("textures/item/vehicle_key_deco.png")
    }
}
