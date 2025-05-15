package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

public class Regeneration extends Perk {

    public Regeneration() {
        super("regeneration", Perk.Type.FUNCTIONAL);
    }

    @Override
    public void tick(GunData data, PerkInstance instance, @Nullable LivingEntity living) {
        ItemStack stack = data.stack;
        var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (cap != null) {
            cap.receiveEnergy((int) (instance.level() * cap.getMaxEnergyStored() / 2000d), false);
        }
    }
}
