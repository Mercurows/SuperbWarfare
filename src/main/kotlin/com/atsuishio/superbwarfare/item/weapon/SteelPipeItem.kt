package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tiers.ModItemTier
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.SwordItem
import org.joml.Math

class SteelPipeItem : SwordItem(ModItemTier.STEEL, 4, -3.0f, Properties().durability(810)) {
    override fun hurtEnemy(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        attacker.level().playSound(
            null,
            target.onPos,
            ModSounds.STEEL_PIPE_HIT.get(),
            SoundSource.PLAYERS,
            1f,
            ((2 * Math.random() - 1) * 0.1f + 1.0f).toFloat()
        )

        val result = super.hurtEnemy(stack, target, attacker)
        if (stack.isEmpty) {
            attacker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack(Items.STICK, 1, stack.tag))
        }
        return result
    }
}