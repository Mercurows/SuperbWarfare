package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.overlay.weapon.AircraftHud
import com.atsuishio.superbwarfare.client.overlay.weapon.ArtilleryHud
import com.atsuishio.superbwarfare.client.overlay.weapon.HelicopterHud
import com.atsuishio.superbwarfare.client.overlay.weapon.LandVehicleHud
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.MathTool.getGradientColor
import com.atsuishio.superbwarfare.tools.RangeTool.calculateFiringSolution
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.VectorTool.lerpGetEntityBoundingBoxCenter
import com.atsuishio.superbwarfare.tools.VectorUtil
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import kotlin.math.cos
import kotlin.math.sin

/**
 * 控制载具主武器的玩家显示的HUD
 */
@OnlyIn(Dist.CLIENT)
object VehicleMainWeaponHudOverlay : LayeredDraw.Layer {

    @JvmField
    val ID = loc("vehicle_main_weapon_hud")
    const val EMPTY = "@Empty"

    private var lerpLock = 1f

    @JvmField
    var lock = false

    private val FRAME_GREEN = loc("textures/overlay/frame/frame_green.png")
    private val FRAME_TARGET = loc("textures/overlay/frame/frame_target.png")
    private val FRAME_TARGET_TRIANGLE = loc("textures/overlay/frame/frame_target_triangle.png")
    private val FRAME_LOCK = loc("textures/overlay/frame/frame_lock.png")

    private val IND_1 = loc("textures/overlay/vehicle/aircraft/locking_ind1.png")
    private val IND_2 = loc("textures/overlay/vehicle/aircraft/locking_ind2.png")
    private val IND_3 = loc("textures/overlay/vehicle/aircraft/locking_ind3.png")
    private val IND_4 = loc("textures/overlay/vehicle/aircraft/locking_ind4.png")

    private val SHOOT_INDICATOR = loc("textures/overlay/frame/frame_diamond.png")
    private val BLOCK = loc("textures/overlay/misc/block.png")

    override fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
        val mc = Minecraft.getInstance()
        if (mc.options.hideGui) return

        val player = mc.player ?: return
        val vehicle = player.vehicle
        if (vehicle !is VehicleEntity) return
        if (ClientEventHandler.isEditing) return

        val type: String = vehicle.computed().hudType
        if (type == EMPTY) return

        val gunData = vehicle.getGunData(player) ?: return

        val poseStack = guiGraphics.pose()
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

        val partialTick = deltaTracker.getGameTimeDeltaPartialTick(true)
        val screenWidth = guiGraphics.guiWidth()
        val screenHeight = guiGraphics.guiHeight()

        when (type) {
            LandVehicleHud.ID -> LandVehicleHud.render(
                vehicle,
                player,
                guiGraphics,
                partialTick,
                screenWidth,
                screenHeight
            )

            HelicopterHud.ID -> HelicopterHud.render(
                vehicle,
                player,
                guiGraphics,
                partialTick,
                screenWidth,
                screenHeight
            )

            ArtilleryHud.ID -> ArtilleryHud.render(vehicle, player, guiGraphics, partialTick, screenWidth, screenHeight)
            AircraftHud.ID -> AircraftHud.render(vehicle, player, guiGraphics, partialTick, screenWidth, screenHeight)
        }

        val seekInfo = gunData.compute().seekWeaponInfo
        if (seekInfo == null) {
            poseStack.popPose()
            return
        }

        val camera = mc.gameRenderer.mainCamera
        val cameraPos = camera.position
        val seekTime = seekInfo.seekTime

