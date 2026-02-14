package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.CustomDamageProperty
import com.atsuishio.superbwarfare.tiers.ModItemTier
import net.minecraft.core.Holder
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.SwordItem
import org.joml.Math

class SteelPipeItem : SwordItem(
    ModItemTier.STEEL, CustomDamageProperty(810).attributes(createAttributes(ModItemTier.STEEL, 4, -3f))
) {
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
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND)

        if (stack.isEmpty) {
            attacker.setItemSlot(
                EquipmentSlot.MAINHAND,
                ItemStack(Holder.direct(Items.STICK), 1, stack.componentsPatch)
            )
        }
        return result
    }
}