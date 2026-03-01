package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

class MortarDeployerItem : AbstractDeployerItem(Properties().rarity(Rarity.RARE)) {
    override fun spawnDeployedEntity(
        level: Level,
        player: Player
    ): Entity {
        return MortarEntity(level, player.yRot)
    }

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(
            Component.translatable("des.superbwarfare.mortar_deployer").withStyle(ChatFormatting.GRAY)
        )
    }
}
