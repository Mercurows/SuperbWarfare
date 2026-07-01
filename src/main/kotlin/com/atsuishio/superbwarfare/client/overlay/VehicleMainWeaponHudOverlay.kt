package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.overlay.weapon.*
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.tools.MathTool.getGradientColor
import com.atsuishio.superbwarfare.tools.RangeTool.calculateFiringSolution
import com.atsuishio.superbwarfare.tools.VectorTool.lerpGetEntityBoundingBoxCenter
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import kotlin.math.cos
import kotlin.math.sin

/**
 * 控制载具主武器的玩家显示的HUD
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(Dist.CLIENT)
object VehicleMainWeaponHudOverlay : CommonOverlay("vehicle_main_weapon_hud") {
    const val EMPTY = "@Empty"

    private var lerpLock = 1f

    @JvmField
    var lock: Boolean = false

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

    var entities: List<Entity>? = null

    override fun shouldRender() = super.shouldRender() && !ClientEventHandler.isEditing

    @SubscribeEvent
    fun onVehicleMainWeaponHudOverlayClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.START) return
        val player = localPlayer ?: return
        val vehicle = player.vehicle
        if (vehicle !is VehicleEntity) return
        val gunData = vehicle.getGunData(player) ?: return
        val seekInfo = gunData.get(GunProp.SEEK_WEAPON_INFO) ?: return

        val camera = mc.gameRenderer.mainCamera
        val cameraPos = camera.position
        val seekVec = vehicle.getSeekVec(player, 1f)

        entities = SeekTool.Builder(player)
            .withinRangeSeekWeapon(seekInfo.seekRange, seekInfo.maxGuidedRange, seekInfo.affectedByStealthTarget, seekInfo.canGuidedByRadar)
            .withinAngle(cameraPos, seekVec, seekInfo.seekAngle)
            .baseFilter()
            .heightRange(seekInfo.minTargetHeight, seekInfo.maxTargetHeight)
            .sizeBiggerThan(seekInfo.minTargetSize)
            .smokeFilter()
            .noVehicle()
            .noClip()
            .notFriendly()
            .buildSeekWeapon(seekInfo.canGuidedByRadar)
    }

    override fun RenderContext.render() {
        val poseStack = guiGraphics.pose()
        poseStack.pushPose()

        val vehicle = player.vehicle
        if (vehicle !is VehicleEntity) {
            poseStack.popPose()
            return
        }


        val type: String = vehicle.computed().hudType
        if (type == EMPTY) {
            poseStack.popPose()
            return
        }

        when (type) {
            LandVehicleHud.ID -> LandVehicleHud.render(
                vehicle,
                player,
                gui,
                guiGraphics,
                partialTick,
                screenWidth,
                screenHeight
            )

            HelicopterHud.ID -> HelicopterHud.render(
                vehicle,
                player,
                gui,
                guiGraphics,
                partialTick,
                screenWidth,
                screenHeight
            )

            ArtilleryHud.ID -> ArtilleryHud.render(
                vehicle,
                player,
                gui,
                guiGraphics,
                partialTick,
                screenWidth,
                screenHeight
            )

            AircraftHud.ID -> AircraftHud.render(
                vehicle,
                player,
                gui,
                guiGraphics,
                partialTick,
                screenWidth,
                screenHeight
            )

            OldAircraftHud.ID -> OldAircraftHud.render(
                vehicle,
                player,
                gui,
                guiGraphics,
                partialTick,
                screenWidth,
                screenHeight
            )

            KirovHud.ID -> KirovHud.render(
                vehicle,
                player,
                gui,
                guiGraphics,
                partialTick,
                screenWidth,
                screenHeight
            )
        }

        val gunData = vehicle.getGunData(player)
        if (gunData == null) {
            poseStack.popPose()
            return
        }


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



        val seekInfo = gunData.get(GunProp.SEEK_WEAPON_INFO)
        val color = gunData.get(GunProp.CROSSHAIR_COLOR).get()
        if (seekInfo == null) {
            poseStack.popPose()
            return
        }

        val level = clientLevel

        val seekTime = seekInfo.seekTime

        if (seekInfo.onlyLockEntity && entities != null) {
            val targetEntity = ClientEventHandler.lockingEntityVehicle
            var nearestEntity = ClientEventHandler.nearestEntityVehicle

            for (en in entities!!) {
                val e = level!!.getEntity(en.id) ?: en
                if (e.type.`is`(ModTags.EntityTypes.DECOY)) continue

                val pos3 = if (level.getEntity(e.id) != null)
                    lerpGetEntityBoundingBoxCenter(e, partialTick)
                else
                    ClientSyncedEntityHandler.getExtrapolatedPos(level, e)
                        .add(0.0, e.bbHeight / 2.0, 0.0)

                val decoy = TraceTool.findLookDecoy(player, cameraPos, cameraPos.vectorTo(pos3).normalize(), seekInfo.seekRange)

                if (decoy == null && pos3.canBeSeen() && !seekInfo.onlyLockBlock) {
                    val point = pos3.worldToScreen()
                    val lockOn = ClientEventHandler.lockOnVehicle && targetEntity != null && e.id == targetEntity.id
                    val nearest =
                        e.id == (if (ClientEventHandler.seekingEntityVehicle == null) nearestEntity?.id else ClientEventHandler.seekingEntityVehicle?.id)

                    poseStack.pushPose()
                    val x = point.x.toFloat()
                    val y = point.y.toFloat()

                    if (lockOn) {
                        lock = true

                        poseStack.pushPose()
                        poseStack.translate(x, y, 0f)
                        val str = "${e.displayName.string} [${FormatTool.format1D(e.distanceTo(player).toDouble())}m]"
                        guiGraphics.drawString(
                            mc.font,
                            str,
                            -mc.font.width(str) / 2,
                            -20,
                            0xFFBD7F,
                            false
                        )
                        poseStack.popPose()

                        RenderHelper.blit(poseStack, FRAME_LOCK, x - 12, y - 12, 0f, 0f, 24f, 24f, 24f, 24f, 1f)
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
                            val point0 = shootPos.worldToScreen()

                            if (shootPos.canBeSeen()) {
                                poseStack.pushPose()
                                val x0 = point0.x.toFloat()
                                val y0 = point0.y.toFloat()

                                val targetHudPos = Vec3(x.toDouble(), y.toDouble(), 0.0)
                                val shootHudPos = Vec3(x0.toDouble(), y0.toDouble(), 0.0)

                                RenderHelper.blit(
                                    poseStack,
                                    SHOOT_INDICATOR,
                                    x0 - 12,
                                    y0 - 12,
                                    0f,
                                    0f,
                                    24f,
                                    24f,
                                    24f,
                                    24f,
                                    1f
                                )
                                poseStack.popPose()

                                val dis = targetHudPos.distanceTo(shootHudPos)
                                var i = 3.0
                                while (i < dis - 3) {
                                    val toVec = targetHudPos.vectorTo(shootHudPos).normalize()
                                    val p0 = targetHudPos.add(toVec.scale(i))
                                    RenderHelper.blit(
                                        poseStack,
                                        BLOCK,
                                        (p0.x - 0.5).toFloat(),
                                        (p0.y - 0.5).toFloat(),
                                        0f,
                                        0f,
                                        1f,
                                        1f,
                                        1f,
                                        1f,
                                        1f
                                    )
                                    i += 3.0
                                }
                            }
                        }
                    } else if (nearest && !lock) {
                        lerpLock = Mth.lerp(partialTick, lerpLock, ClientEventHandler.seekingTimeVehicle.toFloat())
                        val lockTime = ((seekTime - lerpLock) * (20f / seekTime)).coerceIn(0f, 20f)
                        if (ClientEventHandler.seekingTimeVehicle > 0) {
                            RenderHelper.blit(
                                poseStack,
                                IND_1,
                                x - 12,
                                y - 12 - lockTime,
                                0f,
                                0f,
                                24f,
                                24f,
                                24f,
                                24f,
                                1f
                            )
                            RenderHelper.blit(
                                poseStack,
                                IND_2,
                                x - 12,
                                y - 12 + lockTime,
                                0f,
                                0f,
                                24f,
                                24f,
                                24f,
                                24f,
                                1f
                            )
                            RenderHelper.blit(
                                poseStack,
                                IND_3,
                                x - 12 - lockTime,
                                y - 12,
                                0f,
                                0f,
                                24f,
                                24f,
                                24f,
                                24f,
                                1f
                            )
                            RenderHelper.blit(
                                poseStack,
                                IND_4,
                                x - 12 + lockTime,
                                y - 12,
                                0f,
                                0f,
                                24f,
                                24f,
                                24f,
                                24f,
                                1f
                            )
                        }

                        if (ClientEventHandler.seekingTimeVehicle == 0) {
                            poseStack.pushPose()
                            poseStack.translate(x, y, 0f)
                            val string = "[" + ModKeyMappings.VEHICLE_SEEK.key.displayName.string + "]"
                            val width = mc.font.width(string)
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

                        poseStack.pushPose()
                        poseStack.translate(x, y, 0f)
                        val str = "${e.displayName.string} [${FormatTool.format1D(e.distanceTo(player).toDouble())}m]"
                        guiGraphics.drawString(
                            mc.font,
                            str,
                            -mc.font.width(str) / 2,
                            -20,
                            0xFFBD7F,
                            false
                        )
                        poseStack.popPose()

                        RenderHelper.blit(
                            poseStack,
                            if (ClientEventHandler.seekingTimeVehicle > 0) FRAME_TARGET else FRAME_TARGET_TRIANGLE,
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
                    } else {
                        RenderHelper.blit(poseStack, FRAME_GREEN, x - 12, y - 12, 0f, 0f, 24f, 24f, 24f, 24f, 1f)
                    }
                    poseStack.popPose()
                }
            }
        } else if (seekInfo.onlyLockBlock) {
            val pos = ClientEventHandler.lockingPosVehicle
            if (pos != null) {
                val lockOn = ClientEventHandler.lockOnVehicle
                val point = pos.worldToScreen()
                if (pos.canBeSeen()) {
                    poseStack.pushPose()
                    val x = point.x.toFloat()
                    val y = point.y.toFloat()
                    lerpLock = Mth.lerp(partialTick, lerpLock, ClientEventHandler.seekingTimeVehicle.toFloat())
                    val lockTime = ((seekTime - lerpLock) * (20f / seekTime)).coerceIn(0f, 20f)
                    if (ClientEventHandler.seekingTimeVehicle > 0 && !lockOn) {
                        RenderHelper.blit(poseStack, IND_1, x - 12, y - 12 - lockTime, 0f, 0f, 24f, 24f, 24f, 24f, 1f)
                        RenderHelper.blit(poseStack, IND_2, x - 12, y - 12 + lockTime, 0f, 0f, 24f, 24f, 24f, 24f, 1f)
                        RenderHelper.blit(poseStack, IND_3, x - 12 - lockTime, y - 12, 0f, 0f, 24f, 24f, 24f, 24f, 1f)
                        RenderHelper.blit(poseStack, IND_4, x - 12 + lockTime, y - 12, 0f, 0f, 24f, 24f, 24f, 24f, 1f)
                    }

                    if (ClientEventHandler.seekingTimeVehicle == 0) {
                        poseStack.pushPose()
                        poseStack.translate(x, y, 0f)
                        val string = "[" + ModKeyMappings.VEHICLE_SEEK.key.displayName.string + "]"
                        val width = mc.font.width(string)
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
            } else {
                poseStack.pushPose()
                poseStack.translate((screenWidth / 2).toDouble(), (screenHeight / 2).toDouble(), 0.0)

                RenderHelper.blit(
                    poseStack,
                    FRAME_TARGET,
                    -12f,
                    -12f,
                    0f,
                    0f,
                    24f,
                    24f,
                    24f,
                    24f,
                    1f
                )

                val string = "[" + ModKeyMappings.VEHICLE_SEEK.key.displayName.string + "]"
                val width = mc.font.width(string)
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
        } else if (seekInfo.inputBlockPos) {
            poseStack.pushPose()
            poseStack.translate((screenWidth / 2).toDouble() + 90, (screenHeight / 2).toDouble(), 0.0)

            val stringX = "X: " + (ClientEventHandler.missileLockingPos?.x ?: "---")
            guiGraphics.drawString(
                mc.font,
                stringX,
                0,
                -17,
                color,
                false
            )

            val stringY = "Y: " + (ClientEventHandler.missileLockingPos?.y ?: "---")
            guiGraphics.drawString(
                mc.font,
                stringY,
                0,
                -8,
                color,
                false
            )

            val stringZ = "Z: " + (ClientEventHandler.missileLockingPos?.z ?: "---")
            guiGraphics.drawString(
                mc.font,
                stringZ,
                0,
                1,
                color,
                false
            )

            val string = Component.translatable("tips.superbwarfare.input_missile_target", ModKeyMappings.EDIT_MODE.translatedKeyMessage)
            guiGraphics.drawString(
                mc.font,
                string,
                0,
                10,
                color,
                false
            )
            poseStack.popPose()
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

    @JvmStatic
    fun renderWeaponInfoThirdAir(
        guiGraphics: GuiGraphics,
        vehicle: VehicleEntity,
        player: Player?,
        data: GunData,
        font: Font
    ) {
        if (!vehicle.hasWeapon()) return

        val heat = vehicle.getWeaponHeat(player) / 100f
        val component = vehicle.thirdPersonAmmoComponent(data, player)
        val length = font.width(component)

        guiGraphics.drawString(font, component, -length / 2, -9, Mth.hsvToRgb(0f, heat, 1f), false)
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
