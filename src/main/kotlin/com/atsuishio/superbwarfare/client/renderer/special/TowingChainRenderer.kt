package com.atsuishio.superbwarfare.client.renderer.special

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.compat.valkyrienskies.ValkyrienSkiesCompat
import com.atsuishio.superbwarfare.entity.misc.CatapultShuttleEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.clientLevel
import com.atsuishio.superbwarfare.tools.mc
import com.atsuishio.superbwarfare.tools.options
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Matrix4f

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object TowingChainRenderer {

    // Length of each chain link in blocks
    private const val LINK_LENGTH = 0.5f

    // Chain ribbon half-width in blocks (total visible width = 2 * HALF_WIDTH)
    private const val HALF_WIDTH = 0.25f

    private val CHAIN_TEXTURE = loc("textures/item/towline_chain.png")
    private val TOW_BAR_CHAIN_TEXTURE = loc("textures/item/towline_bar.png")

    private fun getCenterPosition(entity: Entity, partialTick: Float): Vec3 {
        var height = entity.bbHeight / 2.0
        if (entity is CatapultShuttleEntity) height = 1.1
        val x = Mth.lerp(partialTick.toDouble(), entity.xo, entity.x)
        val y = Mth.lerp(partialTick.toDouble(), entity.yo, entity.y) + height
        val z = Mth.lerp(partialTick.toDouble(), entity.zo, entity.z)
        val localPos = Vec3(x, y, z)
        return if (ValkyrienSkiesCompat.hasMod()) ValkyrienSkiesCompat.toWorldPos(localPos, entity) else localPos
    }

    private fun renderTowChain(
        pose: Matrix4f,
        partialTick: Float,
        bufferSource: MultiBufferSource.BufferSource,
        from: Entity,
        to: Entity,
        renderType: RenderType
    ) {
        val fromPos = getCenterPosition(from, partialTick)
        val toPos = getCenterPosition(to, partialTick)
        val b1 = bufferSource.getBuffer(renderType)
        renderChain(b1, pose, fromPos, toPos, 0)
        bufferSource.endBatch()
        val b2 = bufferSource.getBuffer(renderType)
        renderChain(b2, pose, fromPos, toPos, 1)
    }

    @SubscribeEvent
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return

        val level = clientLevel ?: return
        val camera = event.camera
        val poseStack = event.poseStack
        val bufferSource = mc.renderBuffers().bufferSource()
        val partialTick = event.partialTick

        poseStack.pushPose()

        // Camera-relative transform
        val camX = camera.position.x
        val camY = camera.position.y
        val camZ = camera.position.z
        poseStack.translate(-camX, -camY, -camZ)

        val pose = poseStack.last().pose()

        val range = options.simulationDistance().get().toDouble() * 8
        val box = AABB.ofSize(camera.position, range, range, range)
        val vehicles = mutableListOf<VehicleEntity>()
        val shuttles = mutableListOf<CatapultShuttleEntity>()
        EntityFindUtil.getEntities(level).get(box) {
            if (it is VehicleEntity && it.towingUUIDs.isNotEmpty()) {
                vehicles.add(it)
            }
            if (it is CatapultShuttleEntity && it.towingUUID.isNotBlank()) {
                shuttles.add(it)
            }
        }

        // --- Vehicle towing chains ---
        val vehicleRenderType = ModRenderTypes.TOW_CHAIN.apply(CHAIN_TEXTURE)
        for (vehicle in vehicles) {
            for (towed in vehicle.towingEntities) {
                renderTowChain(pose, partialTick, bufferSource, vehicle, towed, vehicleRenderType)
            }
        }

        // --- Catapult shuttle towing chains ---
        val shuttleRenderType = ModRenderTypes.TOW_CHAIN.apply(TOW_BAR_CHAIN_TEXTURE)
        for (shuttle in shuttles) {
            val towed = shuttle.towingEntity ?: continue
            renderTowChain(pose, partialTick, bufferSource, shuttle, towed, shuttleRenderType)
        }

        poseStack.popPose()
        bufferSource.endBatch()
    }

    /**
     * Render a chain ribbon between [from] and [to].
     * @param ribbon 0 = XZ-plane ribbon, 1 = perpendicular ribbon (forming X cross-section)
     */
    private fun renderChain(
        consumer: VertexConsumer,
        pose: Matrix4f,
        from: Vec3,
        to: Vec3,
        ribbon: Int
    ) {
        val dx = (to.x - from.x).toFloat()
        val dy = (to.y - from.y).toFloat()
        val dz = (to.z - from.z).toFloat()

        val dist = Mth.sqrt(dx * dx + dy * dy + dz * dz)
        val horizontalDist = Mth.sqrt(dx * dx + dz * dz)

        // One segment per chain link, each link renders one full texture tile
        val segments = (dist / LINK_LENGTH).toInt().coerceAtLeast(1)

        // Normalized chain direction
        val invDist = if (dist > 0.001f) 1f / dist else 1f
        val dirX = dx * invDist
        val dirY = dy * invDist
        val dirZ = dz * invDist

        // First perpendicular: in XZ plane
        val perp1X: Float
        val perp1Z: Float
        if (horizontalDist > 0.001f) {
            perp1X = -dz / horizontalDist
            perp1Z = dx / horizontalDist
        } else {
            perp1X = 1f
            perp1Z = 0f
        }

        // Second perpendicular: cross(dir, perp1), 90° from perp1
        val perp2X = dirY * perp1Z
        val perp2Y = dirZ * perp1X - dirX * perp1Z
        val perp2Z = -dirY * perp1X

        for (i in 0..segments) {
            val t = i.toFloat() / segments

            val px = (from.x + dx * t).toFloat()
            val py = (from.y + dy * t).toFloat()
            val pz = (from.z + dz * t).toFloat()

            val u = i.toFloat()

            when (ribbon) {
                0 -> {
                    // Ribbon 0: width in XZ perpendicular direction
                    consumer.vertex(pose, px + perp1X * HALF_WIDTH, py, pz + perp1Z * HALF_WIDTH)
                        .color(255, 255, 255, 255)
                        .uv(u, 1.0f)
                        .endVertex()

                    consumer.vertex(pose, px - perp1X * HALF_WIDTH, py, pz - perp1Z * HALF_WIDTH)
                        .color(255, 255, 255, 255)
                        .uv(u, 0.0f)
                        .endVertex()
                }

                1 -> {
                    // Ribbon 1: width in perp2 direction (cross(dir, perp1)), forming X cross-section
                    consumer.vertex(pose, px + perp2X * HALF_WIDTH, py + perp2Y * HALF_WIDTH, pz + perp2Z * HALF_WIDTH)
                        .color(255, 255, 255, 255)
                        .uv(u, 1.0f)
                        .endVertex()

                    consumer.vertex(pose, px - perp2X * HALF_WIDTH, py - perp2Y * HALF_WIDTH, pz - perp2Z * HALF_WIDTH)
                        .color(255, 255, 255, 255)
                        .uv(u, 0.0f)
                        .endVertex()
                }
            }
        }
    }
}