package com.atsuishio.superbwarfare.perk.functional;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

public class Regeneration extends Perk {

    public Regeneration() {
        super("regeneration", Perk.Type.FUNCTIONAL);
    }

    @Override
    public void tick(GunData data, PerkInstance instance, @Nullable Entity entity) {
        ItemStack stack = data.stack;
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(
                energy -> energy.receiveEnergy((int) (instance.level() * energy.getMaxEnergyStored() / 2000d), false)
        );
    }
}
