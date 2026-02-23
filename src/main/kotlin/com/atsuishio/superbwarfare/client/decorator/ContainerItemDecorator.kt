package com.atsuishio.superbwarfare.client.decorator

import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.item.container.ContainerBlockItem
import com.atsuishio.superbwarfare.tools.clientLevel
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.client.IItemDecorator

@OnlyIn(Dist.CLIENT)
class ContainerItemDecorator : IItemDecorator {
    override fun render(guiGraphics: GuiGraphics, font: Font, stack: ItemStack, xOffset: Int, yOffset: Int): Boolean {
        if (stack.item !is ContainerBlockItem) return false
        val tag = stack.get(DataComponents.BLOCK_ENTITY_DATA)?.copyTag() ?: return false

        var icon: ResourceLocation? = null
        if (tag.contains("EntityType")) {
            val typeString = tag.getString("EntityType")

            if (icons.containsKey(typeString)) {
                icon = icons[typeString]
            } else {
                val entityType = EntityType.byString(typeString).orElse(null) ?: return false

                val level = clientLevel ?: return false

                val entity: Entity? = entityType.create(level)
                if (entity !is VehicleEntity) return false

                icon = entity.vehicleItemIcon
                icons[typeString] = icon
            }
        }
        if (icon == null) return false

        val pose = guiGraphics.pose()
        pose.pushPose()

        RenderHelper.preciseBlit(guiGraphics, icon, xOffset.toFloat(), yOffset.toFloat(), 200f, 0f, 0f, 8f, 8f, 8f, 8f)

        pose.popPose()

        return true
    }

    companion object {
        private val icons = HashMap<String?, ResourceLocation?>()
    }
}
