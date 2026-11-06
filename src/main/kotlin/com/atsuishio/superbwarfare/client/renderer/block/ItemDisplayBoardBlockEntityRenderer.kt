package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.block.ItemDisplayBoardBlock
import com.atsuishio.superbwarfare.block.entity.ItemDisplayBoardBlockEntity
import com.atsuishio.superbwarfare.client.renderer.gun.StaticGunRenderer
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.util.*

class ItemDisplayBoardBlockEntityRenderer : BlockEntityRenderer<ItemDisplayBoardBlockEntity> {

    private class Visibility(val camera: Vec3, val tick: Long, val visible: Boolean)
    private val visibility = WeakHashMap<ItemDisplayBoardBlockEntity, Visibility>()

    override fun shouldRender(blockEntity: ItemDisplayBoardBlockEntity, cameraPos: Vec3): Boolean {
        if (!blockEntity.hasItem) return false
        if (!super.shouldRender(blockEntity, cameraPos)) return false

        val box = blockEntity.renderBoundingBox
        val frustum = Minecraft.getInstance().levelRenderer.frustum ?: return true
        if (!frustum.isVisible(box)) return false

        val tick = blockEntity.level?.gameTime ?: return true
        val cached = visibility[blockEntity]
        if (cached != null && tick >= cached.tick && tick - cached.tick < 2 &&
            cameraPos.distanceToSqr(cached.camera) < 0.01) return cached.visible

        // Visibility/frustum checks still run every frame. Only the expensive (up to nine) block
        // raycasts are reused for <= 100 ms while the camera is nearly stationary.
        val visible = hasAnyVisibleSample(blockEntity, box, cameraPos)
        visibility[blockEntity] = Visibility(cameraPos, tick, visible)
        return visible
    }

    private fun hasAnyVisibleSample(
        blockEntity: ItemDisplayBoardBlockEntity,
        box: AABB,
        cameraPos: Vec3
    ): Boolean {
        val level = blockEntity.level ?: return true
        val centerX = (box.minX + box.maxX) * 0.5
        val centerY = (box.minY + box.maxY) * 0.5
        val centerZ = (box.minZ + box.maxZ) * 0.5
        val offsets = SAMPLE_OFFSETS

        var i = 0
        while (i < offsets.size) {
            val from = Vec3(centerX + offsets[i], centerY + offsets[i + 1], centerZ + offsets[i + 2])
            if (isVisibleFrom(level, blockEntity.blockPos, from, cameraPos)) {
                return true
            }
            i += 3
        }
        return false
    }

    private fun isVisibleFrom(level: Level, boardPos: BlockPos, from: Vec3, cameraPos: Vec3): Boolean {
        val hit = level.clip(
            ClipContext(from, cameraPos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, null)
        )
        // 没有命中方块，或者命中的是展示板自己（展示板没有碰撞箱，正常情况下不会命中） -> 这个采样点可见
        return hit.type == HitResult.Type.MISS || hit.blockPos == boardPos
    }

    override fun render(
        blockEntity: ItemDisplayBoardBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val itemCount = blockEntity.itemCount
        if (itemCount == 0) return

        val state = blockEntity.blockState
        val facing = state.getValue(ItemDisplayBoardBlock.FACING)
        // 放两个物品时角度固定为 0
        val rotation = if (itemCount >= 2) 0 else state.getValue(ItemDisplayBoardBlock.ROTATION)

        poseStack.pushPose()
        // 板体固定在朝向相反一侧（贴墙/贴地），物品位于板面前方 1px
        poseStack.translate(
            0.5 - facing.stepX * 0.375,
            0.5 - facing.stepY * 0.375,
            0.5 - facing.stepZ * 0.375
        )
        when (facing) {
            Direction.UP -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(-90f))
                poseStack.mulPose(Axis.YP.rotationDegrees(180f))
            }

            Direction.DOWN -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90f))
                poseStack.mulPose(Axis.YP.rotationDegrees(180f))
            }

            else -> poseStack.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()))
        }

        for (slot in 0..1) {
            val stack = blockEntity.getItem(slot)
            if (stack.isEmpty) continue

            // 两个物品时：先放入的在 3/4 处（上），后放入的在 1/4 处（下）；只有一个物品时居中
            val offsetY = if (itemCount >= 2) {
                if (slot == 0) 0.25 else -0.25
            } else {
                0.0
            }

            poseStack.pushPose()
            poseStack.translate(0.0, offsetY, 0.0)
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotation * 11.25f))
            poseStack.scale(0.5f, 0.5f, 0.5f)

            StaticGunRenderer.renderItem {
                Minecraft.getInstance().itemRenderer.renderStatic(
                    stack,
                    ItemDisplayContext.FIXED,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    buffer,
                    blockEntity.level,
                    0
                )
            }

            poseStack.popPose()
        }

        poseStack.popPose()
    }

    companion object {
        private val SAMPLE_OFFSETS: DoubleArray = doubleArrayOf(
            0.0, 0.0, 0.0,
            -1.0, -1.0, -1.0,
            1.0, -1.0, -1.0,
            -1.0, 1.0, -1.0,
            1.0, 1.0, -1.0,
            -1.0, -1.0, 1.0,
            1.0, -1.0, 1.0,
            -1.0, 1.0, 1.0,
            1.0, 1.0, 1.0
        )
    }
}
