package com.atsuishio.superbwarfare.capability;

import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.data.gun.Ammo;
import com.atsuishio.superbwarfare.network.message.receive.PlayerVariablesSyncMessage;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber
public class CapabilityHandler {

    @SubscribeEvent
    public static void registerCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            event.addCapability(LaserCapability.ID, createProvider(LazyOptional.of(LaserCapability.LaserCapabilityImpl::new), ModCapabilities.LASER_CAPABILITY));
            if (!(player instanceof FakePlayer)) {
                event.addCapability(PlayerVariable.ID, createProvider(LazyOptional.of(PlayerVariable::new), ModCapabilities.PLAYER_VARIABLE));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        com.atsuishio.superbwarfare.Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
                new PlayerVariablesSyncMessage(player.getId(), PlayerVariable.getOrDefault(player).compareAndUpdate()));
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        com.atsuishio.superbwarfare.Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
                new PlayerVariablesSyncMessage(player.getId(), PlayerVariable.getOrDefault(player).compareAndUpdate()));
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        com.atsuishio.superbwarfare.Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
                new PlayerVariablesSyncMessage(player.getId(), PlayerVariable.getOrDefault(player).forceUpdate()));
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().revive();
        if (event.getEntity().level().isClientSide()) return;
        var original = PlayerVariable.getOrDefault(event.getOriginal());
        var clone = event.getEntity().getCapability(ModCapabilities.PLAYER_VARIABLE, null).orElse(new PlayerVariable());

        for (var type : Ammo.values()) {
            type.set(clone, type.get(original));
        }

        clone.tacticalSprint = original.tacticalSprint;

        if (event.getEntity().level().isClientSide()) return;

        var player = event.getEntity();
        player.getCapability(ModCapabilities.PLAYER_VARIABLE, null).orElse(new PlayerVariable()).sync(player);
    }

    public static <T extends INBTSerializable<CompoundTag>> ICapabilitySerializable<CompoundTag> createProvider(LazyOptional<T> instance, Capability<T> capability) {
        return new ICapabilitySerializable<>() {
            @Override
            public @NotNull <C> LazyOptional<C> getCapability(@NotNull Capability<C> cap, @Nullable Direction side) {
                return capability.orEmpty(cap, instance.cast());
            }

            @Override
            public CompoundTag serializeNBT() {
                return instance.orElseThrow(NullPointerException::new).serializeNBT();
            }

            @Override
            public void deserializeNBT(CompoundTag nbt) {
                instance.orElseThrow(NullPointerException::new).deserializeNBT(nbt);
            }
        };
    }
}
