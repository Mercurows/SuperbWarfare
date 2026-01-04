package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.network.ClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RadarMenuOpenMessage {

    public BlockPos pos;

    public RadarMenuOpenMessage(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(RadarMenuOpenMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
    }

    public static RadarMenuOpenMessage decode(FriendlyByteBuf buffer) {
        return new RadarMenuOpenMessage(buffer.readBlockPos());
    }

    public static void handler(RadarMenuOpenMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleRadarMenuOpen(message)));
        ctx.get().setPacketHandled(true);
    }
}
