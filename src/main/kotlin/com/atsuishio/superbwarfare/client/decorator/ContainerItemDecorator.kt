package com.atsuishio.superbwarfare.client.decorator

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.item.container.ContainerBlockItem
import com.atsuishio.superbwarfare.tools.clientLevel
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.IItemDecorator

@OnlyIn(Dist.CLIENT)
class ContainerItemDecorator : IItemDecorator {

    override fun render(guiGraphics: GuiGraphics, font: Font?, stack: ItemStack, xOffset: Int, yOffset: Int): Boolean {
        if (stack.item !is ContainerBlockItem) return false
        val tag = BlockItem.getBlockEntityData(stack) ?: return false
        if (!tag.contains("EntityType")) return false

        val typeString = tag.getString("EntityType")

        // 优先检查自定义载具截图（textures/vehicle_icon/container/<vehicleId>.png）
        val customIcon = getCustomIcon(stack)
        if (customIcon != null) {
            val pose = guiGraphics.pose()
            pose.pushPose()
            RenderHelper.blit(guiGraphics.pose(), customIcon, xOffset.toFloat(), yOffset.toFloat(), 16f, 16f, z = 200f)
            pose.popPose()
        }

        // 回退：使用载具自带的小角标图标（8×8）
        var icon: ResourceLocation?
        if (ICON_CACHE.containsKey(typeString)) {
            icon = ICON_CACHE[typeString]
        } else {
            val entityType = EntityType.byString(typeString).orElse(null) ?: return false
            val level = clientLevel ?: return false
            val entity: Entity? = entityType.create(level)
            if (entity !is VehicleEntity) return false
            icon = entity.vehicleItemIcon ?: return false
            ICON_CACHE[typeString] = icon
        }
        if (icon == null) return false

        val pose = guiGraphics.pose()
        pose.pushPose()
        RenderHelper.blit(guiGraphics.pose(), icon, xOffset.toFloat(), yOffset.toFloat(), 8f, 8f, z = 200f)
        pose.popPose()
        return true
    }

    companion object {
        private val ICON_CACHE = hashMapOf<String, ResourceLocation>()

        /**
         * 按完整载具 ID 缓存找到的自定义图标路径
         */
        private val CUSTOM_ICON_CACHE = hashMapOf<ResourceLocation, ResourceLocation>()

        /**
         * 根据ItemStack获取自定义载具图标纹理路径
         * 优先在载具自己的命名空间中查找图标
         */
        fun getCustomIcon(stack: ItemStack): ResourceLocation? {
            val tag = BlockItem.getBlockEntityData(stack) ?: return null
            if (!tag.contains("EntityType")) return null
            val entityType = tag.getString("EntityType")
            val id = ResourceLocation.tryParse(entityType) ?: return null
            CUSTOM_ICON_CACHE[id]?.let { return it }

            val path = "textures/vehicle_icon/container/${id.path}.png"
            val manager = Minecraft.getInstance().resourceManager
            val texture = Mod.rl(id.namespace, path)
            val legacyTexture = Mod.loc(path)
            val result = when {
                manager.getResource(texture).isPresent -> texture
                texture != legacyTexture && manager.getResource(legacyTexture).isPresent -> legacyTexture
                else -> return null
            }
            CUSTOM_ICON_CACHE[id] = result
            return result
        }
    }
}
