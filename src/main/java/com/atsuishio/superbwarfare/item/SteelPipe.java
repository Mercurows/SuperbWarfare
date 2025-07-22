package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tiers.ModItemTier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import org.jetbrains.annotations.NotNull;

public class SteelPipe extends SwordItem {

    public SteelPipe() {
        super(ModItemTier.STEEL, 4, -3.0f, new Properties().durability(810));

    }
    // TODO 音效，特殊攻击效果等

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, LivingEntity target, LivingEntity attacker) {
        attacker.level().playSound(null, target.getOnPos(), ModSounds.STEEL_PIPE_HIT.get(), SoundSource.PLAYERS, 1, (float) ((2 * org.joml.Math.random() - 1) * 0.1f + 1.0f));

        var result = super.hurtEnemy(stack, target, attacker);
//        stack.hurtAndBreak(1, attacker, (player) -> player.broadcastBreakEvent(player.getUsedItemHand()));

        if (stack.isEmpty()) {
            attacker.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK, 1, stack.getTag()));
        }
        return result;
    }
}
