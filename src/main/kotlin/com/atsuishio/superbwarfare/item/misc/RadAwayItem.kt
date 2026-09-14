package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.mobeffect.RadiationMobEffect
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level

@RegistryName("rad_away")
class RadAwayItem : Item(Properties().stacksTo(16)) {

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.rad_away").withStyle(ChatFormatting.GRAY))
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (RadiationMobEffect.getLevel(player) <= 0) return InteractionResultHolder.fail(stack)

        player.startUsingItem(hand)
        return InteractionResultHolder.consume(stack)
    }

    override fun finishUsingItem(stack: ItemStack, level: Level, entity: LivingEntity): ItemStack {
        if (level.isClientSide || !RadiationMobEffect.reduceLevel(entity))
            return super.finishUsingItem(stack, level, entity)

        level.playSound(
            null,
            entity.onPos,
            SoundEvents.HONEY_DRINK,
            SoundSource.PLAYERS,
            0.8f,
            1.2f
        )

        if (entity is Player) {
            entity.cooldowns.addCooldown(this, 20)
            if (!entity.isCreative) stack.shrink(1)
        } else {
            stack.shrink(1)
        }
        return super.finishUsingItem(stack, level, entity)
    }

    override fun getUseAnimation(stack: ItemStack): UseAnim = UseAnim.DRINK

    override fun getUseDuration(
        stack: ItemStack,
        entity: LivingEntity
    ): Int = 20
}
