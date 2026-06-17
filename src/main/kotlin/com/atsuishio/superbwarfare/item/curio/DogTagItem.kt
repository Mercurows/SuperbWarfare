package com.atsuishio.superbwarfare.item.curio

import com.atsuishio.superbwarfare.client.TooltipTool
import com.atsuishio.superbwarfare.client.screens.DogTagEditorScreen
import com.atsuishio.superbwarfare.client.tooltip.component.DogTagImageComponent
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.item.IVehicleInteract
import com.atsuishio.superbwarfare.item.ItemScreenProvider
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import top.theillusivec4.curios.api.CuriosApi
import top.theillusivec4.curios.api.SlotContext
import top.theillusivec4.curios.api.type.capability.ICurioItem
import java.util.*

class DogTagItem : Item(Properties().stacksTo(1)), ICurioItem, ItemScreenProvider, IVehicleInteract {
    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        TooltipTool.addScreenProviderText(pTooltipComponents)
    }

    override fun canEquip(slotContext: SlotContext, stack: ItemStack?): Boolean {
        return CuriosApi.getCuriosInventory(slotContext.entity)
            .map { it.findFirstCurio(this).isEmpty }
            .orElseGet { false }
    }

    override fun getTooltipImage(pStack: ItemStack): Optional<TooltipComponent> {
        return Optional.of(DogTagImageComponent(pStack))
    }

    override fun onInteractVehicle(
        vehicle: VehicleEntity,
        stack: ItemStack,
        player: Player,
        hand: InteractionHand
    ): InteractionResult? {
        if (!player.isShiftKeyDown) return null
        vehicle.dogTagIcon = getColors(stack).map { it.toList() }.toList()
        return InteractionResult.SUCCESS
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

            if (stack.tag == null) return colors
            val tag = stack.tag!!.getCompound("Colors")
            for (i in 0..15) {
                val color = tag.getIntArray("Color$i")
                for (j in color.indices) {
                    colors[i][j] = color[j].toShort()
                }
            }

            return colors
        }
    }
}
