package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ShootMessage {

    private final double spread;
    private final boolean zoom;
    private final UUID uuid;

    public ShootMessage(double spread, boolean zoom, UUID uuid) {
        this.spread = spread;
        this.zoom = zoom;
        this.uuid = uuid;
    }

    public static ShootMessage decode(FriendlyByteBuf buffer) {
        return new ShootMessage(buffer.readDouble(), buffer.readBoolean(), buffer.readUUID());
    }

    public static void encode(ShootMessage message, FriendlyByteBuf buffer) {
        buffer.writeDouble(message.spread);
        buffer.writeBoolean(message.zoom);
        buffer.writeUUID(message.uuid);
    }

    public static void handler(ShootMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                pressAction(context.getSender(), message.spread, message.zoom, message.uuid);
            }
        });
        context.setPacketHandled(true);
    }

    public static void pressAction(Player player, double spread, boolean zoom, UUID uuid) {
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);

        data.item.onShoot(data, player, spread, zoom, uuid);
    }
}
