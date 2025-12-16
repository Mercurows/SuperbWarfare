package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.init.ModTags
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object ArmorPlateOverlay : CommonOverlay("armor_plate") {
    private val ICON = loc("textures/overlay/armor_plate/icon.png")
    private val BAR_1 = loc("textures/overlay/armor_plate/bar_1.png")
    private val BAR_2 = loc("textures/overlay/armor_plate/bar_2.png")
    private val BAR_3 = loc("textures/overlay/armor_plate/bar_3.png")
    private val BAR_FRAME_1 = loc("textures/overlay/armor_plate/bar_frame_1.png")
    private val BAR_FRAME_2 = loc("textures/overlay/armor_plate/bar_frame_2.png")
    private val BAR_FRAME_3 = loc("textures/overlay/armor_plate/bar_frame_3.png")

    override fun shouldRender() = super.shouldRender() && DisplayConfig.ARMOR_PLATE_HUD.get()

    override fun RenderContext.render() {
        val stack = player.getItemBySlot(EquipmentSlot.CHEST)
        if (stack == ItemStack.EMPTY) return
        if (stack.tag == null || !stack.tag!!.contains("ArmorPlate")) return

        var armorLevel = MiscConfig.DEFAULT_ARMOR_LEVEL.get()
        if (stack.`is`(ModTags.Items.MILITARY_ARMOR)) {
            armorLevel = MiscConfig.MILITARY_ARMOR_LEVEL.get()
        } else if (stack.`is`(ModTags.Items.MILITARY_ARMOR_HEAVY)) {
            armorLevel = MiscConfig.HEAVY_MILITARY_ARMOR_LEVEL.get()
        }

        val max = armorLevel * MiscConfig.ARMOR_POINT_PER_LEVEL.get()
        val amount = 60 * (stack.tag!!.getDouble("ArmorPlate") / max)

        val texture: ResourceLocation = when (armorLevel) {
            2 -> BAR_2
            3 -> BAR_3
            else -> BAR_1
        }
        val frame: ResourceLocation = when (armorLevel) {
            2 -> BAR_FRAME_2
            3 -> BAR_FRAME_3
            else -> BAR_FRAME_1
        }

        guiGraphics.pose().pushPose()
        // 渲染图标
        guiGraphics.blit(ICON, 10, screenHeight - 13, 0f, 0f, 8, 8, 8, 8)

        // 渲染框架
        guiGraphics.blit(frame, 20, screenHeight - 12, 0f, 0f, 60, 6, 60, 6)

        // 渲染盔甲值
        guiGraphics.blit(texture, 20, screenHeight - 12, 0f, 0f, amount.toInt(), 6, 60, 6)

        guiGraphics.pose().popPose()
    }
}
