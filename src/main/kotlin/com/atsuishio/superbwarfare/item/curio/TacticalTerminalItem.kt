package com.atsuishio.superbwarfare.item.curio

import com.atsuishio.superbwarfare.client.TooltipTool
import com.atsuishio.superbwarfare.config.server.MapConfig
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.network.message.receive.OpenTacticalMapScreenMessage
import com.atsuishio.superbwarfare.tools.sendPacket
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import top.theillusivec4.curios.api.CuriosApi
import top.theillusivec4.curios.api.SlotContext
import top.theillusivec4.curios.api.type.capability.ICurioItem

@RegistryName("tactical_terminal")
open class TacticalTerminalItem : Item(Properties().stacksTo(1).rarity(Rarity.UNCOMMON)), ICurioItem {
    override fun canEquip(slotContext: SlotContext, stack: ItemStack?): Boolean {
        return CuriosApi.getCuriosInventory(slotContext.entity)
            .map { it.findFirstCurio(this).isEmpty }
            .orElseGet { false }
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltip: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        TooltipTool.addDevelopingText(tooltip)
        if (!MapConfig.ENABLE_TACTICAL_MAP.get()) {
            tooltip.add(
                Component.translatable("des.superbwarfare.tactical_terminal.disabled").withStyle(ChatFormatting.RED)
            )
        }
        tooltip.add(
            Component.translatable(
                "des.superbwarfare.tactical_terminal",
                Component.literal("[${ModKeyMappings.TOGGLE_TACTICAL_MAP.key.displayName.string}]")
                    .withStyle(ChatFormatting.AQUA)
            ).withStyle(ChatFormatting.GRAY)
        )
    }

    override fun use(
        pLevel: Level,
        pPlayer: Player,
        pUsedHand: InteractionHand
    ): InteractionResultHolder<ItemStack> {
        val stack = pPlayer.getItemInHand(pUsedHand)
        if (!MapConfig.ENABLE_TACTICAL_MAP.get()) {
            return InteractionResultHolder.fail(stack)
        }

        val level = pPlayer.level()
        if (!level.isClientSide) {
            pPlayer.sendPacket(OpenTacticalMapScreenMessage)
        }
        return InteractionResultHolder.consume(stack)
    }

    companion object {
        @JvmStatic
        fun isTerminalEquipped(entity: LivingEntity?): Boolean {
            return CuriosApi.getCuriosInventory(entity)
                .map { !it.findFirstCurio(ModItems.TACTICAL_TERMINAL.get()).isEmpty }
                .orElseGet { false }
        }
    }
}
