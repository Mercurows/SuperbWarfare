package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class ShootMessage {

    private final double spread;
    private final boolean zoom;
    private final @Nullable UUID uuid;

    public ShootMessage(double spread, boolean zoom, @Nullable UUID uuid) {
        this.spread = spread;
        this.zoom = zoom;
        this.uuid = uuid;
    }

    public static ShootMessage decode(FriendlyByteBuf buffer) {
        return new ShootMessage(buffer.readDouble(), buffer.readBoolean(), buffer.readOptional(FriendlyByteBuf::readUUID).orElse(null));
    }

    public static void encode(ShootMessage message, FriendlyByteBuf buffer) {
        buffer.writeDouble(message.spread);
        buffer.writeBoolean(message.zoom);
        buffer.writeOptional(Optional.ofNullable(message.uuid), FriendlyByteBuf::writeUUID);
    }

    public static void handler(ShootMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) {
                var stack = player.getMainHandItem();
                if (!(stack.getItem() instanceof GunItem)) return;

                GunData.from(stack).shoot(player, message.spread, message.zoom, message.uuid);
            }
        });
        context.setPacketHandled(true);
    }
}
