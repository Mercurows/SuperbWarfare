package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.block.BlueprintResearchTableBlock
import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity
import com.atsuishio.superbwarfare.client.layer.block.BlueprintResearchTableBlockLayer
import com.atsuishio.superbwarfare.client.model.block.BlueprintResearchTableBlockModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.properties.BedPart
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import software.bernie.geckolib.renderer.GeoBlockRenderer

class BlueprintResearchTableBlockEntityRenderer : GeoBlockRenderer<BlueprintResearchTableBlockEntity>(
    BlueprintResearchTableBlockModel()
) {
    init {
        this.addRenderLayer(BlueprintResearchTableBlockLayer(this))
    }

    override fun getRenderType(
        animatable: BlueprintResearchTableBlockEntity,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType? = RenderType.entityTranslucent(getTextureLocation(animatable))

    override fun shouldRender(
        pBlockEntity: BlueprintResearchTableBlockEntity,
        pCameraPos: Vec3
    ): Boolean {
        return pBlockEntity.blockState.getValue(BlueprintResearchTableBlock.PART) == BedPart.FOOT
    }

    override fun getRenderBoundingBox(blockEntity: BlueprintResearchTableBlockEntity): AABB {
        val worldPosition = blockEntity.blockPos

        // 创建一个更大的边界框（示例：覆盖从方块底部到顶部上方2格的范围）
        val expansion = 2.0 // 根据模型实际大小调整
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