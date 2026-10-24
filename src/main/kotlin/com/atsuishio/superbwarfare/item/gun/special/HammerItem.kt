package com.atsuishio.superbwarfare.item.gun.special

import com.atsuishio.superbwarfare.client.TooltipTool
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer
import com.atsuishio.superbwarfare.client.renderer.gun.HammerRenderer
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.NBTTool
import com.atsuishio.superbwarfare.tools.getOrCreateTag
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerEvent

open class HammerItem(properties: Properties) : KnifeItem(properties) {

    override fun hasCraftingRemainingItem(stack: ItemStack): Boolean {
        return true
    }

    override fun getCraftingRemainingItem(itemstack: ItemStack): ItemStack {
        val stack = itemstack.copy()

        val tag = NBTTool.getTag(stack)
        tag.putInt("CraftCount", tag.getInt("CraftCount") + 1)
        NBTTool.saveTag(stack, tag)

        if (!itemstack.isDamageableItem) return stack

        stack.damageValue = itemstack.damageValue + 1

        if (stack.damageValue >= stack.maxDamage) {
            return ItemStack.EMPTY
        }
        return stack
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        list: MutableList<Component>,
        flag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, list, flag)
        TooltipTool.addHideText(
            list,
            Component.translatable("des.superbwarfare.hammer", stack.getOrCreateTag().getInt("CraftCount"))
                .withStyle(ChatFormatting.GRAY)
        )
    }

    @OnlyIn(Dist.CLIENT)
    override fun createRenderer(): GeoGunRenderer = HammerRenderer()

    @EventBusSubscriber
    companion object {
        @SubscribeEvent
        fun onItemCraftedByHammer(event: PlayerEvent.ItemCraftedEvent) {
            val item = event.crafting
            val container = event.inventory
            val player = event.entity ?: return

            if (player.level().isClientSide) return

            if (item.`is`(ModTags.Items.HAMMER)) {
                var count = 0
                for (i in 0..<container.containerSize) {
                    if (container.getItem(i).`is`(ModTags.Items.HAMMER)) count++
                }
                if (count == 2) {
                    container.clearContent()
                }
            }
        }
    }
}
