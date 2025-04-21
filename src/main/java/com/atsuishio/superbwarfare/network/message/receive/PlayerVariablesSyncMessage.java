package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModAttachments;
import com.atsuishio.superbwarfare.tools.Ammo;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public record PlayerVariablesSyncMessage(int target, Map<Byte, Integer> data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlayerVariablesSyncMessage> TYPE = new CustomPacketPayload.Type<>(Mod.loc("player_variable_sync"));

    public static final StreamCodec<ByteBuf, PlayerVariablesSyncMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            PlayerVariablesSyncMessage::target,
            ByteBufCodecs.map(
                    HashMap::new,
                    ByteBufCodecs.BYTE,
                    ByteBufCodecs.VAR_INT,
                    256
            ),
            PlayerVariablesSyncMessage::data,
            PlayerVariablesSyncMessage::new
    );


    public static void handler(final PlayerVariablesSyncMessage message) {
        if (Minecraft.getInstance().player == null) return;

        var entity = Minecraft.getInstance().player.level().getEntity(message.target());
        if (entity == null) return;

        var variable = entity.getData(ModAttachments.PLAYER_VARIABLE);
        var map = message.data();

        for (var entry : map.entrySet()) {
            var type = entry.getKey();
            if (type == -1) {
                variable.tacticalSprint = entry.getValue() == 1;
            } else {
                var ammoTypes = Ammo.values();
                if (type < ammoTypes.length) {
                    var ammo = ammoTypes[type];
                    variable.ammo.put(ammo, entry.getValue());
                }
            }
        }

        entity.setData(ModAttachments.PLAYER_VARIABLE, variable);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}