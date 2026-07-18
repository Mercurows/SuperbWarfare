package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.VectorTool
import com.atsuishio.superbwarfare.tools.canBeSeen
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.atsuishio.superbwarfare.tools.worldToScreen
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent

/**
 * PJM: 敌我识别 —— 坐在载具里时，友方载具上方显示绿色三角标记
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
object FriendlyVehicleMarkerOverlay : CommonOverlay("friendly_vehicle_marker") {
    private val TRIANGLE = loc("textures/overlay/teammate/friendly_triangle.png")
    private const val COLOR = 0x7FFFAD
    private const val RANGE = 256.0

    /**
     * 友方载具及其是否被遮挡，每tick更新一次，避免每帧遍历实体和射线检测
     */
    private var markers = listOf<Pair<VehicleEntity, Boolean>>()

    private val localPlayerVehicle get() = localPlayer?.vehicle as? VehicleEntity

    /**
     * 有瞄准HUD的机型：固定翼/直升机武器位没有ZoomPosition，但有瞄准HUD
     */
    private val HUD_TYPES_WITH_SIGHT = setOf("@Aircraft", "@Helicopter")

    /**
     * 仅在能瞄准的座位上显示标记：
     * 带瞄准镜(ZoomPosition)的座位，或有瞄准HUD机型的武器位。
     * 皮卡/卡车/快艇等既无瞄准镜也无瞄准HUD，不显示。
     */
    private fun VehicleEntity.hasScope(passenger: Entity): Boolean {
        val seatIndex = getSeatIndex(passenger)
        val seat = computed().seats().getOrNull(seatIndex) ?: return false
        if (seat.cameraPos?.zoomPosition != null) return true
        return computed().hudType in HUD_TYPES_WITH_SIGHT && hasWeapon(seatIndex)
    }

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        val player = localPlayer
        val ownVehicle = localPlayerVehicle
        if (player == null || ownVehicle == null || !ownVehicle.hasScope(player)) {
            markers = emptyList()
            return
        }

        val cameraPos = mc.gameRenderer.mainCamera.position
        markers = SeekTool.Builder(player)
            .friendly()
            .`is`(VehicleEntity::class.java)
            .withinRange(RANGE)
            .build()
            .filterIsInstance<VehicleEntity>()
            .filter { it !== ownVehicle && !it.isWreck }
            .map { it to IFFOverlay.checkNoClip(player, it, cameraPos) }
    }

    override fun shouldRender() = super.shouldRender() && localPlayerVehicle != null && markers.isNotEmpty()

    override fun RenderContext.render() {
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

        for ((vehicle, noClip) in markers) {
            if (vehicle.isRemoved) continue

            val pos = markerPos(vehicle)
            if (!pos.canBeSeen()) continue

            // 被墙挡住时半透明
            RenderSystem.setShaderColor(1f, 1f, 1f, if (noClip) 1f else 0.4f)

            val point = pos.worldToScreen()
            RenderHelper.preciseBlitWithColor(
                guiGraphics,
                TRIANGLE,
                (point.x.toFloat() - 4).coerceIn(0f, (screenWidth - 8).toFloat()),
                (point.y.toFloat() - 4).coerceIn(0f, (screenHeight - 8).toFloat()),
                0f,
                0f,
                8f,
                8f,
                8f,
                8f,
                COLOR
            )
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        RenderSystem.depthMask(true)
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
    }

    private fun RenderContext.markerPos(vehicle: Entity): Vec3 =
        VectorTool.lerpGetEntityBoundingBoxCenter(vehicle, partialTick)
            .add(0.0, vehicle.bbHeight / 2.0 + 0.5, 0.0)
}
