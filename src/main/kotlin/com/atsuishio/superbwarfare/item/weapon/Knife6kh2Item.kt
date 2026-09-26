package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.client.renderer.item.Knife6kh2Renderer
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.function.Consumer

@RegistryName("knife_6kh2")
class Knife6kh2Item : KnifeItem() {

    override fun initializeClient(consumer: Consumer<IClientItemExtensions?>) {
        consumer.accept(object : IClientItemExtensions {
            private var renderer: BlockEntityWithoutLevelRenderer? = null

            override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                if (renderer == null) {
                    renderer = Knife6kh2Renderer(mc.blockEntityRenderDispatcher, mc.entityModels)
                }
                return renderer!!
            }
        })
    }
}
