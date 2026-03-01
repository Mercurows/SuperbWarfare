package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.tools.FormatTool
import net.minecraft.ChatFormatting
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

open class TranscriptItem : Item(Properties().stacksTo(1)) {
    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltip: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        tooltip.add(Component.translatable("des.superbwarfare.transcript").withStyle(ChatFormatting.GRAY))
        addScoresText(stack, tooltip)
    }

    fun addScoresText(stack: ItemStack, tooltip: MutableList<Component>) {
        val tags = stack.getOrCreateTag().getList(TAG_SCORES, Tag.TAG_COMPOUND.toInt())

        var total = 0
        for (i in tags.indices) {
            val tag = tags.getCompound(i)

            val score = tag.getInt("Score")
            total += score
            tooltip.add(
                Component.translatable("des.superbwarfare.transcript.score").withStyle(ChatFormatting.GRAY)
                    .append(
                        Component.literal("$score ")
                            .withStyle(if (score == 10) ChatFormatting.GOLD else ChatFormatting.WHITE)
                    )
                    .append(
                        Component.translatable("des.superbwarfare.transcript.distance").withStyle(ChatFormatting.GRAY)
                    )
                    .append(
                        Component.literal(FormatTool.format1D(tag.getDouble("Distance"), "m")).withStyle(
                            ChatFormatting.WHITE))
            )
        }

        tooltip.add(
            Component.translatable("des.superbwarfare.transcript.total").withStyle(ChatFormatting.YELLOW)
                .append(
                    Component.literal("$total ")
                        .withStyle(if (total == 100) ChatFormatting.GOLD else ChatFormatting.WHITE)
                )
        )
    }

    override fun use(pLevel: Level, pPlayer: Player, pUsedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (pPlayer.isCrouching) {
            val stack = pPlayer.getItemInHand(pUsedHand)
            stack.getOrCreateTag().put(TAG_SCORES, ListTag())
            return InteractionResultHolder.success(stack)
        }
        return super.use(pLevel, pPlayer, pUsedHand)
    }

    companion object {
        const val TAG_SCORES: String = "Scores"
    }
}