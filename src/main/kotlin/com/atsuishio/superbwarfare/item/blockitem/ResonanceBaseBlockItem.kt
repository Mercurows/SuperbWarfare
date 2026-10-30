package com.atsuishio.superbwarfare.item.blockitem

import com.atsuishio.superbwarfare.client.renderer.item.ResonanceBaseBlockItemRenderer
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent

@RegistryName("resonance_base")
class ResonanceBaseBlockItem : BlockItem(ModBlocks.RESONANCE_BASE.get(), Properties().rarity(Rarity.EPIC)) {

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        tooltip.add(Component.translatable("des.superbwarfare.resonance_base").withStyle(ChatFormatting.GRAY))
    }

    @EventBusSubscriber
    companion object {
        @SubscribeEvent
        fun registerRender(event: RegisterClientExtensionsEvent) {
            event.registerItem(object : IClientItemExtensions {
                private val renderer by lazy {
                    ResonanceBaseBlockItemRenderer(mc.blockEntityRenderDispatcher, mc.entityModels)
                }

                override fun getCustomRenderer() = renderer
            }, ModItems.RESONANCE_BASE)
        }
    }
}
