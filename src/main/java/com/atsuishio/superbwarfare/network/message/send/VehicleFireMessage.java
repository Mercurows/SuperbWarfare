package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class VehicleFireMessage {
    private final @Nullable UUID uuid;
    private final @Nullable Vector3f targetPos;

    public VehicleFireMessage(@Nullable UUID uuid) {
        this.uuid = uuid;
        this.targetPos = null;
    }

    public VehicleFireMessage(@Nullable UUID uuid, @Nullable Vector3f targetPos) {
        this.uuid = uuid;
        this.targetPos = targetPos;
    }

    public static VehicleFireMessage decode(FriendlyByteBuf buffer) {
        UUID uuid = buffer.readOptional(FriendlyByteBuf::readUUID).orElse(null);
        boolean flag = buffer.readBoolean();
        if (flag) {
            return new VehicleFireMessage(uuid, buffer.readVector3f());
        } else {
            return new VehicleFireMessage(uuid);
        }
    }

    public static void encode(VehicleFireMessage message, FriendlyByteBuf buffer) {
        buffer.writeOptional(Optional.ofNullable(message.uuid), FriendlyByteBuf::writeUUID);

        boolean flag = message.targetPos != null;
        buffer.writeBoolean(flag);
        if (flag) {
            buffer.writeVector3f(message.targetPos);
        }
    }

    public static void handler(VehicleFireMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null && player.getVehicle() instanceof VehicleEntity vehicle) {
                if (message.targetPos != null) {
                    vehicle.vehicleShoot(player, message.uuid, new Vec3(message.targetPos));
                } else {
                    vehicle.vehicleShoot(player, message.uuid, null);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
