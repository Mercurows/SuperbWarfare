package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.overlay.IFFOverlay.FRIENDLY_ARTILLERY
import com.atsuishio.superbwarfare.client.overlay.IFFOverlay.FRIENDLY_INDICATOR
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.misc.ArtilleryIndicatorItem
import com.atsuishio.superbwarfare.item.misc.firingParameters
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.VectorTool.lerpGetEntityBoundingBoxCenter
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object DroneHudOverlay : CommonOverlay("drone_hud") {
    private val FRAME = loc("textures/overlay/frame/frame.png")
    private val TV_FRAME = loc("textures/overlay/vehicle/land/tv_frame.png")
    private val CROSSHAIR = loc("textures/overlay/vehicle/crosshair/third_camera.png")
    private val DRONE_FOV = loc("textures/overlay/drone/drone_fov.png")
    private val DRONE_FOV_MOVE = loc("textures/overlay/drone/drone_fov_move.png")

    private val INDICATOR = loc("textures/overlay/spyglass/indicator.png")

    val maxDistance: Int
        get() {
            return (mc.connection?.serverSimulationDistance ?: 16) * 16
        }

    override fun RenderContext.render() {
        val poseStack = guiGraphics.pose()
        val stack = player.mainHandItem

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

        val tag = NBTTool.getTag(stack)
        val firstPerson =
            Minecraft.getInstance().options.cameraType == CameraType.FIRST_PERSON || Minecraft.getInstance().options.cameraType == CameraType.THIRD_PERSON_BACK

        if (stack.`is`(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) {
            if (firstPerson) {
                guiGraphics.blit(CROSSHAIR, screenWidth / 2 - 16, screenHeight / 2 - 16, 0f, 0f, 32, 32, 32, 32)
                guiGraphics.blit(DRONE_FOV, screenWidth / 2 + 100, screenHeight / 2 - 64, 0f, 0f, 64, 129, 64, 129)
                val addW = (screenWidth / screenHeight) * 48
                val addH = (screenWidth / screenHeight) * 27
                RenderHelper.preciseBlit(
                    guiGraphics,
                    TV_FRAME,
                    -addW.toFloat() / 2,
                    -addH.toFloat() / 2,
                    10f,
                    0f,
                    0f,
                    (screenWidth + addW).toFloat(),
                    (screenHeight + addH).toFloat(),
                    (screenWidth + addW).toFloat(),
                    (screenHeight + addH).toFloat()
                )

                RenderHelper.preciseBlit(
                    guiGraphics,
                    DRONE_FOV_MOVE,
                    screenWidth.toFloat() / 2 + 100,
                    (screenHeight / 2f - 64 - ((ClientEventHandler.droneFovLerp - 1) * 23.8)).toFloat(),
                    0f,
                    0f,
                    64f,
                    129f,
                    64f,
                    129f
                )
                guiGraphics.drawString(
                    mc.font,
                    Component.literal(format1D(ClientEventHandler.droneFovLerp, "x")),
                    screenWidth / 2 + 144,
                    screenHeight / 2 + 56 - ((ClientEventHandler.droneFovLerp - 1) * 23.8).toInt(),
                    -1,
                    false
                )

                val entity = EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone"))

                if (entity != null) {
                    var lookAtEntity = false
                    val distance = player.position().subtract(entity.position()).horizontalDistance()

                    val result = entity.level().clip(
                        ClipContext(
                            cameraPos, cameraPos.add(entity.getViewVector(1f).scale(512.0)),
                            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity
                        )
                    )
                    val hitPos = result.getLocation()

                    val blockRange = cameraPos.distanceTo(hitPos)

                    var entityRange = 0.0

                    val lookingEntity = TraceTool.droneFindLookingEntity(entity, cameraPos, 512.0, partialTick)
                    if (lookingEntity != null) {
                        lookAtEntity = true
                        entityRange = entity.distanceTo(lookingEntity).toDouble()
                    }

                    var color = -1

                    // 超出距离警告
                    if (distance > maxDistance - 48) {
                        guiGraphics.drawString(
                            mc.font, Component.translatable("tips.superbwarfare.drone.warning"),
                            screenWidth / 2 - 18, screenHeight / 2 - 47, -65536, false
                        )
                        color = -65536
                    }

                    // 距离
                    guiGraphics.drawString(
                        mc.font, Component.translatable("tips.superbwarfare.drone.distance")
                            .append(Component.literal(format1D(distance, "m"))),
                        screenWidth / 2 + 10, screenHeight / 2 + 33, color, false
                    )

                    // 血量
                    guiGraphics.drawString(
                        mc.font, Component.translatable("tips.superbwarfare.drone.health")
                            .append(
                                Component.literal(
                                    format1D(entity.health.toDouble()) + " / " + format1D(
                                        entity.getMaxHealth().toDouble()
                                    )
                                )
                            ),
                        screenWidth / 2 - 77, screenHeight / 2 + 33, -1, false
                    )
                    if (!entity.getEntityData().get(DroneEntity.IS_KAMIKAZE)) {
                        // 弹药
                        guiGraphics.drawString(
                            mc.font, Component.translatable("tips.superbwarfare.drone.ammo")
                                .append(
                                    Component.literal(
                                        entity.ammo.toString() + " / " + entity.getEntityData()
                                            .get<Int?>(DroneEntity.MAX_AMMO)
                                    )
                                ),
                            screenWidth / 2 + 12, screenHeight / 2 - 37, -1, false
                        )
                    } else {
                        // 神风
                        guiGraphics.drawString(
                            mc.font, Component.translatable("tips.superbwarfare.drone.kamikaze"),
                            screenWidth / 2 + 12, screenHeight / 2 - 37, -65536, false
                        )
                    }

                    if (lookAtEntity) {
                        // 实体距离
                        guiGraphics.drawString(
                            mc.font, Component.translatable("tips.superbwarfare.drone.range")
                                .append(
                                    Component.literal(
                                        format1D(
                                            entityRange,
                                            "m "
                                        ) + lookingEntity!!.displayName!!.string
                                    )
                                ),
                            screenWidth / 2 + 12, screenHeight / 2 - 28, color, false
                        )
                    } else {
                        // 方块距离
                        if (blockRange > 500) {
                            guiGraphics.drawString(
                                mc.font,
                                Component.translatable("tips.superbwarfare.drone.range")
                                    .append(Component.literal("---m")),
                                screenWidth / 2 + 12,
                                screenHeight / 2 - 28,
                                color,
                                false
                            )
                        } else {
                            guiGraphics.drawString(
                                mc.font, Component.translatable("tips.superbwarfare.drone.range")
                                    .append(Component.literal(format1D(blockRange, "m"))),
                                screenWidth / 2 + 12, screenHeight / 2 - 28, color, false
                            )
                        }
                    }

                    val entities = SeekTool.seekLivingEntities(entity, 256.0, 30.0)
                    for (e in entities) {
                        val pos = Vec3(
                            Mth.lerp(partialTick.toDouble(), e.xo, e.x),
                            Mth.lerp(partialTick.toDouble(), e.yo + e.eyeHeight, e.eyeY),
                            Mth.lerp(partialTick.toDouble(), e.zo, e.z)
                        )
                        val point = pos.worldToScreen()
                        poseStack.pushPose()
                        val x = point.x.toFloat()
                        val y = point.y.toFloat()

                        RenderHelper.preciseBlit(
                            guiGraphics,
                            FRAME,
                            x - 12,
                            y - 12,
                            24f,
                            24f,
                            0f,
                            0f,
                            24f,
                            24f,
                            24f,
                            24f
                        )
                        poseStack.popPose()
                    }
                }

                // 射击诸元标记
                val offStack = player.offhandItem
                if (offStack.`is`(ModItems.FIRING_PARAMETERS, ModItems.ARTILLERY_INDICATOR)) {
                    val parameters = offStack.firingParameters
                    val blockPos = parameters.pos

                    val targetX = blockPos.x.toDouble()
                    val targetY = blockPos.y.toDouble()
                    val targetZ = blockPos.z.toDouble()

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

                    // 标记位置
                    val pos = Vec3(targetX, targetY, targetZ)
                    if (pos.canBeSeen()) {
                        val point = pos.worldToScreen()
                        val x = point.x.toFloat()
                        val y = point.y.toFloat()
                        RenderHelper.preciseBlit(
                            guiGraphics,
                            INDICATOR,
                            Mth.clamp(x - 6, 0f, (screenWidth - 12).toFloat()),
                            Mth.clamp(y - 6, 0f, (screenHeight - 12).toFloat()),
                            0f,
                            0f,
                            12f,
                            12f,
                            12f,
                            12f
                        )
                    }

                    // 火炮位置
                    if (offStack.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
                        val tags =
                            NBTTool.getTag(offStack)
                                .getList(ArtilleryIndicatorItem.TAG_CANNON, Tag.TAG_COMPOUND.toInt())
                        for (m in tags.indices) {
                            val tag = tags.getCompound(m)
                            val e = EntityFindUtil.findEntity(player.level(), tag.getString("UUID"))
                            if (e != null && e.position().canBeSeen()) {
                                val posF = lerpGetEntityBoundingBoxCenter(e, partialTick)
                                val pointF = posF.worldToScreen()
                                val xf = pointF.x.toFloat()
                                val yf = pointF.y.toFloat()

                                RenderHelper.preciseBlit(
                                    guiGraphics,
                                    FRIENDLY_ARTILLERY,
                                    Mth.clamp(xf - 6, 0f, (screenWidth - 12).toFloat()),
                                    Mth.clamp(yf - 6, 0f, (screenHeight - 12).toFloat()),
                                    0f,
                                    0f,
                                    12f,
                                    12f,
                                    12f,
                                    12f
                                )
                            }
                        }
                    }
                }
            } else {
                if (player.position().canBeSeen()) {
                    var team: Entity? = player
                    if (player.vehicle != null) {
                        team = player.vehicle
                    }
                    val pos = lerpGetEntityBoundingBoxCenter(team!!, partialTick)
                    val point = pos.worldToScreen()
                    val xf = point.x.toFloat()
                    val yf = point.y.toFloat()

                    RenderHelper.preciseBlit(
                        guiGraphics,
                        FRIENDLY_INDICATOR,
                        Mth.clamp(xf - 6, 0f, (screenWidth - 12).toFloat()),
                        Mth.clamp(yf - 6, 0f, (screenHeight - 12).toFloat()),
                        0f,
                        0f,
                        12f,
                        12f,
                        12f,
                        12f
                    )
                }
            }
        }

        RenderSystem.depthMask(true)
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        poseStack.popPose()
    }
}
