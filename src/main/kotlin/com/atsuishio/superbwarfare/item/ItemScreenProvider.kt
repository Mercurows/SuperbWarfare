package com.atsuishio.superbwarfare.item

import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

interface ItemScreenProvider {
    @OnlyIn(Dist.CLIENT)
    fun getItemScreen(stack: ItemStack, player: Player, hand: InteractionHand): Screen?
}
