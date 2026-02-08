package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.worldToScreen
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object RedTriangleOverlay : CommonOverlay("red_triangle") {
    private val TRIANGLE = loc("textures/overlay/rpg/red_triangle.png")

    override fun RenderContext.render() {
        val poseStack = guiGraphics.pose()

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.banHand(player)) return

        val stack = player.mainHandItem
        if (stack.`is`(ModItems.RPG.get()) && from(stack).selectedAmmoType.get() == 0) {
            val idf = SeekTool.seekLivingEntity(player, 128.0, 6.0) ?: return

            val distance = idf.position().distanceTo(cameraPos)
            val pos = Vec3(
                Mth.lerp(partialTick.toDouble(), idf.xo, idf.x),
                Mth.lerp(
                    partialTick.toDouble(),
                    idf.yo + idf.eyeHeight + 0.5 + 0.07 * distance,
                    idf.eyeY + 0.5 + 0.07 * distance
                ),
                Mth.lerp(partialTick.toDouble(), idf.zo, idf.z)
            )
            val point = pos.worldToScreen()

            poseStack.pushPose()
            val x = point.x.toFloat()
            val y = point.y.toFloat()
            RenderHelper.blit(poseStack, TRIANGLE, x - 4, y - 4, 0f, 0f, 8f, 8f, 8f, 8f, -65536)
            RenderSystem.depthMask(true)
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableDepthTest()
            RenderSystem.disableBlend()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            poseStack.popPose()
        }
    }
}