        if (seekInfo.onlyLockEntity) {
            val targetEntity = ClientEventHandler.lockingEntityVehicle
            var nearestEntity = ClientEventHandler.nearestEntityVehicle
            val seekVec: Vec3? = vehicle.getSeekVec(player, partialTick)

            val entities = SeekTool.Builder(vehicle)
                .withinRange(seekInfo.seekRange)
                .withinAngle(cameraPos, seekVec, seekInfo.seekAngle)
                .baseFilter()
                .heightRange(seekInfo.minTargetHeight, seekInfo.maxTargetHeight)
                .sizeBiggerThan(seekInfo.minTargetSize)
                .smokeFilter()
                .noVehicle()
                .noClip()
                .notFriendly()
                .build()

            val decoy = TraceTool.findLookDecoy(player, cameraPos, seekVec, seekInfo.seekRange)

            if (decoy != null && decoy.type.`is`(ModTags.EntityTypes.DECOY)) return

            for (e in entities) {
                if (e.type.`is`(ModTags.EntityTypes.DECOY)) continue

                val pos3 = lerpGetEntityBoundingBoxCenter(e, partialTick)
                if (VectorUtil.canSee(pos3) && !seekInfo.onlyLockBlock) {
                    val point = VectorUtil.worldToScreen(pos3)
                    val lockOn = ClientEventHandler.lockOnVehicle && targetEntity != null && e === targetEntity
                    val nearest =
                        e === (if (ClientEventHandler.seekingEntityVehicle == null) nearestEntity else ClientEventHandler.seekingEntityVehicle)

                    poseStack.pushPose()
                    val x = point.x.toFloat()
                    val y = point.y.toFloat()

                    if (lockOn) {
                        lock = true
                        RenderHelper.preciseBlitWithColor(
                            guiGraphics,
                            FRAME_LOCK,
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
                        nearestEntity = targetEntity
                        if (seekInfo.calculateTrajectory) {
                            val shootVector = calculateFiringSolution(
                                vehicle.getShootPos(player, partialTick),
                                lerpGetEntityBoundingBoxCenter(targetEntity, partialTick),
                                targetEntity.deltaMovement.scale(1.25),
                                vehicle.getProjectileVelocity(player).toDouble(),
                                vehicle.getProjectileGravity(player).toDouble()
                            ).normalize()
                            val shootPos: Vec3 = vehicle.getShootPos(player, partialTick).add(
                                shootVector.scale(
                                    vehicle.getShootPos(player, partialTick).distanceTo(
                                        lerpGetEntityBoundingBoxCenter(targetEntity, partialTick)
                                    )
                                )
                            )
                            val point0 = VectorUtil.worldToScreen(shootPos)

                            if (VectorUtil.canSee(shootPos)) {
                                poseStack.pushPose()
                                val x0 = point0.x.toFloat()
                                val y0 = point0.y.toFloat()

                                val targetHudPos = Vec3(x.toDouble(), y.toDouble(), 0.0)
                                val shootHudPos = Vec3(x0.toDouble(), y0.toDouble(), 0.0)

                                RenderHelper.preciseBlitWithColor(
                                    guiGraphics,
                                    SHOOT_INDICATOR,
                                    x0 - 12,
                                    y0 - 12,
                                    0f,
                                    0f,
                                    24f,
                                    24f,
                                    24f,
                                    24f,
                                    -0x1
                                )
                                poseStack.popPose()

                                val dis = targetHudPos.distanceTo(shootHudPos)
                                var i = 3.0
                                while (i < dis - 3) {
                                    val toVec = targetHudPos.vectorTo(shootHudPos).normalize()
                                    val p0 = targetHudPos.add(toVec.scale(i))
                                    RenderHelper.preciseBlitWithColor(
                                        guiGraphics,
                                        BLOCK,
                                        (p0.x - 0.5).toFloat(),
                                        (p0.y - 0.5).toFloat(),
                                        0f,
                                        0f,
                                        1f,
                                        1f,
                                        1f,
                                        1f,
                                        -0x1
                                    )
                                    i += 3.0
                                }
                            }
                        }
                    } else if (nearest && !lock) {
                        lerpLock = Mth.lerp(partialTick, lerpLock, ClientEventHandler.seekingTimeVehicle.toFloat())
                        val lockTime = Mth.clamp((seekTime - lerpLock) * (20f / seekTime), 0f, 20f)
                        if (ClientEventHandler.seekingTimeVehicle > 0) {
                            RenderHelper.preciseBlitWithColor(
                                guiGraphics,
                                IND_1,
                                x - 12,
                                y - 12 - lockTime,
                                0f,
                                0f,
                                24f,
                                24f,
                                24f,
                                24f,
                                -0x1
                            )
                            RenderHelper.preciseBlitWithColor(
                                guiGraphics,
                                IND_2,
                                x - 12,
                                y - 12 + lockTime,
                                0f,
                                0f,
                                24f,
                                24f,
                                24f,
                                24f,
                                -0x1
                            )
                            RenderHelper.preciseBlitWithColor(
                                guiGraphics,
                                IND_3,
                                x - 12 - lockTime,
                                y - 12,
                                0f,
                                0f,
                                24f,
                                24f,
                                24f,
                                24f,
                                -0x1
                            )
                            RenderHelper.preciseBlitWithColor(
                                guiGraphics,
                                IND_4,
                                x - 12 + lockTime,
                                y - 12,
                                0f,
                                0f,
                                24f,
                                24f,
                                24f,
                                24f,
                                -0x1
                            )
                        }

                        if (ClientEventHandler.seekingTimeVehicle == 0) {
                            poseStack.pushPose()
                            poseStack.translate(x, y, 0f)
                            val string = "[" + ModKeyMappings.VEHICLE_SEEK.key.displayName.string + "]"
                            val width = Minecraft.getInstance().font.width(string)
                            guiGraphics.drawString(
                                mc.font,
                                string,
                                -width / 2,
                                10,
                                0xFFBD7F,
                                false
                            )
                            poseStack.popPose()
                        }

                        RenderHelper.preciseBlitWithColor(
                            guiGraphics,
                            if (ClientEventHandler.seekingTimeVehicle > 0) FRAME_TARGET else FRAME_TARGET_TRIANGLE,
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
                    } else {
                        RenderHelper.preciseBlitWithColor(
                            guiGraphics,
                            FRAME_GREEN,
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
                    }
                    poseStack.popPose()
                }
            }
        } else {
            val pos = ClientEventHandler.lockingPosVehicle
            if (pos != null) {
                val lockOn = ClientEventHandler.lockOnVehicle
                val point = VectorUtil.worldToScreen(pos)
                if (VectorUtil.canSee(pos)) {
                    poseStack.pushPose()
                    val x = point.x.toFloat()
                    val y = point.y.toFloat()
                    lerpLock = Mth.lerp(partialTick, lerpLock, ClientEventHandler.seekingTimeVehicle.toFloat())
                    val lockTime = Mth.clamp((seekTime - lerpLock) * (20f / seekTime), 0f, 20f)
                    if (ClientEventHandler.seekingTimeVehicle > 0 && !lockOn) {
                        RenderHelper.preciseBlitWithColor(
                            guiGraphics,
                            IND_1,
                            x - 12,
                            y - 12 - lockTime,
                            0f,
                            0f,
                            24f,
                            24f,
                            24f,
                            24f,
                            -0x1
                        )
                        RenderHelper.preciseBlitWithColor(
                            guiGraphics,
                            IND_2,
                            x - 12,
                            y - 12 + lockTime,
                            0f,
                            0f,
                            24f,
                            24f,
                            24f,
                            24f,
                            -0x1
                        )
                        RenderHelper.preciseBlitWithColor(
                            guiGraphics,
                            IND_3,
                            x - 12 - lockTime,
                            y - 12,
                            0f,
                            0f,
                            24f,
                            24f,
                            24f,
                            24f,
                            -0x1
                        )
                        RenderHelper.preciseBlitWithColor(
                            guiGraphics,
                            IND_4,
                            x - 12 + lockTime,
                            y - 12,
                            0f,
                            0f,
                            24f,
                            24f,
                            24f,
                            24f,
                            -0x1
                        )
                    }

                    if (ClientEventHandler.seekingTimeVehicle == 0) {
                        poseStack.pushPose()
                        poseStack.translate(x, y, 0f)
                        val string = "[" + ModKeyMappings.VEHICLE_SEEK.key.displayName.string + "]"
                        val width = Minecraft.getInstance().font.width(string)
                        guiGraphics.drawString(
                            mc.font,
                            string,
                            -width / 2,
                            10,
                            0xFFBD7F,
                            false
                        )
                        poseStack.popPose()
                    }

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

        poseStack.popPose()
    }

    /**
     * 通用渲染方法，在低电量时渲染警告
     */
    @JvmStatic
    fun renderEnergyInfo(
        vehicle: VehicleEntity,
        guiGraphics: GuiGraphics,
        screenWidth: Int,
        screenHeight: Int,
        font: Font
    ) {
        if (!vehicle.hasEnergyStorage()) return

        if (vehicle.energy < 0.02 * vehicle.maxEnergy) {
            guiGraphics.drawString(
                font,
                Component.literal("NO POWER!"),
                screenWidth / 2 - 144,
                screenHeight / 2 + 14,
                -65536,
                false
            )
        } else if (vehicle.energy < 0.2 * vehicle.maxEnergy) {
            guiGraphics.drawString(
                font,
                Component.literal("LOW POWER"),
                screenWidth / 2 - 144,
                screenHeight / 2 + 14,
                0xFF6B00,
                false
            )
        }
    }

    // TODO 正确显示文本和备弹数量，正确判断是否应该显示武器名称
    @JvmStatic
    fun renderWeaponInfoFirst(
        guiGraphics: GuiGraphics,
        vehicle: VehicleEntity,
        player: Player?,
        data: GunData,
        font: Font,
        screenWidth: Int,
        screenHeight: Int,
        color: Int
    ) {
        val heat = vehicle.getWeaponHeat(player)
        val component = vehicle.firstPersonAmmoComponent(data, player)

        guiGraphics.drawString(
            font, component, (screenWidth - font.width(component)) / 2, screenHeight - 65,
            getGradientColor(color, 0xFF0000, heat, 2), false
        )
    }

    @JvmStatic
    fun renderWeaponInfoThird(
        guiGraphics: GuiGraphics,
        vehicle: VehicleEntity,
        player: Player?,
        data: GunData,
        font: Font
    ) {
        if (!vehicle.hasWeapon()) return

        val heat = vehicle.getWeaponHeat(player) / 100f
        val component = vehicle.thirdPersonAmmoComponent(data, player)

        guiGraphics.drawString(font, component, 30, -9, Mth.hsvToRgb(0f, heat, 1f), false)
    }

    fun getAroundPos(direction: Vec3, center: Vec3, radius: Double): Vec3 {
        var direction = direction
        direction = direction.normalize()

        // 构建垂直正交基
        val randomPerp: Vec3 = getRandomPerpendicular(direction)
        val u = randomPerp.normalize()
        val v = direction.cross(u).normalize()

        val theta = 2 * Math.PI
        val xOffset = radius * (cos(theta) * u.x + sin(theta) * v.x)
        val yOffset = radius * (cos(theta) * u.y + sin(theta) * v.y)
        val zOffset = radius * (cos(theta) * u.z + sin(theta) * v.z)

        return center.add(xOffset, yOffset, zOffset)
    }

    private fun getRandomPerpendicular(dir: Vec3): Vec3 {
        val candidate1 = Vec3(dir.y, -dir.x, 0.0) // 在XY平面垂直
        if (candidate1.lengthSqr() > 1e-4) return candidate1
        return Vec3(0.0, dir.z, -dir.y) // 备用垂直向量
    }
}
