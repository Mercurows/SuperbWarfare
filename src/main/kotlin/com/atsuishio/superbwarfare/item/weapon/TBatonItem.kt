package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tiers.ModItemTier
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SwordItem
import org.joml.Math

class TBatonItem : SwordItem(ModItemTier.STEEL, 3, -2f, Properties().durability(1115)) {
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
}