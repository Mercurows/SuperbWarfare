package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.misc.ArtilleryIndicatorItem
import com.atsuishio.superbwarfare.item.misc.firingParameters
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.VectorTool.lerpGetEntityBoundingBoxCenter
import com.atsuishio.superbwarfare.tools.canBeSeen
import com.atsuishio.superbwarfare.tools.worldToScreen
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.level.ClipContext
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.min

@OnlyIn(Dist.CLIENT)
object SpyglassRangeOverlay : CommonOverlay("spyglass_range") {
    private val INDICATOR = loc("textures/overlay/spyglass/indicator.png")
    private val SPYGLASS = loc("textures/overlay/spyglass/spyglass.png")

    private var scopeScale = 1f
    private var lerpHoldArtilleryIndicator = 0f

    override fun RenderContext.render() {
        val poseStack = guiGraphics.pose()

        val stack = player.getUseItem()
        if (((player.isUsingItem && player.getUseItem()
                .`is`(ModItems.ARTILLERY_INDICATOR.get())) || player.isScoping) && mc.options.cameraType == CameraType.FIRST_PERSON
        ) {
            if (player.getUseItem().`is`(ModItems.ARTILLERY_INDICATOR.get())) {
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

                scopeScale = Mth.lerp(
                    (0.5f * deltaFrame).toDouble(),
                    scopeScale.toDouble(),
                    1.35f + (0.2f * ClientEventHandler.boltMove)
                ).toFloat()
                val f = min(screenWidth, screenHeight).toFloat()
                val f1: Float = min(screenWidth.toFloat() / f, screenHeight.toFloat() / f) * scopeScale
                val i = Mth.floor(f * f1).toFloat()
                val j = Mth.floor(f * f1).toFloat()
                val k = ((screenWidth - i) / 2)
                val l = ((screenHeight - j) / 2)
                val w = i * 21 / 9
                RenderHelper.preciseBlit(guiGraphics, SPYGLASS, k - (2 * w / 7), l, 0f, 0f, w, j, w, j)

                // 标记位置
                val parameters = stack.firingParameters
                val pos = parameters.pos.center
                val point = pos.worldToScreen()
                if (pos.canBeSeen()) {
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
                val tags = stack.getOrCreateTag().getList(ArtilleryIndicatorItem.TAG_CANNON, Tag.TAG_COMPOUND.toInt())
                for (m in tags.indices) {
                    val tag = tags.getCompound(m)
                    val entity = EntityFindUtil.findEntity(player.level(), tag.getString("UUID"))
                    if (entity != null) {
                        val posF = lerpGetEntityBoundingBoxCenter(entity, partialTick)
                        val pointF = posF.worldToScreen()
                        if (posF.canBeSeen()) {
                            val xf = pointF.x.toFloat()
                            val yf = pointF.y.toFloat()
                            RenderHelper.preciseBlit(
                                guiGraphics,
                                IFFOverlay.FRIENDLY_ARTILLERY,
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

                poseStack.popPose()

                lerpHoldArtilleryIndicator =
                    Mth.lerp(partialTick, lerpHoldArtilleryIndicator, 0.05f * ClientEventHandler.holdArtilleryIndicator)

                if (lerpHoldArtilleryIndicator > 0) {
                    val alpha = Mth.clamp(lerpHoldArtilleryIndicator * 20, 0f, 5f) * 0.2f
                    RenderHelper.renderCircularRing(
                        guiGraphics,
                        screenWidth / 2f, screenHeight / 2f,
                        0.07f, 0.052f,
                        floatArrayOf(0f, 0f, 0f, 0.4f * alpha),
                        floatArrayOf(1f, 1f, 1f, 0.8f * alpha),
                        lerpHoldArtilleryIndicator,
                        true
                    )
                }
            }

            var lookAtEntity = false

            val result = player.level().clip(
                ClipContext(
                    player.eyePosition, player.eyePosition.add(player.getViewVector(1f).scale(512.0)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
                )
            )
            val hitPos = result.getLocation()

            val blockRange = player.getEyePosition(1f).distanceTo(hitPos)

            var entityRange = 0.0
            val lookingEntity = TraceTool.findLookingEntity(player, 520.0)

            if (lookingEntity is VehicleEntity) return

            if (lookingEntity != null) {
                lookAtEntity = true
                entityRange = player.distanceTo(lookingEntity).toDouble()
            }

            if (lookAtEntity) {
                guiGraphics.drawString(
                    Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.drone.range")
                        .append(
                            Component.literal(
                                format1D(entityRange, "M ") + lookingEntity!!.displayName.string
                            )
                        ),
                    screenWidth / 2 + 12, screenHeight / 2 - 28, -1, false
                )
            } else {
                if (blockRange > 500) {
                    guiGraphics.drawString(
                        Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.drone.range")
                            .append(Component.literal("---M")), screenWidth / 2 + 12, screenHeight / 2 - 28, -1, false
                    )
                } else {
                    guiGraphics.drawString(
                        Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.drone.range")
                            .append(Component.literal(format1D(blockRange, "M"))),
                        screenWidth / 2 + 12, screenHeight / 2 - 28, -1, false
                    )
                }
            }
        } else {
            scopeScale = 1f
        }
    }
}
