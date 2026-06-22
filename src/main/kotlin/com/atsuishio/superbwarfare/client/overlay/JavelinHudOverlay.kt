package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.VectorTool.lerpGetEntityBoundingBoxCenter
import com.atsuishio.superbwarfare.tools.canBeSeen
import com.atsuishio.superbwarfare.tools.seekQuery
import com.atsuishio.superbwarfare.tools.worldToScreen
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.RenderType
import net.minecraft.util.Mth
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.min

@OnlyIn(Dist.CLIENT)
object JavelinHudOverlay : CommonOverlay("javelin_hud") {
    private val FRAME = loc("textures/overlay/frame/frame.png")
    private val FRAME_TARGET = loc("textures/overlay/frame/frame_target_triangle.png")
    private val FRAME_LOCK = loc("textures/overlay/frame/frame_lock.png")
    private val JAVELIN_HUD = loc("textures/overlay/javelin/javelin_hud.png")
    private val TOP = loc("textures/overlay/javelin/top.png")
    private val DIR = loc("textures/overlay/javelin/dir.png")
    private val MISSILE_GREEN = loc("textures/overlay/javelin/missile_green.png")
    private val MISSILE_RED = loc("textures/overlay/javelin/missile_red.png")
    private val SEEK = loc("textures/overlay/javelin/seek.png")

    private var scopeScale = 1f

    override fun shouldRender() = super.shouldRender() && !ClientEventHandler.isEditing

    override fun RenderContext.render() {
        val poseStack = guiGraphics.pose()
        val stack = player.mainHandItem

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.banHand(player)) return

        if (stack.item === ModItems.JAVELIN.get() && ClientEventHandler.zoomPos > 0.8 && isFirstPerson && ClientEventHandler.zoom) {
            val data = from(stack)

            poseStack.pushPose()

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

            val moveX =
                (-32 * ClientEventHandler.turnRot[1] - (if (player.isSprinting) 100 else 67) * ClientEventHandler.movePosX + 3 * ClientEventHandler.cameraRot[2]).toFloat()
            val moveY =
                (-32 * ClientEventHandler.turnRot[0] + 100 * ClientEventHandler.velocityY.toFloat() - (if (player.isSprinting) 100 else 67) * ClientEventHandler.movePosY - 12 * ClientEventHandler.boltMove + 3 * ClientEventHandler.cameraRot[1]).toFloat()
            scopeScale = Mth.lerp(
                (0.5f * deltaFrame).toDouble(),
                scopeScale.toDouble(),
                1.35f + (0.2f * ClientEventHandler.boltMove)
            ).toFloat()
            val f = min(screenWidth, screenHeight).toFloat()
            val f1: Float = min(screenWidth.toFloat() / f, screenHeight.toFloat() / f) * scopeScale
            val i = Mth.floor(f * f1).toFloat()
            val j = Mth.floor(f * f1).toFloat()
            val k = ((screenWidth - i) / 2) + moveX
            val l = ((screenHeight - j) / 2) + moveY
            val i1 = k + i
            val j1 = l + j
            RenderHelper.preciseBlit(guiGraphics, JAVELIN_HUD, k, l, 0f, 0f, i, j, i, j)
            RenderHelper.preciseBlit(
                guiGraphics,
                if (data.selectedFireModeInfo().name == "Top") TOP else DIR,
                k,
                l,
                0f,
                0f,
                i,
                j,
                i,
                j
            )
            RenderHelper.preciseBlit(
                guiGraphics,
                if (data.hasEnoughAmmoToShoot(player)) MISSILE_GREEN else MISSILE_RED,
                k,
                l,
                0f,
                0f,
                i,
                j,
                i,
                j
            )
            if (stack.getOrCreateTag().getInt("SeekTime") in 2..<20) {
                RenderHelper.preciseBlit(guiGraphics, SEEK, k, l, 0f, 0f, i, j, i, j)
            }

            guiGraphics.fill(RenderType.guiOverlay(), 0, l.toInt(), k.toInt() + 3, j1.toInt(), -90, -16777216)
            guiGraphics.fill(RenderType.guiOverlay(), i1.toInt(), l.toInt(), screenWidth, j1.toInt(), -90, -16777216)
            RenderSystem.depthMask(true)
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableDepthTest()
            RenderSystem.disableBlend()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            val decoy = TraceTool.findLookDecoy(player, cameraPos, player.getViewVector(partialTick), 512.0)

            if (decoy == null) {
                val targetEntity = ClientEventHandler.lockingEntity
                val entities = player.seekQuery {
                    withinRangeSeekWeapon(
                        data.get(GunProp.SEEK_RANGE),
                        data.get(GunProp.MAX_GUIDED_RANGE),
                        data.get(GunProp.AFFECTED_BY_STEALTH_TARGET),
                        data.get(GunProp.CAN_GUIDED_BY_RADAR)
                    )
                    withinAngle(data.get(GunProp.SEEK_ANGLE))
                    baseFilter()
                    heightRange(data.get(GunProp.MIN_TARGET_HEIGHT), data.get(GunProp.MAX_TARGET_HEIGHT))
                    smokeFilter()
                    noVehicle()
                    noClip()
                    notFriendly()
                }.buildSeekWeapon(data.get(GunProp.CAN_GUIDED_BY_RADAR))
                val nearestEntity = ClientEventHandler.nearestEntity

                if (ClientEventHandler.guideType == 0) {
                    for (e in entities) {
                        val pos = lerpGetEntityBoundingBoxCenter(e, partialTick)
                        val point = pos.worldToScreen()
                        val lockOn = ClientEventHandler.lockOn && e.id == targetEntity?.id
                        val nearest = e === nearestEntity

                        poseStack.pushPose()
                        val x = point.x.toFloat()
                        val y = point.y.toFloat()

                        RenderHelper.blit(
                            poseStack,
                            if (lockOn) FRAME_LOCK else if (nearest) FRAME_TARGET else FRAME,
                            x - 12,
                            y - 12,
                            0f,
                            0f,
                            24f,
                            24f,
                            24f,
                            24f,
                            1f
                        )
                        poseStack.popPose()
                    }
                } else {
                    val pos = ClientEventHandler.lockingPos
                    val lockOn = ClientEventHandler.lockOn
                    if (pos != null) {
                        val point = pos.worldToScreen()
                        if (pos.canBeSeen()) {
                            poseStack.pushPose()
                            val x = point.x.toFloat()
                            val y = point.y.toFloat()

                            RenderHelper.blit(
                                poseStack,
                                if (lockOn) FRAME_LOCK else FRAME_TARGET,
                                x - 12,
                                y - 12,
                                0f,
                                0f,
                                24f,
                                24f,
                                24f,
                                24f,
                                1f
                            )
                            poseStack.popPose()
                        }
                    }
                }
            }
            poseStack.popPose()
        } else {
            scopeScale = 1f
        }
    }
}
