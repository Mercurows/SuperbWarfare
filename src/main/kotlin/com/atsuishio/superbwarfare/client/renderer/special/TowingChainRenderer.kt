package com.atsuishio.superbwarfare.client.renderer.special

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.clientLevel
import com.atsuishio.superbwarfare.tools.mc
import com.atsuishio.superbwarfare.tools.options
import com.mojang.blaze3d.vertex.VertexConsumer
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

    private fun getCenterPosition(entity: Entity, partialTick: Float): Vec3 {
        val x = Mth.lerp(partialTick.toDouble(), entity.xo, entity.x)
        val y = Mth.lerp(partialTick.toDouble(), entity.yo, entity.y) + entity.bbHeight / 2.0
        val z = Mth.lerp(partialTick.toDouble(), entity.zo, entity.z)
        return Vec3(x, y, z)
    }

    @SubscribeEvent
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return

        val level = clientLevel ?: return
        val camera = event.camera
        val poseStack = event.poseStack
        val bufferSource = mc.renderBuffers().bufferSource()
        val partialTick = event.partialTick

        val renderType = ModRenderTypes.TOW_CHAIN.apply(CHAIN_TEXTURE)

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
        EntityFindUtil.getEntities(level).get(box) {
            if (it is VehicleEntity && it.towingUUID.isNotBlank()) {
                vehicles.add(it)
            }
        }

        // --- Pass 1: ribbon in XZ plane ---
        var builder = bufferSource.getBuffer(renderType)
        for (vehicle in vehicles) {
            val towedEntity = vehicle.towingEntity ?: continue
            val fromPos = getCenterPosition(vehicle, partialTick)
            val toPos = getCenterPosition(towedEntity, partialTick)
            renderChain(builder, pose, fromPos, toPos, 0)
            bufferSource.endBatch()

            builder = bufferSource.getBuffer(renderType)
            renderChain(builder, pose, fromPos, toPos, 1)
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