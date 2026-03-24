package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.entity.projectile.C4Entity
import com.atsuishio.superbwarfare.tools.FormatTool.format1DZZ
import com.atsuishio.superbwarfare.tools.TraceTool
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

open class DefuserItem : Item(Properties().durability(8)) {
    override fun use(pLevel: Level, pPlayer: Player, pUsedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = pPlayer.getItemInHand(pUsedHand)
        if (findBombInSight(pPlayer) != null) {
            pPlayer.startUsingItem(pUsedHand)
            return InteractionResultHolder.consume(stack)
        }
        return InteractionResultHolder.fail(stack)
    }

    override fun onUseTick(pLevel: Level, pLivingEntity: LivingEntity, pStack: ItemStack, pRemainingUseDuration: Int) {
        if (pLivingEntity !is Player) return
        val target = findBombInSight(pLivingEntity) ?: return

        val useTick = pStack.useDuration - pRemainingUseDuration

        if (!pLevel.isClientSide) {
            pLivingEntity.displayClientMessage(
                Component.literal(
                    format1DZZ((C4Entity.DEFAULT_DEFUSE_PROGRESS - useTick) / 20.0, "s")
                ).withStyle(ChatFormatting.GREEN), true
            )
        }

        if (useTick >= C4Entity.DEFAULT_DEFUSE_PROGRESS && pLevel is ServerLevel) {
            pLivingEntity.stopUsingItem()
            pStack.hurtAndBreak(
                1,
                pLivingEntity
            ) { it.broadcastBreakEvent(pLivingEntity.usedItemHand) }
            target.defuse()
        }
    }

    override fun getUseDuration(pStack: ItemStack): Int {
        return 72000
    }

    companion object {
        private fun findBombInSight(player: Player): C4Entity? {
            val target = TraceTool.findLookingEntity(player, 4.0)
            return target as? C4Entity
        }
    }
}
