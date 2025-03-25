package com.atsuishio.superbwarfare.capability.laser;

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
public class LaserCapabilityProvider implements ICapabilityProvider<Player, Void, LaserCapability>, INBTSerializable<CompoundTag> {

    private final LaserCapability instance = new LaserCapability();

    @Override
    public @Nullable LaserCapability getCapability(@NotNull Player object, Void context) {
        return object.getCapability(ModCapabilities.LASER_CAPABILITY, context);
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        return instance.serializeNBT(provider);
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag nbt) {
        instance.deserializeNBT(provider, nbt);
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(ModCapabilities.LASER_CAPABILITY, EntityType.PLAYER, new LaserCapabilityProvider());
    }
}