package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.block.VehicleAssemblingTableBlock
import com.atsuishio.superbwarfare.block.entity.VehicleAssemblingTableBlockEntity
import com.atsuishio.superbwarfare.block.property.BlockPart
import com.atsuishio.superbwarfare.client.layer.block.VehicleAssemblingTableBlockLayer
import com.atsuishio.superbwarfare.client.model.block.VehicleAssemblingTableBlockModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import software.bernie.geckolib.renderer.GeoBlockRenderer

class VehicleAssemblingTableBlockEntityRenderer :
    GeoBlockRenderer<VehicleAssemblingTableBlockEntity>(VehicleAssemblingTableBlockModel()) {
    init {
        this.addRenderLayer(VehicleAssemblingTableBlockLayer(this))
    }

    override fun getRenderType(
        animatable: VehicleAssemblingTableBlockEntity,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }

    override fun shouldRender(blockEntity: VehicleAssemblingTableBlockEntity, cameraPos: Vec3): Boolean {
        return blockEntity.blockState.getValue(VehicleAssemblingTableBlock.BLOCK_PART) == BlockPart.FLB
    }

    override fun getRenderBoundingBox(blockEntity: VehicleAssemblingTableBlockEntity): AABB {
        // 创建一个更大的边界框（示例：覆盖从方块底部到顶部上方2格的范围）
        val expansion = 2.0 // 根据模型实际大小调整

        val worldPosition = blockEntity.blockPos
        return AABB(
            (worldPosition.x - 1).toDouble(),
            worldPosition.y.toDouble(),
            (worldPosition.z - 1).toDouble(),
            (worldPosition.x + 2).toDouble(),
            worldPosition.y + expansion,
            (worldPosition.z + 2).toDouble()
        )
    }
}