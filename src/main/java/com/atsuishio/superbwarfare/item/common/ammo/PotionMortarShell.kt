package com.atsuishio.superbwarfare.item.common.ammo

import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.util.FastColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent
import javax.annotation.ParametersAreNonnullByDefault

class PotionMortarShell : MortarShell() {
    override fun getDefaultInstance(): ItemStack {
        val stack = super.getDefaultInstance()
        stack.set(DataComponents.POTION_CONTENTS, PotionContents(Potions.POISON))
        return stack
    }

    @ParametersAreNonnullByDefault
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        stack.get(DataComponents.POTION_CONTENTS)?.addPotionTooltip(
            { e -> tooltipComponents.add(e) },
            0.125f,
            context.tickRate()
        )
    }

    @EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
    companion object {
        @SubscribeEvent
        fun onRegisterColorHandlers(event: RegisterColorHandlersEvent.Item) {
            event.register(
                { stack, layer ->
                    if (layer == 1) FastColor.ARGB32.opaque(
                        stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).color
                    ) else -1
                },
                ModItems.POTION_MORTAR_SHELL.get()
            )
        }
    }
}
