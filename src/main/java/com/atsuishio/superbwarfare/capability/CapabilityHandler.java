package com.atsuishio.superbwarfare.capability;

import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber
public class CapabilityHandler {

    @SubscribeEvent
    public static void registerCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            event.addCapability(LaserCapability.ID, new LaserCapability.LaserCapabilityProvider());
            if (!(player instanceof FakePlayer)) {
                event.addCapability(PlayerVariable.ID, new PlayerVariablesProvider());
            }
        }
    }

    public static class PlayerVariablesProvider implements ICapabilitySerializable<CompoundTag> {

        private final PlayerVariable playerVariable = new PlayerVariable();
        private final LazyOptional<PlayerVariable> instance = LazyOptional.of(() -> playerVariable);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
            return cap == ModCapabilities.PLAYER_VARIABLE ? instance.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return playerVariable.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            playerVariable.deserializeNBT(nbt);
        }
    }

}
