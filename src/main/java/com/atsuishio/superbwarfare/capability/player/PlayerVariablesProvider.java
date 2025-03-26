package com.atsuishio.superbwarfare.capability.player;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;


public class PlayerVariablesProvider implements ICapabilityProvider<Player, Void, PlayerVariable>, INBTSerializable<CompoundTag> {
    private final PlayerVariable playerVariables = new PlayerVariable();

    @Override
    public @Nullable PlayerVariable getCapability(@NotNull Player object, Void context) {
        return playerVariables;
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        return playerVariables.writeToNBT();
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag nbt) {
        playerVariables.readFromNBT(nbt);
    }

}