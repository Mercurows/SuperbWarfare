package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tiers.ModItemTier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import org.jetbrains.annotations.NotNull;

public class SteelPipe extends SwordItem {
    public SteelPipe() {
        super(ModItemTier.STEEL, new Item.Properties()
                .durability(1919)
                .attributes(SwordItem.createAttributes(ModItemTier.STEEL, 6, -2.5f))
        );
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        pAttacker.level().playSound(null, pTarget.getOnPos(), ModSounds.STEEL_PIPE_HIT.get(), SoundSource.PLAYERS, 1, (float) ((2 * org.joml.Math.random() - 1) * 0.1f + 1.0f));
        return super.hurtEnemy(pStack, pTarget, pAttacker);
    }
}
