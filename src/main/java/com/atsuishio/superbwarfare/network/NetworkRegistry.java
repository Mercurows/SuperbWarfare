package com.atsuishio.superbwarfare.network;

import com.atsuishio.superbwarfare.network.message.receive.*;
import com.atsuishio.superbwarfare.network.message.send.*;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkRegistry {
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(PlayerVariablesSyncMessage.TYPE, PlayerVariablesSyncMessage.STREAM_CODEC, PlayerVariablesSyncMessage::handler);
        registrar.playToClient(ShakeClientMessage.TYPE, ShakeClientMessage.STREAM_CODEC, ShakeClientMessage::handler);
        registrar.playToClient(ClientMotionSyncMessage.TYPE, ClientMotionSyncMessage.STREAM_CODEC, ClientMotionSyncMessage::handler);
        registrar.playToClient(ClientIndicatorMessage.TYPE, ClientIndicatorMessage.STREAM_CODEC, ClientIndicatorMessage::handler);
        registrar.playToClient(PlayerGunKillMessage.TYPE, PlayerGunKillMessage.STREAM_CODEC, PlayerGunKillMessage::handler);
        registrar.playToClient(GunsDataMessage.TYPE, GunsDataMessage.STREAM_CODEC, GunsDataMessage::handler);

        registrar.playToServer(LaserShootMessage.TYPE, LaserShootMessage.STREAM_CODEC, LaserShootMessage::handler);
        registrar.playToServer(BreathMessage.TYPE, BreathMessage.STREAM_CODEC, BreathMessage::handler);
        registrar.playToServer(ShootMessage.TYPE, ShootMessage.STREAM_CODEC, ShootMessage::handler);
        registrar.playToServer(DoubleJumpMessage.TYPE, DoubleJumpMessage.STREAM_CODEC, DoubleJumpMessage::handler);
        registrar.playToServer(VehicleMovementMessage.TYPE, VehicleMovementMessage.STREAM_CODEC, VehicleMovementMessage::handler);
    }
}
