package com.atsuishio.superbwarfare.item.blockitem

import com.atsuishio.superbwarfare.client.renderer.item.BlueprintResearchingTableBlockItemRenderer
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.world.item.BlockItem
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.function.Consumer

class BlueprintResearchTableBlockItem : BlockItem(ModBlocks.BLUEPRINT_RESEARCH_TABLE.get(), Properties()) {
    override fun initializeClient(consumer: Consumer<IClientItemExtensions>) {
        super.initializeClient(consumer)
        consumer.accept(object : IClientItemExtensions {
            private var renderer: BlockEntityWithoutLevelRenderer? = null

            override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                if (renderer == null) {
                    renderer =
                        BlueprintResearchingTableBlockItemRenderer(mc.blockEntityRenderDispatcher, mc.entityModels)
                }
                return renderer!!
            }
        })
    }
}