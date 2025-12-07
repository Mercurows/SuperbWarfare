package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.VectorTool.lerpGetEntityBoundingBoxCenter
import com.atsuishio.superbwarfare.tools.VectorUtil
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import javax.annotation.ParametersAreNonnullByDefault
import kotlin.math.min

@OnlyIn(Dist.CLIENT)
object JavelinHudOverlay : LayeredDraw.Layer {
    @JvmField
    val ID: ResourceLocation = loc("javelin_hud")

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

    @ParametersAreNonnullByDefault
    override fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
        if (Minecraft.getInstance().options.hideGui) return
        val w = guiGraphics.guiWidth()
        val h = guiGraphics.guiHeight()
        val player: Player? = Minecraft.getInstance().player
        val poseStack = guiGraphics.pose()

        if (player == null) return
        val stack = player.mainHandItem

        if (ClientEventHandler.isEditing) return
        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.banHand(player)) return

        if ((stack.item === ModItems.JAVELIN.get() && ClientEventHandler.zoomPos > 0.8) && Minecraft.getInstance().options.cameraType
                .isFirstPerson && ClientEventHandler.zoom
        ) {
            val data = from(stack)
            val tag = data.tag()

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

            val deltaFrame = deltaTracker.getGameTimeDeltaPartialTick(true)
            val moveX =
                (-32 * ClientEventHandler.turnRot[1] - (if (player.isSprinting) 100 else 67) * ClientEventHandler.movePosX + 3 * ClientEventHandler.cameraRot[2]).toFloat()
            val moveY =
                (-32 * ClientEventHandler.turnRot[0] + 100 * ClientEventHandler.velocityY.toFloat() - (if (player.isSprinting) 100 else 67) * ClientEventHandler.movePosY - 12 * ClientEventHandler.firePos + 3 * ClientEventHandler.cameraRot[1]).toFloat()
            scopeScale = Mth.lerp(
                (0.5f * deltaFrame).toDouble(),
                scopeScale.toDouble(),
                1.35f + (0.2f * ClientEventHandler.firePos)
            ).toFloat()
            val f = min(w, h).toFloat()
            val f1: Float = min(w.toFloat() / f, h.toFloat() / f) * scopeScale
            val i = Mth.floor(f * f1).toFloat()
            val j = Mth.floor(f * f1).toFloat()
            val k = ((w - i) / 2) + moveX
            val l = ((h - j) / 2) + moveY
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
            if (tag.getInt("SeekTime") in 2..<20) {
                RenderHelper.preciseBlit(guiGraphics, SEEK, k, l, 0f, 0f, i, j, i, j)
            }

            guiGraphics.fill(RenderType.guiOverlay(), 0, l.toInt(), k.toInt() + 3, j1.toInt(), -90, -16777216)
            guiGraphics.fill(RenderType.guiOverlay(), i1.toInt(), l.toInt(), w, j1.toInt(), -90, -16777216)
            RenderSystem.depthMask(true)
            RenderSystem.defaultBlendFunc()
            RenderSystem.enableDepthTest()
            RenderSystem.disableBlend()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            val mc = Minecraft.getInstance()
            val camera = mc.gameRenderer.mainCamera
            val cameraPos = camera.position

            val decoy = TraceTool.findLookDecoy(
                player,
                cameraPos,
                player.getViewVector(deltaTracker.getGameTimeDeltaPartialTick(true)),
                512.0
            )

            if (decoy == null) {
                val targetEntity = ClientEventHandler.lockingEntity
                val entities = SeekTool.Builder(player)
                    .withinRange(data.compute().seekRange)
                    .withinAngle(data.compute().seekAngle)
                    .baseFilter()
                    .heightRange(data.compute().minTargetHeight, data.compute().maxTargetHeight)
                    .smokeFilter()
                    .noVehicle()
                    .noClip()
                    .notFriendly()
                    .build()
                val nearestEntity = ClientEventHandler.nearestEntity

                if (ClientEventHandler.guideType == 0) {
                    for (e in entities) {
                        val pos = lerpGetEntityBoundingBoxCenter(e, deltaTracker.getGameTimeDeltaPartialTick(true))
                        val point = VectorUtil.worldToScreen(pos)
                        val lockOn = ClientEventHandler.lockOn && e === targetEntity
                        val nearest = e === nearestEntity

                        poseStack.pushPose()
                        val x = point.x.toFloat()
                        val y = point.y.toFloat()

                        RenderHelper.preciseBlitWithColor(
                            guiGraphics,
                            if (lockOn) FRAME_LOCK else if (nearest) FRAME_TARGET else FRAME,
                            x - 12,
                            y - 12,
                            0f,
                            0f,
                            24f,
                            24f,
                            24f,
                            24f,
                            -0x1
                        )
                        poseStack.popPose()
                    }
                } else {
                    val pos = ClientEventHandler.lockingPos
                    val lockOn = ClientEventHandler.lockOn
                    if (pos != null) {
                        val point = VectorUtil.worldToScreen(pos)
                        if (VectorUtil.canSee(pos)) {
                            poseStack.pushPose()
                            val x = point.x.toFloat()
                            val y = point.y.toFloat()

                            RenderHelper.preciseBlitWithColor(
                                guiGraphics,
                                if (lockOn) FRAME_LOCK else FRAME_TARGET,
                                x - 12,
                                y - 12,
                                0f,
                                0f,
                                24f,
                                24f,
                                24f,
                                24f,
                                -0x1
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