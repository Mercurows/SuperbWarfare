package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.data.gun.Ammo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

public class PlayerVariablesSyncMessage {
    private final int target;
    private final Map<Byte, Integer> data;

    public PlayerVariablesSyncMessage(FriendlyByteBuf buffer) {
        this.target = buffer.readVarInt();
        this.data = buffer.readMap(FriendlyByteBuf::readByte, FriendlyByteBuf::readVarInt);
    }

    public PlayerVariablesSyncMessage(int entityId, Map<Byte, Integer> data) {
        this.data = data;
        this.target = entityId;
    }

    public static void buffer(PlayerVariablesSyncMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.target);
        buffer.writeMap(message.data, (buf, key) -> buf.writeByte(key), FriendlyByteBuf::writeVarInt);
    }

    public static void handler(PlayerVariablesSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            context.setPacketHandled(true);
            if (context.getDirection().getReceptionSide().isServer() || Minecraft.getInstance().player == null) {
                return;
            }

            var entity = Minecraft.getInstance().player.level().getEntity(message.target);
            if (entity == null) return;

            PlayerVariable variables = entity.getCapability(ModCapabilities.PLAYER_VARIABLE, null).orElse(new PlayerVariable());

            for (var entry : message.data.entrySet()) {
                var type = entry.getKey();
                if (type == -1) {
                    variables.tacticalSprint = entry.getValue() == 1;
                } else {
                    var types = Ammo.values();
                    if (type < types.length) {
                        types[type].set(variables, entry.getValue());
                    }
                }
            }
        });
    }
}
