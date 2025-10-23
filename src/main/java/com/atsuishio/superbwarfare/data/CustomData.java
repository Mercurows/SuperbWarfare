package com.atsuishio.superbwarfare.data;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.drone_attachment.DroneAttachmentData;
import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.ProjectileInfo;
import com.atsuishio.superbwarfare.data.mob_guns.DefaultMobGunData;
import com.atsuishio.superbwarfare.data.mob_guns.MobGunData;
import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData;
import com.atsuishio.superbwarfare.data.vehicle.VehicleData;
import com.atsuishio.superbwarfare.resource.gun.DefaultGunResource;
import com.atsuishio.superbwarfare.resource.gun.GunResource;

public class CustomData {
    public static final DataLoader.DataMap<ProjectileInfo> LAUNCHABLE_ENTITY = DataLoader.createData(Mod.MODID, "launchable", ProjectileInfo.class);
    public static final DataLoader.DataMap<DefaultVehicleData> VEHICLE_DATA = DataLoader.createData(Mod.MODID, "vehicles", DefaultVehicleData.class, map -> VehicleData.dataCache.invalidateAll());
    public static final DataLoader.DataMap<DefaultGunData> GUN_DATA = DataLoader.createData(Mod.MODID, "guns", DefaultGunData.class, map -> GunData.DATA_CACHE.invalidateAll());
    public static final DataLoader.DataMap<DroneAttachmentData> DRONE_ATTACHMENT = DataLoader.createData(Mod.MODID, "drone_attachments", DroneAttachmentData.class);
    public static final DataLoader.DataMap<DefaultMobGunData> MOB_GUNS = DataLoader.createData(Mod.MODID, "mob_guns", DefaultMobGunData.class, map -> MobGunData.dataCache.invalidateAll());

    public static final DataLoader.DataMap<DefaultGunResource> GUN_RESOURCE = DataLoader.createResource(Mod.MODID, "guns", DefaultGunResource.class, map -> GunResource.RESOURCE_CACHE.invalidateAll());

    // 务必在Mod加载时调用该方法，确保上面的静态数据加载成功
    public static void load() {
    }
}
