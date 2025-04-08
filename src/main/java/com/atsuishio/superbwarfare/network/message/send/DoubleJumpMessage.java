package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModCapabilities;
import com.atsuishio.superbwarfare.init.ModSounds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record DoubleJumpMessage(boolean canDoubleJump) implements CustomPacketPayload {
    public static final Type<DoubleJumpMessage> TYPE = new Type<>(Mod.loc("double_jump"));

    public static final StreamCodec<ByteBuf, DoubleJumpMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            DoubleJumpMessage::canDoubleJump,
            DoubleJumpMessage::new
    );

    public static void handler(final DoubleJumpMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        Level level = player.level();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        level.playSound(null, BlockPos.containing(x, y, z), ModSounds.DOUBLE_JUMP.get(), SoundSource.BLOCKS, 1, 1);

        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap == null) return;

        cap.playerDoubleJump = message.canDoubleJump;
        cap.syncPlayerVariables(player);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
