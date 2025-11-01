package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.*;

public class VehicleFireMessage {

    private final int type;

    public VehicleFireMessage(int type) {
        this.type = type;
    }

    public static VehicleFireMessage decode(FriendlyByteBuf buffer) {
        return new VehicleFireMessage(buffer.readInt());
    }

    public static void encode(VehicleFireMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.type);
    }

    public static void handler(VehicleFireMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                var player = context.getSender();

                if (player.getVehicle() instanceof ArmedVehicleEntity iVehicle && player.getVehicle() instanceof VehicleEntity vehicle) {
                    iVehicle.vehicleShoot(player, message.type);

                    var gunData = vehicle.getGunData(vehicle.getSeatIndex(player));

                    if (gunData != null) {
                        vehicle.getEntityData().set(CANNON_RECOIL_TIME, gunData.get(GunProp.RECOIL_TIME));
                        vehicle.getEntityData().set(CANNON_RECOIL_FORCE, gunData.get(GunProp.RECOIL_FORCE));
                        var list = gunData.get(GunProp.SHOOT_POS).positions.list;
                        vehicle.currentFirePosIndex = ++vehicle.currentFirePosIndex % list.size();
                    }

                    vehicle.playShootSound3p(player);
                    vehicle.getEntityData().set(YAW_WHILE_SHOOT, vehicle.getTurretYRot());
                }
            }
        });
        context.setPacketHandled(true);
    }

}
