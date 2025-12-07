package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.VectorUtil
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.client.player.LocalPlayer
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import javax.annotation.ParametersAreNonnullByDefault

@OnlyIn(Dist.CLIENT)
object RedTriangleOverlay : LayeredDraw.Layer {
    @JvmField
    val ID: ResourceLocation = loc("red_triangle")

    private val TRIANGLE = loc("textures/overlay/rpg/red_triangle.png")

    @ParametersAreNonnullByDefault
    override fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
        if (Minecraft.getInstance().options.hideGui) return
        val mc = Minecraft.getInstance()
        val poseStack = guiGraphics.pose()
        val camera = mc.gameRenderer.mainCamera
        val cameraPos = camera.position

        val player: LocalPlayer = mc.player ?: return
        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.banHand(player)) return

        val stack = player.mainHandItem
        if (stack.`is`(ModItems.RPG.get()) && from(stack).selectedAmmoType.get() == 0) {
            val idf = SeekTool.seekLivingEntity(player, 128.0, 6.0) ?: return

            val distance = idf.position().distanceTo(cameraPos)
            val pos = Vec3(
                Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true).toDouble(), idf.xo, idf.x),
                Mth.lerp(
                    deltaTracker.getGameTimeDeltaPartialTick(true).toDouble(),
                    idf.yo + idf.eyeHeight + 0.5 + 0.07 * distance,
                    idf.eyeY + 0.5 + 0.07 * distance
                ),
                Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(true).toDouble(), idf.zo, idf.z)
            )
            val point = VectorUtil.worldToScreen(pos)

            poseStack.pushPose()
            val x = point.x.toFloat()
            val y = point.y.toFloat()

            RenderHelper.preciseBlit(guiGraphics, TRIANGLE, x - 4, y - 4, 8f, 8f, 0f, 0f, 8f, 8f, 8f, 8f)

            RenderSystem.depthMask(true)
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableDepthTest()
            RenderSystem.disableBlend()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            poseStack.popPose()
        }
    }
}
