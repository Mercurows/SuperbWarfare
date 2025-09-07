package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.SeekTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record IglaSeekMessage(int msgType) implements CustomPacketPayload {
    public static final Type<IglaSeekMessage> TYPE = new Type<>(Mod.loc("igla_seek"));

    public static final StreamCodec<ByteBuf, IglaSeekMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            IglaSeekMessage::msgType,
            IglaSeekMessage::new
    );

    public static void handler(IglaSeekMessage message, final IPayloadContext context) {
        var player = (ServerPlayer) context.player();
        if (player.getMainHandItem().getItem() != ModItems.IGLA.get()) return;

        var handItem = player.getMainHandItem();
        var data = GunData.from(handItem);
        var tag = data.tag;

        if (message.msgType == 0) {
            Entity seekingEntity = SeekTool.seekEntity(player, player.level(), 512, 20);
            Entity targetEntity = EntityFindUtil.findEntity(player.level(), tag.getString("TargetEntity"));

            if (seekingEntity != null) {
                if (data.hasEnoughAmmoToShoot(player)) {
                    tag.putString("TargetEntity", seekingEntity.getStringUUID());
                    tag.putBoolean("Seeking", true);
                    if (!tag.getBoolean("Seeking")) {
                        tag.putInt("SeekTime", 0);
                    }

                    if (seekingEntity != targetEntity) {
                        tag.putBoolean("Seeking", false);
                        tag.putInt("SeekTime", 0);
                        var clientboundstopsoundpacket = new ClientboundStopSoundPacket(Mod.loc("igla_9k38_lock"), SoundSource.PLAYERS);
                        player.connection.send(clientboundstopsoundpacket);
                    }
                }

            } else {
                tag.putString("TargetEntity", "none");
            }
        } else if (message.msgType == 1) {
            tag.putBoolean("Seeking", false);
            tag.putInt("SeekTime", 0);
            tag.putString("TargetEntity", "none");

            var clientboundstopsoundpacket = new ClientboundStopSoundPacket(Mod.loc("igla_9k38_lock"), SoundSource.PLAYERS);
            player.connection.send(clientboundstopsoundpacket);
        }

        data.save();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
