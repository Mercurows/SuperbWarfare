package com.atsuishio.superbwarfare.client.overlay.weapon

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getXRotFromVector
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getYRotFromVector
import com.atsuishio.superbwarfare.tools.FormatTool
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.TraceTool
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.gui.overlay.ForgeGui

@OnlyIn(Dist.CLIENT)
object ArtilleryHud {
    const val ID: String = "@Artillery"
    private val COMPASS = loc("textures/overlay/vehicle/base/compass.png")
    private val ROLL_IND_WHITE = loc("textures/overlay/vehicle/cannon/roll_ind_white.png")
    private val CANNON_PITCH = loc("textures/overlay/vehicle/cannon/cannon_pitch.png")
    private val CANNON_PITCH_IND = loc("textures/overlay/vehicle/cannon/cannon_pitch_ind.png")

    fun render(
        vehicle: VehicleEntity,
        player: Player,
        gui: ForgeGui,
        guiGraphics: GuiGraphics,
        partialTick: Float,
        screenWidth: Int,
        screenHeight: Int
    ) {
        val mc = gui.getMinecraft()

        if (vehicle.getSeatIndex(player) != vehicle.computed().turretControllerIndex) return

        val index = vehicle.getSeatIndex(player)
        vehicle.getGunData(index) ?: return

        val poseStack = guiGraphics.pose()
        val camera = mc.gameRenderer.mainCamera
        val cameraPos = camera.position
        val viewVec = Vec3(camera.lookVector)

        poseStack.pushPose()

        val yaw = -getYRotFromVector(vehicle.getBarrelVector(partialTick))
        val pitch = -getXRotFromVector(vehicle.getBarrelVector(partialTick))

        RenderHelper.preciseBlit(
            guiGraphics,
            COMPASS,
            screenWidth.toFloat() / 2 - 128,
            10f,
            128 + (64f / 45 * yaw.toFloat()),
            0f,
            256f,
            16f,
            512f,
            16f
        )
        RenderHelper.preciseBlit(
            guiGraphics,
            ROLL_IND_WHITE,
            screenWidth.toFloat() / 2 - 4,
            27f,
            0f,
            0f,
            8f,
            8f,
            8f,
            8f
        )

        val width = Minecraft.getInstance().font.width(FormatTool.DECIMAL_FORMAT_1ZZ.format(yaw))
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            Component.literal(FormatTool.DECIMAL_FORMAT_1ZZ.format(yaw)),
            screenWidth / 2 - width / 2,
            40,
            -1,
            false
        )

        RenderHelper.preciseBlit(
            guiGraphics,
            CANNON_PITCH,
            screenWidth.toFloat() / 2 + 166,
            screenHeight.toFloat() / 2 - 64,
            0f,
            0f,
            8f,
            128f,
            8f,
            128f
        )

        val widthP = Minecraft.getInstance().font.width(FormatTool.DECIMAL_FORMAT_1ZZ.format(pitch))

        poseStack.pushPose()

        guiGraphics.pose().translate(0.0, pitch * 0.7, 0.0)
        RenderHelper.preciseBlit(
            guiGraphics,
            CANNON_PITCH_IND,
            screenWidth.toFloat() / 2 + 158,
            screenHeight.toFloat() / 2 - 4,
            0f,
            0f,
            8f,
            8f,
            8f,
            8f
        )
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            Component.literal(FormatTool.DECIMAL_FORMAT_1ZZ.format(pitch)),
            screenWidth / 2 + 157 - widthP,
            screenHeight / 2 - 4,
            -1,
            false
        )

        poseStack.popPose()

        var shootPos = player.getEyePosition(partialTick)

        if (vehicle !is AnnihilatorEntity) {
            shootPos = vehicle.getZoomPos(player, partialTick)
        }

        val lookingEntity = TraceTool.cameraFindLookingEntity(player, cameraPos, viewVec, 512.0)
        var lookAtEntity = false

        val result = player.level().clip(
            ClipContext(
                shootPos, shootPos.add(player.getViewVector(1f).scale(512.0)),
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player
            )
        )
        val hitPos = result.getLocation()

        val blockRange = player.getEyePosition(1f).distanceTo(hitPos)
        var entityRange = 0.0

        if (lookingEntity is LivingEntity) {
            lookAtEntity = true
            entityRange = player.distanceTo(lookingEntity).toDouble()
        }

        if (lookAtEntity) {
            guiGraphics.drawString(
                Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.drone.range")
                    .append(
                        Component.literal(
                            format1D(entityRange, "m ") + lookingEntity!!.displayName.string
                        )
                    ),
                screenWidth / 2 + 14, screenHeight / 2 - 20, -1, false
            )
        } else {
            if (blockRange > 511) {
                guiGraphics.drawString(
                    Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.drone.range")
                        .append(Component.literal("---m")), screenWidth / 2 + 14, screenHeight / 2 - 20, -1, false
                )
            } else {
                guiGraphics.drawString(
                    Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.drone.range")
                        .append(Component.literal(format1D(blockRange, "m"))),
                    screenWidth / 2 + 14, screenHeight / 2 - 20, -1, false
                )
            }
        }
        poseStack.popPose()
    }
}
