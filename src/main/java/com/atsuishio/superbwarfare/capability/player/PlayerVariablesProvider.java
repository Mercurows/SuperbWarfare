package com.atsuishio.superbwarfare.capability.player;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

@EventBusSubscriber(modid = ModUtils.MODID, bus = EventBusSubscriber.Bus.MOD)
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

    @SubscribeEvent
    public static void init(RegisterCapabilitiesEvent event) {
        event.registerEntity(ModCapabilities.PLAYER_VARIABLE, EntityType.PLAYER, new PlayerVariablesProvider());
    }

}