package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModPerks
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.VectorUtil
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.gui.overlay.ForgeGui
import net.minecraftforge.client.gui.overlay.IGuiOverlay

@OnlyIn(Dist.CLIENT)
object HandsomeFrameOverlay : IGuiOverlay {
    const val ID: String = Mod.MODID + "_handsome_frame"

    private val FRAME = loc("textures/overlay/frame/frame.png")
    private val FRAME_WEAK = loc("textures/overlay/frame/frame_weak.png")
    private val FRAME_TARGET = loc("textures/overlay/frame/frame_target_triangle.png")
    private val FRAME_LOCK = loc("textures/overlay/frame/frame_lock.png")

    override fun render(
        gui: ForgeGui,
        guiGraphics: GuiGraphics,
        partialTick: Float,
        screenWidth: Int,
        screenHeight: Int
    ) {
        val player: Player? = gui.getMinecraft().player
        val poseStack = guiGraphics.pose()

        if (player == null) return
        if (player.isSpectator) return

        val stack = player.mainHandItem

        if (ClientEventHandler.isEditing) return
        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.banHand(player)) return

        if (stack.item is GunItem && Minecraft.getInstance().options.cameraType.isFirstPerson) {
            val level = from(stack).perk!!.getLevel(ModPerks.INTELLIGENT_CHIP).toInt()
            if (level == 0) return

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

            val allEntities = SeekTool.seekLivingEntitiesThroughWall(player, (32 + 8 * (level - 1)).toDouble(), 30.0)
            val visibleEntities = SeekTool.seekLivingEntities(player, (32 + 8 * (level - 1)).toDouble(), 30.0)

            val nearestEntity = SeekTool.seekLivingEntity(player, (32 + 8 * (level - 1)).toDouble(), 30.0)
            val targetEntity = ClientEventHandler.lockedEntity

            for (e in allEntities) {
                val pos = Vec3(
                    Mth.lerp(partialTick.toDouble(), e.xo, e.x),
                    Mth.lerp(partialTick.toDouble(), e.yo + e.eyeHeight, e.eyeY),
                    Mth.lerp(partialTick.toDouble(), e.zo, e.z)
                )
                val point = VectorUtil.worldToScreen(pos)

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

                RenderHelper.blit(poseStack, icon, x - 12, y - 12, 0f, 0f, 24f, 24f, 24f, 24f, 1f)
                poseStack.popPose()
            }
        }
    }
}
