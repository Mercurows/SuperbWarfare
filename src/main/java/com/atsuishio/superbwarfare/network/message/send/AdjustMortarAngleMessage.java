package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.SoundTool;
import com.atsuishio.superbwarfare.tools.TraceTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.atsuishio.superbwarfare.entity.vehicle.MortarEntity.PITCH;

public record AdjustMortarAngleMessage(double scroll) implements CustomPacketPayload {
    public static final Type<AdjustMortarAngleMessage> TYPE = new Type<>(Mod.loc("adjust_mortar_angle"));

    public static final StreamCodec<ByteBuf, AdjustMortarAngleMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            AdjustMortarAngleMessage::scroll,
            AdjustMortarAngleMessage::new
    );

    public static void handler(AdjustMortarAngleMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        Entity looking = TraceTool.findLookingEntity(player, 6);
        if (looking == null) return;

        if (looking instanceof MortarEntity mortar) {
            mortar.getEntityData().set(PITCH, (float) Mth.clamp(mortar.getEntityData().get(PITCH) + 0.5 * message.scroll, -89, -20));
        }

        SoundTool.playLocalSound(player, ModSounds.ADJUST_FOV.get(), 1f, 0.7f);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
