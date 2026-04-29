package com.atsuishio.superbwarfare.item.curio

import com.atsuishio.superbwarfare.client.TooltipTool
import com.atsuishio.superbwarfare.client.screens.DogTagEditorScreen
import com.atsuishio.superbwarfare.client.tooltip.component.DogTagImageComponent
import com.atsuishio.superbwarfare.init.ModDataComponents
import com.atsuishio.superbwarfare.item.ItemScreenProvider
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import top.theillusivec4.curios.api.CuriosApi
import top.theillusivec4.curios.api.SlotContext
import top.theillusivec4.curios.api.type.capability.ICurioItem
import java.util.*
import javax.annotation.ParametersAreNonnullByDefault

class DogTagItem : Item(Properties().stacksTo(1)), ICurioItem, ItemScreenProvider {
    @ParametersAreNonnullByDefault
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        TooltipTool.addScreenProviderText(tooltipComponents)
    }

    override fun canEquip(slotContext: SlotContext, stack: ItemStack?): Boolean {
        return CuriosApi.getCuriosInventory(slotContext.entity())
            .flatMap { c -> c.findFirstCurio(this) }
            .isEmpty
    }

    override fun getTooltipImage(pStack: ItemStack): Optional<TooltipComponent> {
        return Optional.of(DogTagImageComponent(pStack))
    }

    @OnlyIn(Dist.CLIENT)
    override fun getItemScreen(stack: ItemStack, player: Player, hand: InteractionHand): Screen {
        return DogTagEditorScreen(stack, hand)
    }

    companion object {
        @JvmStatic
        fun getColors(stack: ItemStack): Array<ShortArray> {
            val colors: Array<ShortArray> = Array(16) { ShortArray(16) }
            for (el in colors) {
                Arrays.fill(el, (-1).toShort())
            }

            val data = stack.get(ModDataComponents.DOG_TAG_IMAGE).takeIf { !it.isNullOrEmpty() } ?: return colors

            for (i in data.indices union colors.indices) {
                val color = data[i].takeIf { !it.isNullOrEmpty() } ?: continue
                for (j in color.indices union colors[i].indices) {
                    colors[i][j] = color[j].toShort()
                }
            }

            return colors
        }
    }
}
