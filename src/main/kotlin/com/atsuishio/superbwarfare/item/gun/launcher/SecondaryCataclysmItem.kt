package com.atsuishio.superbwarfare.item.gun.launcher

import com.atsuishio.superbwarfare.client.TooltipTool.addHideText
import com.atsuishio.superbwarfare.init.ModRarities
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItemV2
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import javax.annotation.ParametersAreNonnullByDefault

@RegistryName("secondary_cataclysm")
object SecondaryCataclysmItem : GeoGunItemV2(Properties().rarity(ModRarities.VIRTUAL)) {

    @ParametersAreNonnullByDefault
    override fun appendHoverText(stack: ItemStack, level: Level?, list: MutableList<Component>, flag: TooltipFlag) {
        list.add(Component.empty())
        list.add(
            Component.translatable("des.superbwarfare.secondary_cataclysm_1").withStyle(ChatFormatting.GRAY)
                .withStyle(ChatFormatting.ITALIC)
        )

        addHideText(list, Component.empty())
        addHideText(list, Component.translatable("des.superbwarfare.trachelium_3").withStyle(ChatFormatting.WHITE))
        addHideText(
            list,
            Component.translatable("des.superbwarfare.secondary_cataclysm_2").withStyle(Style.EMPTY.withColor(0x68B9F6))
        )
    }
}
