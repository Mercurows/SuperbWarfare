package com.atsuishio.superbwarfare.item.gun.sniper

import com.atsuishio.superbwarfare.client.TooltipTool
import com.atsuishio.superbwarfare.init.ModRarities
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItemV2
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

@RegistryName("ql_1031")
object Ql1031Item : GeoGunItemV2(Properties().rarity(ModRarities.VIRTUAL)) {
    override fun appendHoverText(stack: ItemStack, level: Level?, list: MutableList<Component>, flag: TooltipFlag) {
        list.add(Component.empty())
        list.add(
            Component.translatable("des.superbwarfare.ql_1031_1").withStyle(ChatFormatting.GRAY)
                .withStyle(ChatFormatting.ITALIC)
        )

        TooltipTool.addHideText(list, Component.empty())
        TooltipTool.addHideText(
            list,
            Component.translatable("des.superbwarfare.trachelium_3").withStyle(ChatFormatting.WHITE)
        )
        TooltipTool.addHideText(
            list,
            Component.translatable("des.superbwarfare.ql_1031_2").withStyle(Style.EMPTY.withColor(0xFFECE7))
        )
    }
}