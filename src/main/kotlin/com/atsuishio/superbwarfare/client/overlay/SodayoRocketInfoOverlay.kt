package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.entity.vehicle.SodayoPickUpRocketEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.tools.OBB
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.worldToScreen
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object SodayoRocketInfoOverlay : CommonOverlay("sodayo_rocket_info") {
    private val AP by lazy { ItemStack(ModItems.MEDIUM_ROCKET_AP.get()) }
    private val HE by lazy { ItemStack(ModItems.MEDIUM_ROCKET_HE.get()) }
    private val CM by lazy { ItemStack(ModItems.MEDIUM_ROCKET_CM.get()) }

    override fun RenderContext.render() {
        val poseStack = guiGraphics.pose()

        val lookingEntity = TraceTool.findLookingEntity(player, player.entityInteractionRange())
        if (lookingEntity !is SodayoPickUpRocketEntity) return

        val items = lookingEntity.getEntityData().get(SodayoPickUpRocketEntity.LOADED_AMMO)
        for (i in lookingEntity.barrel.indices) {
            if (OBB.getLookingObb(player, player.entityInteractionRange()) === lookingEntity.barrel[i]) {
                val type: Int = items[i]!!

                val stack = when (type) {
                    0 -> AP
                    1 -> HE
                    2 -> CM
                    else -> ItemStack.EMPTY
                }

                val pos = OBB.vector3dToVec3(lookingEntity.barrel[i].center)
                val point = pos.worldToScreen()

                poseStack.pushPose()
                val x = point.x.toFloat()
                val y = point.y.toFloat()

                var component = stack.hoverName

                if (stack.isEmpty) {
                    component = Component.translatable("tips.superbwarfare.barrel_empty")
                    val width = Minecraft.getInstance().font.width(component)

                    poseStack.translate(x - width / 2f, y, 0f)
                    guiGraphics.drawString(Minecraft.getInstance().font, component, 0, 0, -1, false)
                } else {
                    val width = Minecraft.getInstance().font.width(component) + 20

                    poseStack.pushPose()
                    poseStack.translate(x - width / 2f, y, 0f)
                    guiGraphics.renderFakeItem(stack, 0, 0)

                    poseStack.translate(20f, 4f, 0f)
                    guiGraphics.drawString(Minecraft.getInstance().font, component, 0, 0, -1, false)
                }

                poseStack.popPose()
            }
        }
    }
}
