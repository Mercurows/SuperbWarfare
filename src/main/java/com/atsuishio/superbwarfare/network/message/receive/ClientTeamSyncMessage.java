package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.mixin.ModTeam;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;

public record ClientTeamSyncMessage(String name, boolean flag) implements CustomPacketPayload {
    public static final Type<ClientTeamSyncMessage> TYPE = new Type<>(Mod.loc("client_team_sync"));

    public static final StreamCodec<ByteBuf, ClientTeamSyncMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ClientTeamSyncMessage::name,
            ByteBufCodecs.BOOL,
            ClientTeamSyncMessage::flag,
            ClientTeamSyncMessage::new
    );

    public static void handler(ClientTeamSyncMessage message) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam playerteam = scoreboard.getPlayerTeam(message.name());
        if (playerteam == null) return;
        ModTeam.of(playerteam).superbWarfare$setDeathMatch(message.flag());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
