package com.atsuishio.superbwarfare.item.blockitem

import com.atsuishio.superbwarfare.client.renderer.item.ResonanceBaseBlockItemRenderer
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.function.Consumer

@RegistryName("resonance_base")
class ResonanceBaseBlockItem : BlockItem(ModBlocks.RESONANCE_BASE.get(), Properties().rarity(Rarity.EPIC)) {
    override fun initializeClient(consumer: Consumer<IClientItemExtensions>) {
        consumer.accept(object : IClientItemExtensions {
            private val renderer by lazy {
                ResonanceBaseBlockItemRenderer(mc.blockEntityRenderDispatcher, mc.entityModels)
            }
            override fun getCustomRenderer() = renderer
        })
    }

    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        tooltip.add(Component.translatable("des.superbwarfare.resonance_base").withStyle(ChatFormatting.GRAY))
    }
}
