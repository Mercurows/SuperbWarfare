package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.FiringParameters;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record FiringParametersEditMessage(
        int x, int y, int z,
        int radius, boolean isDepressed, boolean mainHand
) implements CustomPacketPayload {
    public static final Type<FiringParametersEditMessage> TYPE = new Type<>(Mod.loc("firing_parameters_edit"));

    public static final StreamCodec<ByteBuf, FiringParametersEditMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, FiringParametersEditMessage::x,
            ByteBufCodecs.INT, FiringParametersEditMessage::y,
            ByteBufCodecs.INT, FiringParametersEditMessage::z,
            ByteBufCodecs.INT, FiringParametersEditMessage::radius,
            ByteBufCodecs.BOOL, FiringParametersEditMessage::isDepressed,
            ByteBufCodecs.BOOL, FiringParametersEditMessage::mainHand,
            FiringParametersEditMessage::new
    );

    public static void handler(FiringParametersEditMessage message, final IPayloadContext context) {
        var player = context.player();

        ItemStack stack = message.mainHand ? player.getMainHandItem() : player.getOffhandItem();
        if (!stack.is(ModItems.FIRING_PARAMETERS.get()) && !stack.is(ModItems.ARTILLERY_INDICATOR.get())) return;

        var parameters = new FiringParameters.Parameters(new BlockPos(message.x, message.y, message.z), message.radius, message.isDepressed);
        stack.set(ModDataComponents.FIRING_PARAMETERS, parameters);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
