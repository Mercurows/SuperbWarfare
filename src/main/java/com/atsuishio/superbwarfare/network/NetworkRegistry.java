package com.atsuishio.superbwarfare.network;

import com.atsuishio.superbwarfare.network.message.PlayerVariablesSyncMessage;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkRegistry {
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                PlayerVariablesSyncMessage.TYPE,
                PlayerVariablesSyncMessage.STREAM_CODEC,
                PlayerVariablesSyncMessage::handler
        );
    }
}
