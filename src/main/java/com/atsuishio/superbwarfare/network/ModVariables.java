package com.atsuishio.superbwarfare.network;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.tools.Ammo;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public class ModVariables {

    @SubscribeEvent
    public static void init(RegisterCapabilitiesEvent event) {
        event.register(PlayerVariable.class);
    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber
    public static class EventBusVariableHandlers {
        @SubscribeEvent
        public static void onPlayerLoggedInSyncPlayerVariables(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity().level().isClientSide()) return;

            var player = event.getEntity();
            player.getCapability(PLAYER_VARIABLE, null).orElse(new PlayerVariable()).sync(player);
        }

        @SubscribeEvent
        public static void onPlayerRespawnedSyncPlayerVariables(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity().level().isClientSide()) return;

            var player = event.getEntity();
            player.getCapability(PLAYER_VARIABLE, null).orElse(new PlayerVariable()).sync(player);
        }

        @SubscribeEvent
        public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity().level().isClientSide()) return;

            var player = event.getEntity();
            player.getCapability(PLAYER_VARIABLE, null).orElse(new PlayerVariable()).sync(player);
        }

        @SubscribeEvent
        public static void clonePlayer(PlayerEvent.Clone event) {
            event.getOriginal().revive();
            PlayerVariable original = event.getOriginal().getCapability(PLAYER_VARIABLE, null).orElse(new PlayerVariable());
            PlayerVariable clone = event.getEntity().getCapability(PLAYER_VARIABLE, null).orElse(new PlayerVariable());

            for (var type : Ammo.values()) {
                type.set(clone, type.get(original));
            }

            clone.tacticalSprint = original.tacticalSprint;

            if (event.getEntity().level().isClientSide()) return;

            var player = event.getEntity();
            player.getCapability(PLAYER_VARIABLE, null).orElse(new PlayerVariable()).sync(player);
        }
    }

    public static final Capability<PlayerVariable> PLAYER_VARIABLE = CapabilityManager.get(new CapabilityToken<>() {
    });

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber
    private static class PlayerVariablesProvider implements ICapabilitySerializable<Tag> {
        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player && !(event.getObject() instanceof FakePlayer))
                event.addCapability(Mod.loc("player_variables"), new PlayerVariablesProvider());
        }

        private final PlayerVariable playerVariable = new PlayerVariable();
        private final LazyOptional<PlayerVariable> instance = LazyOptional.of(() -> playerVariable);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
            return cap == PLAYER_VARIABLE ? instance.cast() : LazyOptional.empty();
        }

        @Override
        public Tag serializeNBT() {
            return playerVariable.writeNBT();
        }

        @Override
        public void deserializeNBT(Tag nbt) {
            playerVariable.readNBT(nbt);
        }
    }
}
