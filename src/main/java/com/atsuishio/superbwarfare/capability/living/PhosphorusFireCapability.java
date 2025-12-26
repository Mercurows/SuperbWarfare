package com.atsuishio.superbwarfare.capability.living;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModAttachments;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class PhosphorusFireCapability implements INBTSerializable<CompoundTag> {

    public static final ResourceLocation ID = Mod.loc("phosphorus_fire_capability");
    public static final String TAG_PHOSPHORUS_FIRE = "SbwPhosphorusFire";

    private boolean onFire = false;

    public static PhosphorusFireCapability of(LivingEntity living) {
        return living.getData(ModAttachments.PHOSPHORUS_FIRE);
    }

    public boolean isOnFire() {
        return onFire;
    }

    public void setOnFire(boolean onFire) {
        this.onFire = onFire;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        var tag = new CompoundTag();
        tag.putBoolean(TAG_PHOSPHORUS_FIRE, onFire);
        return tag;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        if (nbt.contains(TAG_PHOSPHORUS_FIRE)) {
            this.onFire = nbt.getBoolean(TAG_PHOSPHORUS_FIRE);
        }
    }
}
