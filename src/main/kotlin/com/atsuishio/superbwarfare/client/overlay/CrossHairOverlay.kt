package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.compat.realcamera.RealCameraCompatHolder
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.perk.AmmoPerk
import com.atsuishio.superbwarfare.perk.IAmmoStat
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.tools.TraceTool
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.math.Axis
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.max
import kotlin.math.min

@OnlyIn(Dist.CLIENT)
object CrossHairOverlay : CommonOverlay("cross_hair") {
    const val CROSSHAIR_EMPTY: String = "@Empty"
    const val CROSSHAIR_CUSTOM: String = "@Custom"
    const val CROSSHAIR_GUN_DEFAULT: String = "@GunDefault"
    const val CROSSHAIR_GUN_REPAIR_TOOL: String = "@GunRepairTool"
    const val CROSSHAIR_GUN_BOCEK: String = "@GunBocek"
    const val CROSSHAIR_GUN_GRENADE: String = "@GunGrenade"

    private val REX = loc("textures/overlay/crosshair/rex.png")
    private val REX_HORIZONTAL = loc("textures/overlay/crosshair/rex_horizontal.png")
    private val REX_VERTICAL = loc("textures/overlay/crosshair/rex_vertical.png")
    private val POINT = loc("textures/overlay/crosshair/point.png")
    private val SHOTGUN = loc("textures/overlay/crosshair/rex_circle.png")
    private val HIT_MARKER = loc("textures/overlay/crosshair/hit_marker.png")
    private val HIT_MARKER_VEHICLE = loc("textures/overlay/crosshair/hit_marker_vehicle.png")
    private val HEADSHOT_MARKER = loc("textures/overlay/crosshair/headshot_marker.png")
    private val KILL_MARKER_1 = loc("textures/overlay/crosshair/kill_marker_1.png")
    private val KILL_MARKER_2 = loc("textures/overlay/crosshair/kill_marker_2.png")
    private val KILL_MARKER_3 = loc("textures/overlay/crosshair/kill_marker_3.png")
    private val KILL_MARKER_4 = loc("textures/overlay/crosshair/kill_marker_4.png")

    @JvmField
    var hitIndicator: Int = 0

    @JvmField
    var headIndicator: Int = 0

    @JvmField
    var killIndicator: Int = 0

    @JvmField
    var vehicleIndicator: Int = 0

    @JvmField
    var gunRot: Float = 0f

    private var scopeScale = 1f

    override fun shouldRender() = super.shouldRender() && !ClientEventHandler.isEditing

