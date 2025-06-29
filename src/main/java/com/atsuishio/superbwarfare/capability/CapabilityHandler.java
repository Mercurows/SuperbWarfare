package com.atsuishio.superbwarfare.capability;

import com.atsuishio.superbwarfare.tools.Ammo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class CapabilityHandler {

    @SubscribeEvent
    public static void registerCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            event.addCapability(LaserCapability.ID, new LaserCapability.LaserCapabilityProvider());
            if (!(player instanceof FakePlayer)) {
                event.addCapability(PlayerVariable.ID, new PlayerVariable.PlayerVariablesProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedInSyncPlayerVariables(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        var player = event.getEntity();
        player.getCapability(ModCapabilities.PLAYER_VARIABLE, null).orElse(new PlayerVariable()).sync(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawnedSyncPlayerVariables(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        var player = event.getEntity();
        player.getCapability(ModCapabilities.PLAYER_VARIABLE, null).orElse(new PlayerVariable()).sync(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        var player = event.getEntity();
        player.getCapability(ModCapabilities.PLAYER_VARIABLE, null).orElse(new PlayerVariable()).forceSync(player);
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().revive();
        PlayerVariable original = event.getOriginal().getCapability(ModCapabilities.PLAYER_VARIABLE, null).orElse(new PlayerVariable());
        PlayerVariable clone = event.getEntity().getCapability(ModCapabilities.PLAYER_VARIABLE, null).orElse(new PlayerVariable());

        for (var type : Ammo.values()) {
            type.set(clone, type.get(original));
        }

        clone.tacticalSprint = original.tacticalSprint;

        if (event.getEntity().level().isClientSide()) return;

        var player = event.getEntity();
        player.getCapability(ModCapabilities.PLAYER_VARIABLE, null).orElse(new PlayerVariable()).sync(player);
    }
}
