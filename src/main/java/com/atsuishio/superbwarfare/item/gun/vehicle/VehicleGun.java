package com.atsuishio.superbwarfare.item.gun.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.vehicle.VehicleProp;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

// TODO 实现基于VehicleGun的开火控制
public class VehicleGun extends GunItem {

    public VehicleGun() {
        super(new Properties());
    }

    // TODO dynamic icon
    @Override
    public ResourceLocation getGunIcon(GunData data) {
        return Mod.loc("textures/gun_icon/m_79_icon.png");
    }

    @Nullable
    public static GunData fromVehicle(VehicleEntity vehicle, int seatIndex) {
        var seats = vehicle.data().get(VehicleProp.SEATS);
        if (seatIndex < 0 || seatIndex >= seats.size()) return null;

        var seat = seats.get(seatIndex);
        if (seat.weaponData == null) return null;

        // TODO 正确读取和存储VehicleGun ItemStack
        var data = GunData.from(new ItemStack(ModItems.VEHICLE_GUN.get()));
        data.defaultDataSupplier = () -> seat.weaponData;

        return data;
    }
}