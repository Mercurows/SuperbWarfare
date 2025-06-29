package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TacticalSprintMessage(boolean sprint) {

    public static void encode(TacticalSprintMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.sprint);
    }

    public static TacticalSprintMessage decode(FriendlyByteBuf buffer) {
        return new TacticalSprintMessage(buffer.readBoolean());
    }

    public static void handler(TacticalSprintMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            PlayerVariable.modify(player, capability -> capability.tacticalSprint = MiscConfig.ALLOW_TACTICAL_SPRINT.get() && message.sprint);
        });
        context.setPacketHandled(true);
    }
}