    override fun RenderContext.render() {
        val stack = player.mainHandItem
        val vehicle = player.vehicle
        if (stack.item !is GunItem || (vehicle is VehicleEntity && vehicle.banHand(player))) return

        val data = from(stack)

        val crosshair = data.get(GunProp.CROSSHAIR)
        if (crosshair == CROSSHAIR_EMPTY || crosshair == CROSSHAIR_CUSTOM) return

        val spread = ClientEventHandler.gunSpread + 1 * ClientEventHandler.boltMove
        var moveX = 0f
        var moveY = 0f

        var r = 1

        if (com.atsuishio.superbwarfare.tools.mc.options.cameraType != CameraType.FIRST_PERSON) {
            r = 0
        }

        // 平滑准星
        if (DisplayConfig.FLOAT_CROSS_HAIR.get() && vehicle == null) {
            moveX =
                (-6 * ClientEventHandler.turnRot[1] * r - (if (player.isSprinting) 10 else 6) * ClientEventHandler.movePosX).toFloat()
            moveY =
                (-6 * ClientEventHandler.turnRot[0] * r + 6 * ClientEventHandler.velocityY.toFloat() - (if (player.isSprinting) 10 else 6) * ClientEventHandler.movePosY - 0.25 * ClientEventHandler.boltMove).toFloat()
            // 判断RC是否加载，用于适配动态准星
            if (RealCameraCompatHolder.hasMod()) {
                moveX = RealCameraCompatHolder.getCompatMoveX(moveX)
                moveY = RealCameraCompatHolder.getCompatMoveY(moveY)
            }
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

        scopeScale = Mth.lerp((0.5f * deltaFrame).toDouble(), scopeScale.toDouble(), 1 + 1.5f * spread).toFloat()

        val minLength = min(screenWidth, screenHeight).toFloat()
        val scaledMinLength: Float =
            min(screenWidth.toFloat() / minLength, screenHeight.toFloat() / minLength) * 0.012f * scopeScale
        val finLength = Mth.floor(minLength * scaledMinLength).toFloat()
        val finPosX = (screenWidth - finLength) / 2 + moveX
        val finPosY = (screenHeight - finLength) / 2 + moveY

        // 第一人称下的准星
        if (Minecraft.getInstance().options.cameraType == CameraType.FIRST_PERSON) {
            when (crosshair) {
                CROSSHAIR_GUN_DEFAULT -> renderGunDefaultCrosshair(
                    guiGraphics,
                    stack,
                    player,
                    screenWidth,
                    screenHeight,
                    moveX,
                    moveY,
                    finPosX,
                    finPosY,
                    finLength,
                    spread
                )

                CROSSHAIR_GUN_REPAIR_TOOL -> renderRepairToolCrosshair(
                    guiGraphics,
                    data,
                    player,
                    screenWidth,
                    screenHeight,
                    moveX,
                    moveY
                )

                CROSSHAIR_GUN_BOCEK -> renderBocekCrosshair(
                    guiGraphics,
                    data,
                    player,
                    screenWidth,
                    screenHeight,
                    moveX,
                    moveY,
                    finPosX,
                    finPosY,
                    finLength,
                    spread
                )

                CROSSHAIR_GUN_GRENADE -> renderGrenadeCrosshair(guiGraphics, stack, screenWidth, screenHeight)
            }
        }

        // 第三人称下的准星
        if (Minecraft.getInstance().options.cameraType == CameraType.THIRD_PERSON_BACK && (ClientEventHandler.zoomTime > 0 || ClientEventHandler.bowPullPos > 0)) {
            renderGunDefaultCrosshair(
                guiGraphics,
                stack,
                player,
                screenWidth,
                screenHeight,
                moveX,
                moveY,
                finPosX,
                finPosY,
                finLength,
                spread
            )
        }

        // 在开启伤害指示器时才进行渲染
        if (DisplayConfig.KILL_INDICATION.get() && !(vehicle?.type == ModEntities.AH_6.get() && vehicle.firstPassenger === player)) {
            renderKillIndicatorDynamic(guiGraphics, screenWidth, screenHeight, moveX, moveY)
        }

        RenderSystem.depthMask(true)
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }


    /**
     * 渲染标准十字准星
     */
    fun normalCrossHair(guiGraphics: GuiGraphics, w: Int, h: Int, spread: Double, moveX: Float, moveY: Float) {
        val poseStack = guiGraphics.pose()

        poseStack.pushPose()
        poseStack.rotateAround(
            Axis.ZP.rotationDegrees(-gunRot * Mth.RAD_TO_DEG),
            w / 2f + moveX,
            h / 2f + moveY,
            0f
        )

        RenderHelper.preciseBlit(
            guiGraphics,
            REX_HORIZONTAL,
            (w / 2f - 13.5f - 2.8f * spread).toFloat() + moveX,
            h / 2f - 7.5f + moveY,
            0f,
            0f,
            16f,
            16f,
            16f,
            16f
        )
        RenderHelper.preciseBlit(
            guiGraphics,
            REX_HORIZONTAL,
            (w / 2f - 2.5f + 2.8f * spread).toFloat() + moveX,
            h / 2f - 7.5f + moveY,
            0f,
            0f,
            16f,
            16f,
            16f,
            16f
        )
        RenderHelper.preciseBlit(
            guiGraphics,
            REX_VERTICAL,
            w / 2f - 7.5f + moveX,
            (h / 2f - 2.5f + 2.8f * spread).toFloat() + moveY,
            0f,
            0f,
            16f,
            16f,
            16f,
            16f
        )
        RenderHelper.preciseBlit(
            guiGraphics,
            REX_VERTICAL,
            w / 2f - 7.5f + moveX,
            (h / 2f - 13.5f - 2.8f * spread).toFloat() + moveY,
            0f,
            0f,
            16f,
            16f,
            16f,
            16f
        )

        poseStack.popPose()
    }

    /**
     * 渲染圆形准星
     */
    fun shotgunCrossHair(guiGraphics: GuiGraphics?, finPosX: Float, finPosY: Float, finLength: Float) {
        RenderHelper.preciseBlit(
            guiGraphics,
            SHOTGUN,
            finPosX,
            finPosY,
            0f,
            0f,
            finLength,
            finLength,
            finLength,
            finLength
        )
    }

    fun renderGunDefaultCrosshair(
        guiGraphics: GuiGraphics, stack: ItemStack, player: Player, screenWidth: Int, screenHeight: Int,
        moveX: Float, moveY: Float, finPosX: Float, finPosY: Float, finLength: Float, spread: Double
    ) {
        val data = from(stack)

        if (Minecraft.getInstance().options.cameraType == CameraType.FIRST_PERSON) {
            if (ClientEventHandler.zoomTime > 0.8 && GunResource.compute(stack).hideCrosshairWhenZoom) return
        }

        RenderHelper.preciseBlit(
            guiGraphics,
            POINT,
            screenWidth / 2f - 7.5f + moveX,
            screenHeight / 2f - 7.5f + moveY,
            0f,
            0f,
            16f,
            16f,
            16f,
            16f
        )
        if (!player.isSprinting || ClientEventHandler.noSprintTicks > 0) {
            if (data.get(GunProp.PROJECTILE_AMOUNT) > 1) {
                shotgunCrossHair(guiGraphics, finPosX, finPosY, finLength)
            } else {
                normalCrossHair(guiGraphics, screenWidth, screenHeight, spread, moveX, moveY)
            }
        }
    }

    fun renderRepairToolCrosshair(
        guiGraphics: GuiGraphics,
        data: GunData,
        player: Player,
        screenWidth: Int,
        screenHeight: Int,
        moveX: Float,
        moveY: Float
    ) {
        val range = data.get(GunProp.RANGE)
        val lookingEntity = TraceTool.findLookingEntity(player, range.toDouble())

        var health = 0f
        if (lookingEntity is LivingEntity) {
            health = lookingEntity.health / lookingEntity.maxHealth
        } else if (lookingEntity is VehicleEntity) {
            health = lookingEntity.health / lookingEntity.getMaxHealth()
        }

        RenderHelper.preciseBlit(
            guiGraphics,
            POINT,
            screenWidth / 2f - 7.5f + moveX,
            screenHeight / 2f - 7.5f + moveY,
            0f,
            0f,
            16f,
            16f,
            16f,
            16f
        )

        if (health > 0) {
            RenderHelper.renderCircularRing(
                guiGraphics,
                screenWidth / 2f + moveX, screenHeight / 2f + moveY,
                0.035f, 0.028f,
                floatArrayOf(0f, 0f, 0f, 0.4f), floatArrayOf(1f, 1f, 1f, 1f),
                health, true
            )
        }
    }

    fun renderBocekCrosshair(
        guiGraphics: GuiGraphics, data: GunData, player: Player, screenWidth: Int, screenHeight: Int,
        moveX: Float, moveY: Float, finPosX: Float, finPosY: Float, finLength: Float, spread: Double
    ) {
        if (ClientEventHandler.zoomPos >= 0.7) return

        val perk = data.perk.get(Perk.Type.AMMO)

        RenderHelper.preciseBlit(
            guiGraphics,
            POINT,
            screenWidth / 2f - 7.5f + moveX,
            screenHeight / 2f - 7.5f + moveY,
            0f,
            0f,
            16f,
            16f,
            16f,
            16f
        )
        if (!player.isSprinting || ClientEventHandler.noSprintTicks > 0 || ClientEventHandler.bowPullPos > 0) {
            if (ClientEventHandler.zoomTime < 0.1) {
                val isSlug = when (perk) { is AmmoPerk -> perk.slug; is IAmmoStat -> perk.slug; else -> false }
                if (isSlug) {
                    normalCrossHair(guiGraphics, screenWidth, screenHeight, spread, moveX, moveY)
                } else {
                    shotgunCrossHair(guiGraphics, finPosX, finPosY, finLength)
                }
            } else {
                normalCrossHair(guiGraphics, screenWidth, screenHeight, spread, moveX, moveY)
            }
        }
    }

    fun renderGrenadeCrosshair(guiGraphics: GuiGraphics, stack: ItemStack?, screenWidth: Int, screenHeight: Int) {
        if (ClientEventHandler.zoomTime > 0.8 && GunResource.compute(stack).hideCrosshairWhenZoom) return

        guiGraphics.blit(REX, screenWidth / 2 - 16, screenHeight / 2 - 16, 0f, 0f, 32, 32, 32, 32)
    }

    private fun renderKillIndicatorDynamic(guiGraphics: GuiGraphics?, w: Int, h: Int, moveX: Float, moveY: Float) {
        val posX = w / 2f - 7.5f + (2 * (Math.random() - 0.5f)).toFloat()
        val posY = h / 2f - 7.5f + (2 * (Math.random() - 0.5f)).toFloat()
        val rate: Float = (40 - killIndicator * 5) / 5.5f

        if (hitIndicator > 0) {
            RenderHelper.preciseBlit(
                guiGraphics,
                HIT_MARKER,
                posX + moveX,
                posY + moveY,
                0f,
                0f,
                16f,
                16f,
                16f,
                16f
            )
        }

        if (vehicleIndicator > 0) {
            RenderHelper.preciseBlit(
                guiGraphics,
                HIT_MARKER_VEHICLE,
                posX + moveX,
                posY + moveY,
                0f,
                0f,
                16f,
                16f,
                16f,
                16f
            )
        }

        if (headIndicator > 0) {
            RenderHelper.preciseBlit(
                guiGraphics,
                HEADSHOT_MARKER,
                posX + moveX,
                posY + moveY,
                0f,
                0f,
                16f,
                16f,
                16f,
                16f
            )
        }

        if (killIndicator > 0) {
            val posX1 = w / 2f - 7.5f - 2 + rate + moveX
            val posY1 = h / 2f - 7.5f - 2 + rate + moveY
            val posX2 = w / 2f - 7.5f + 2 - rate + moveX
            val posY2 = h / 2f - 7.5f + 2 - rate + moveY

            RenderHelper.preciseBlit(guiGraphics, KILL_MARKER_1, posX1, posY1, 0f, 0f, 16f, 16f, 16f, 16f)
            RenderHelper.preciseBlit(guiGraphics, KILL_MARKER_2, posX2, posY1, 0f, 0f, 16f, 16f, 16f, 16f)
            RenderHelper.preciseBlit(guiGraphics, KILL_MARKER_3, posX1, posY2, 0f, 0f, 16f, 16f, 16f, 16f)
            RenderHelper.preciseBlit(guiGraphics, KILL_MARKER_4, posX2, posY2, 0f, 0f, 16f, 16f, 16f, 16f)
        }
    }

    @JvmStatic
    fun handleRenderDamageIndicator() {
        headIndicator = max(0, headIndicator - 1)
        hitIndicator = max(0, hitIndicator - 1)
        killIndicator = max(0, killIndicator - 1)
        vehicleIndicator = max(0, vehicleIndicator - 1)
    }
}