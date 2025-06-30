package com.atsuishio.superbwarfare.data;

import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.ProjectileInfo;
import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData;
import com.atsuishio.superbwarfare.data.vehicle.VehicleData;

import java.util.HashMap;

public class CustomData {
    public static final HashMap<String, ProjectileInfo> LAUNCHABLE_ENTITY = DataLoader.createData("launchable", ProjectileInfo.class);
    public static final HashMap<String, DefaultVehicleData> VEHICLE = DataLoader.createData("vehicles", DefaultVehicleData.class, map -> VehicleData.dataCache.invalidateAll());
    public static final HashMap<String, DefaultGunData> GUN = DataLoader.createData("guns", DefaultGunData.class, map -> GunData.dataCache.invalidateAll());

    // 务必在Mod加载时调用该方法，确保上面的静态数据加载成功
    public static void load() {
    }
}
