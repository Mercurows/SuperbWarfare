package com.atsuishio.superbwarfare.capability.laser;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public class LaserCapabilityProvider implements ICapabilityProvider<Player, Void, LaserCapability>, INBTSerializable<CompoundTag> {

    private final LaserCapability instance = new LaserCapability();

    @Override
    public @Nullable LaserCapability getCapability(@NotNull Player object, Void context) {
        return instance;
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        return instance.serializeNBT(provider);
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag nbt) {
        instance.deserializeNBT(provider, nbt);
    }
}