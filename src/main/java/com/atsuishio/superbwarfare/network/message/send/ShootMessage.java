package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class ShootMessage {

    private final double spread;
    private final boolean zoom;
    private final @Nullable UUID uuid;
    private final @Nullable Vector3f targetPos;

    public ShootMessage(double spread, boolean zoom, @Nullable UUID uuid) {
        this.spread = spread;
        this.zoom = zoom;
        this.uuid = uuid;
        this.targetPos = null;
    }

    public ShootMessage(double spread, boolean zoom, @Nullable UUID uuid, Vector3f targetPos) {
        this.spread = spread;
        this.zoom = zoom;
        this.uuid = uuid;
        this.targetPos = targetPos;
    }

    public static ShootMessage decode(FriendlyByteBuf buffer) {
        double spread = buffer.readDouble();
        boolean zoom = buffer.readBoolean();
        UUID uuid = buffer.readOptional(FriendlyByteBuf::readUUID).orElse(null);
        boolean flag = buffer.readBoolean();
        if (flag) {
            return new ShootMessage(spread, zoom, uuid, buffer.readVector3f());
        } else {
            return new ShootMessage(spread, zoom, uuid);
        }
    }

    public static void encode(ShootMessage message, FriendlyByteBuf buffer) {
        buffer.writeDouble(message.spread);
        buffer.writeBoolean(message.zoom);
        buffer.writeOptional(Optional.ofNullable(message.uuid), FriendlyByteBuf::writeUUID);

        boolean flag = message.targetPos != null;
        buffer.writeBoolean(flag);
        if (flag) {
            buffer.writeVector3f(message.targetPos);
        }
    }

    public static void handler(ShootMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) {
                var stack = player.getMainHandItem();
                if (!(stack.getItem() instanceof GunItem)) return;

                if (message.targetPos == null) {
                    GunData.from(stack).shoot(player, message.spread, message.zoom, message.uuid);
                } else {
                    GunData.from(stack).shoot(player, message.spread, message.zoom, message.uuid, new Vec3(message.targetPos));
                }
            }
        });
        context.setPacketHandled(true);
    }
}
