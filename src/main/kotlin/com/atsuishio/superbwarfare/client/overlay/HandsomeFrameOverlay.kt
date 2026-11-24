package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.data.gun.ActiveGun
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.worldToScreen
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object HandsomeFrameOverlay : CommonOverlay("handsome_frame") {
    private val FRAME = loc("textures/overlay/frame/frame.png")
    private val FRAME_WEAK = loc("textures/overlay/frame/frame_weak.png")
    private val FRAME_TARGET = loc("textures/overlay/frame/frame_target_triangle.png")
    private val FRAME_LOCK = loc("textures/overlay/frame/frame_lock.png")

    override fun RenderContext.render() {
        val poseStack = guiGraphics.pose()
        // 自瞄锁定框跟随当前操控的枪，包括部署中的副武器。
        val stack = ActiveGun.stackOf(player)

        if (ClientEventHandler.isEditing) return
        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.banHand(player)) return

        if (GunItem.isOperable(stack) && isFirstPerson) {
            val data = from(stack)
            if (!data.get(GunProp.AUTO_AIM)) return
            val range = data.get(GunProp.AUTO_AIM_RANGE)

            RenderSystem.disableDepthTest()
            RenderSystem.depthMask(false)
            RenderSystem.enableBlend()
            RenderSystem.setShader { GameRenderer.getPositionTexShader() }
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
            )
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            val allEntities = SeekTool.seekLivingEntitiesThroughWall(player, range, 30.0)
            val visibleEntities = SeekTool.seekLivingEntities(player, range, 30.0)

            val nearestEntity = SeekTool.seekLivingEntity(player, range, 30.0)
            val targetEntity = ClientEventHandler.lockedEntity

            for (e in allEntities) {
                val pos = Vec3(
                    Mth.lerp(partialTick.toDouble(), e.xo, e.x),
                    Mth.lerp(partialTick.toDouble(), e.yo + e.eyeHeight, e.eyeY),
                    Mth.lerp(partialTick.toDouble(), e.zo, e.z)
                )
                val point = pos.worldToScreen()

                val lockOn = e === targetEntity
                val isNearestEntity = e === nearestEntity

                poseStack.pushPose()
                val x = point.x.toFloat()
                val y = point.y.toFloat()

                val canBeSeen = visibleEntities.contains(e)
                val icon = if (lockOn) {
                    FRAME_LOCK
                } else if (canBeSeen) {
                    if (isNearestEntity) {
                        FRAME_TARGET
                    } else {
                        FRAME
                    }
                } else {
                    FRAME_WEAK
                }

                RenderHelper.blit(poseStack, icon, x - 12, y - 12, 24f, 24f)
                poseStack.popPose()
            }
        }
    }
}
