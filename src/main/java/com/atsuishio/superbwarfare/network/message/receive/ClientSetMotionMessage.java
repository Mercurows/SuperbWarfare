package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;

import java.util.function.Supplier;

public record ClientSetMotionMessage(Vec3 motion) {

    public static void encode(ClientSetMotionMessage message, FriendlyByteBuf buffer) {
        buffer.writeVector3f(message.motion.toVector3f());
    }

    public static ClientSetMotionMessage decode(FriendlyByteBuf buffer) {
        Vector3f v = buffer.readVector3f();
        return new ClientSetMotionMessage(new Vec3(v));
    }

    public static void handler(ClientSetMotionMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleClientSetMotion(message, ctx)));
        ctx.get().setPacketHandled(true);
    }
}
