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

public class SeekingWeaponShootMessage {

    private final double spread;
    private final boolean zoom;
    private final @Nullable UUID uuid;

    private final Vector3f targetPos;

    public SeekingWeaponShootMessage(double spread, boolean zoom, @Nullable UUID uuid, Vector3f targetPos) {
        this.spread = spread;
        this.zoom = zoom;
        this.uuid = uuid;
        this.targetPos = targetPos;
    }

    public static SeekingWeaponShootMessage decode(FriendlyByteBuf buffer) {
        return new SeekingWeaponShootMessage(buffer.readDouble(), buffer.readBoolean(), buffer.readOptional(FriendlyByteBuf::readUUID).orElse(null), buffer.readVector3f());
    }

    public static void encode(SeekingWeaponShootMessage message, FriendlyByteBuf buffer) {
        buffer.writeDouble(message.spread);
        buffer.writeBoolean(message.zoom);
        buffer.writeOptional(Optional.ofNullable(message.uuid), FriendlyByteBuf::writeUUID);
        buffer.writeVector3f(message.targetPos);
    }

    public static void handler(SeekingWeaponShootMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) {
                var stack = player.getMainHandItem();
                if (!(stack.getItem() instanceof GunItem)) return;

                GunData.from(stack).shoot(player, message.spread, message.zoom, message.uuid, new Vec3(message.targetPos));
            }
        });
        context.setPacketHandled(true);
    }
}
