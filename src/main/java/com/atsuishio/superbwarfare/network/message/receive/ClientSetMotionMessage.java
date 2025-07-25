package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientSetMotionMessage(int id, Vec3 motion) {

    public static void encode(ClientSetMotionMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.id);
        buffer.writeDouble(message.motion.x);
        buffer.writeDouble(message.motion.y);
        buffer.writeDouble(message.motion.z);
    }

    public static ClientSetMotionMessage decode(FriendlyByteBuf buffer) {
        int id = buffer.readVarInt();
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        return new ClientSetMotionMessage(id, new Vec3(x, y, z));
    }

    public static void handler(ClientSetMotionMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleClientSetMotion(message, ctx)));
        ctx.get().setPacketHandled(true);
    }
}
