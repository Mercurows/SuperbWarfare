package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SoundClientMessage(ResourceLocation location, double x, double y, double z, float radius, float pitch, UUID sender) {

    public static void encode(SoundClientMessage message, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(message.location);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeFloat(message.radius);
        buffer.writeFloat(message.pitch);
        buffer.writeUUID(message.sender);
    }

    public static SoundClientMessage decode(FriendlyByteBuf buffer) {
        return new SoundClientMessage(buffer.readResourceLocation(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(), buffer.readFloat(), buffer.readUUID());
    }

    public static void handler(SoundClientMessage message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleSoundClient(message, context)));
        context.get().setPacketHandled(true);
    }
}
