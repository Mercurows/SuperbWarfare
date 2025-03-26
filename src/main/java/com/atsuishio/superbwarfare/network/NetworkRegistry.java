package com.atsuishio.superbwarfare.network;

import com.atsuishio.superbwarfare.network.message.*;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkRegistry {
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(PlayerVariablesSyncMessage.TYPE, PlayerVariablesSyncMessage.STREAM_CODEC, PlayerVariablesSyncMessage::handler);
        registrar.playToClient(ShakeClientMessage.TYPE, ShakeClientMessage.STREAM_CODEC, ShakeClientMessage::handler);
        registrar.playToClient(ClientMotionSyncMessage.TYPE, ClientMotionSyncMessage.STREAM_CODEC, ClientMotionSyncMessage::handler);
        registrar.playToServer(LaserShootMessage.TYPE, LaserShootMessage.STREAM_CODEC, LaserShootMessage::handler);
        registrar.playToServer(BreathMessage.TYPE, BreathMessage.STREAM_CODEC, BreathMessage::handler);

    }
}
