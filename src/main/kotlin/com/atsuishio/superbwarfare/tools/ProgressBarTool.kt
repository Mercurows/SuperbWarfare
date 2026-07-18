package com.atsuishio.superbwarfare.tools

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

// PJM: прогресс-бар в actionbar (установка/разборка миномёта)
object ProgressBarTool {
    private const val SEGMENTS = 20

    fun show(player: Player, key: String, progress: Float) {
        val filled = (progress.coerceIn(0f, 1f) * SEGMENTS).toInt()
        player.displayClientMessage(
            Component.translatable(key)
                .append(Component.literal(" ["))
                .append(Component.literal("|".repeat(filled)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal("|".repeat(SEGMENTS - filled)).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("]")),
            true
        )
    }
}
