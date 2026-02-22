package com.atsuishio.superbwarfare.client.renderer.special

import com.atsuishio.superbwarfare.block.ContainerBlock
import com.atsuishio.superbwarfare.block.entity.ContainerBlockEntity
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber

@EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object ContainerBlockPreview {
    @SubscribeEvent
    fun render(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return

        val player = localPlayer ?: return
        // 仅在手持撬棍时检测
        val item = player.mainHandItem
        if (!item.`is`(ModTags.Items.TOOLS_CROWBAR)) return

        val level = player.level()
        val look = player.lookAngle

        // 查找玩家看向方块
        val distance = 32
        val start = player.position().add(0.0, player.eyeHeight.toDouble(), 0.0)
        val end = player.position().add(look.x * distance, look.y * distance + player.eyeHeight, look.z * distance)
        val context = ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)
        val result = player.level().clip(context)

        if (result.type == HitResult.Type.MISS) return

        // 获取集装箱
        val blockEntity = level.getBlockEntity(result.blockPos)
        if (blockEntity !is ContainerBlockEntity) return

        // 获取实体信息
        val entityType = blockEntity.entityType ?: return
        val entityTag = blockEntity.entityTag

        val entity: Entity? = entityType.create(level)
        if (entity != null && entityTag != null) {
            entity.load(entityTag)
        }

        var w = (entityType.dimensions.width / 2 + 1).toInt()
        var h = (entityType.dimensions.height + 1).toInt()
        if (entity != null) {
            w = (entity.type.dimensions.width / 2 + 1).toInt()
            h = (entity.type.dimensions.height + 1).toInt()
        }
        if (w == 0 || h == 0) return

        val poseStack = event.poseStack
        poseStack.pushPose()
        val pos = blockEntity.blockPos
        val view = mc.gameRenderer.mainCamera.position
        poseStack.translate(pos.x - view.x, pos.y - view.y + 1, pos.z - view.z)

        // 什么b位置
        val aabb = AABB(pos)
            .inflate(w.toDouble(), 0.0, w.toDouble())
            .expandTowards(0.0, (h - 1).toDouble(), 0.0)
            .move(0.0, -1.0, 0.0)

        val startX = aabb.minX.toFloat() - 0.001f - pos.x
        val startY = aabb.minY.toFloat() - 0.001f - pos.y
        val startZ = aabb.minZ.toFloat() - 0.001f - pos.z
        val endX = aabb.maxX.toFloat() + 0.001f - pos.x
        val endY = aabb.maxY.toFloat() + 0.001f - pos.y
        val endZ = aabb.maxZ.toFloat() + 0.001f - pos.z

        val hasEnoughSpace = ContainerBlock.canOpen(level, pos, entityType, entityTag)

        val red = if (hasEnoughSpace) 0 else 1
        val green = 1 - red
        val blue = 0.0f
        val alpha = 0.2f

        val builder = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(ModRenderTypes.BLOCK_OVERLAY)
        val m4f = poseStack.last().pose()

        // east
        builder.vertex(m4f, startX, startY, startZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, startX, endY, startZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, endX, endY, startZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, endX, startY, startZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()

        // west
        builder.vertex(m4f, startX, startY, endZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, endX, startY, endZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, endX, endY, endZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, startX, endY, endZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()

        // south
        builder.vertex(m4f, endX, startY, startZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, endX, endY, startZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, endX, endY, endZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, endX, startY, endZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()

        // north
        builder.vertex(m4f, startX, startY, startZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, startX, startY, endZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, startX, endY, endZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, startX, endY, startZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()

        // top
        builder.vertex(m4f, startX, endY, startZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, endX, endY, startZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, endX, endY, endZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, startX, endY, endZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()

        // bottom
        builder.vertex(m4f, startX, startY, startZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, endX, startY, startZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, endX, startY, endZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()
        builder.vertex(m4f, startX, startY, endZ).color(red.toFloat(), green.toFloat(), blue, alpha).endVertex()

        poseStack.popPose()
    }
}
