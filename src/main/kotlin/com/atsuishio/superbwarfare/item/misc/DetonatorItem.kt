package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.entity.projectile.C4Entity
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

open class DetonatorItem : Item(Properties().stacksTo(1)) {
    override fun use(world: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        player.cooldowns.addCooldown(stack.item, 10)

        if (player is ServerPlayer) {
            player.level()
                .playSound(null, player.onPos, ModSounds.C4_DETONATOR_CLICK.get(), SoundSource.PLAYERS, 1f, 1f)
        }

        this.releaseUsing(stack, player.level(), player, 1)

        val entities = getC4(player, player.level())
        for (e in entities) {
            if (e is C4Entity && e.getEntityData().get(C4Entity.IS_CONTROLLABLE)) {
                e.explode()
            }
        }

        return InteractionResultHolder.consume(stack)
    }

    companion object {
        fun getC4(player: Player?, level: Level): MutableList<Entity> {
            return EntityFindUtil.getEntities(level).all.asSequence().filter { it is C4Entity && it.owner == player }.toMutableList()
        }
    }
}
