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
            RenderHelper.preciseBlit(guiGraphics, customIcon, xOffset.toFloat(), yOffset.toFloat(), 200f, 0f, 0f, 16f, 16f, 16f, 16f)
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
        RenderHelper.preciseBlit(guiGraphics, icon, xOffset.toFloat(), yOffset.toFloat(), 200f, 0f, 0f, 8f, 8f, 8f, 8f)
        pose.popPose()
        return true
    }

    companion object {
        private val ICON_CACHE = hashMapOf<String, ResourceLocation>()

        /**
         * 缓存已检查过的车辆自定义图标路径，null表示已检查且未找到
         */
        private val CUSTOM_ICON_CACHE = hashMapOf<String, ResourceLocation>()

        /**
         * 根据ItemStack获取自定义载具图标纹理路径
         * 检查 textures/vehicle_icon/container/<vehicleId>.png 是否存在
         */
        fun getCustomIcon(stack: ItemStack): ResourceLocation? {
            val tag = BlockItem.getBlockEntityData(stack) ?: return null
            if (!tag.contains("EntityType")) return null
            val entityType = tag.getString("EntityType")
            val id = ResourceLocation.tryParse(entityType) ?: return null
            val vehicleId = id.path

            val cached = CUSTOM_ICON_CACHE[vehicleId]
            if (cached !== null || CUSTOM_ICON_CACHE.containsKey(vehicleId)) return cached

            val texturePath = Mod.loc("textures/vehicle_icon/container/$vehicleId.png")
            val resource = Minecraft.getInstance().resourceManager.getResource(texturePath)
            val result = if (resource.isPresent) texturePath else return null
            CUSTOM_ICON_CACHE[vehicleId] = result
            return result
        }
    }
}
