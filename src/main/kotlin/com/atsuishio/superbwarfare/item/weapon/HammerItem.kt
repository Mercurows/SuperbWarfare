package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.client.TooltipTool
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.Tier
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Math

open class HammerItem(tier: Tier, attackDamage: Int, attackSpeed: Float, properties: Properties) :
    SwordItem(tier, attackDamage, attackSpeed, properties) {
    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        TooltipTool.addHideText(
            pTooltipComponents,
            Component.translatable("des.superbwarfare.hammer", pStack.getOrCreateTag().getInt("CraftCount"))
                .withStyle(ChatFormatting.GRAY)
        )
    }

    override fun hasCraftingRemainingItem(stack: ItemStack?): Boolean {
        return true
    }

    override fun getCraftingRemainingItem(itemstack: ItemStack): ItemStack {
        val stack = itemstack.copy()
        stack.hurt(1, RandomSource.create(), null)
        stack.getOrCreateTag().putInt("CraftCount", stack.getOrCreateTag().getInt("CraftCount") + 1)
        if (stack.isEmpty || stack.damageValue >= stack.maxDamage) {
            return ItemStack.EMPTY
        }
        return stack
    }

    override fun isRepairable(itemstack: ItemStack): Boolean {
        return true
    }

    override fun hurtEnemy(pStack: ItemStack, pTarget: LivingEntity, pAttacker: LivingEntity): Boolean {
        pAttacker.level().playSound(
            null,
            pTarget.onPos,
            ModSounds.MELEE_HIT.get(),
            SoundSource.PLAYERS,
            1f,
            ((2 * Math.random() - 1) * 0.1f + 1.0f).toFloat()
        )
        return super.hurtEnemy(pStack, pTarget, pAttacker)
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
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