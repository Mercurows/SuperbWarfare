package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.client.renderer.item.KnifeRenderer
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.tiers.ModItemTier
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.world.item.SwordItem
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.function.Consumer

@RegistryName("knife")
open class KnifeItem : SwordItem(
    ModItemTier.STEEL, 4, -1.8F, Properties().durability(1600)
) {

    override fun initializeClient(consumer: Consumer<IClientItemExtensions?>) {
        super.initializeClient(consumer)
        consumer.accept(object : IClientItemExtensions {
            private var renderer: BlockEntityWithoutLevelRenderer? = null

            override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                if (renderer == null) {
                    renderer = KnifeRenderer(mc.blockEntityRenderDispatcher, mc.entityModels)
                }
                return renderer!!
            }
        })
    }
}
