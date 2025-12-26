package com.atsuishio.superbwarfare.capability.living;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.util.INBTSerializable;

@AutoRegisterCapability
public class PhosphorusFireCapability implements INBTSerializable<CompoundTag> {

    public static final ResourceLocation ID = Mod.loc("phosphorus_fire_capability");
    public static final String TAG_PHOSPHORUS_FIRE = "SbwPhosphorusFire";

    private boolean onFire = false;

    public static PhosphorusFireCapability of(LivingEntity living) {
        return living.getCapability(ModCapabilities.PHOSPHORUS_FIRE_CAPABILITY).orElseGet(PhosphorusFireCapability::new);
    }

    public boolean isOnFire() {
        return onFire;
    }

    public void setOnFire(boolean onFire) {
        this.onFire = onFire;
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.putBoolean(TAG_PHOSPHORUS_FIRE, onFire);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains(TAG_PHOSPHORUS_FIRE)) {
            this.onFire = nbt.getBoolean(TAG_PHOSPHORUS_FIRE);
        }
    }
}
