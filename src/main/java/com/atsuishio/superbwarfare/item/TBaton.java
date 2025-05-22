package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tiers.ModItemTier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

import javax.annotation.ParametersAreNonnullByDefault;

public class TBaton extends SwordItem {
    public TBaton() {
        super(ModItemTier.STEEL, new Properties()
                .durability(1115)
                .attributes(SwordItem.createAttributes(ModItemTier.STEEL, 3, -2))
        );
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        attacker.level().playSound(null, target.getOnPos(), ModSounds.MELEE_HIT.get(), SoundSource.PLAYERS, 1, (float) ((2 * org.joml.Math.random() - 1) * 0.1f + 1));
        return super.hurtEnemy(stack, target, attacker);
    }

}
