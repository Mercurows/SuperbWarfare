package com.atsuishio.superbwarfare.data;

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
import com.atsuishio.superbwarfare.resource.vehicle.DefaultVehicleResource;
import com.atsuishio.superbwarfare.resource.vehicle.VehicleResource;

public class CustomData {
    public static final DataLoader.DataMap<ProjectileInfo> LAUNCHABLE_ENTITY = DataLoader.createData("sbw/launchable", ProjectileInfo.class);
    public static final DataLoader.DataMap<DefaultVehicleData> VEHICLE_DATA = DataLoader.createData("sbw/vehicles", DefaultVehicleData.class, map -> VehicleData.dataCache.invalidateAll());
    public static final DataLoader.DataMap<DefaultGunData> GUN_DATA = DataLoader.createData("sbw/guns", DefaultGunData.class, map -> GunData.DATA_CACHE.invalidateAll());
    public static final DataLoader.DataMap<DroneAttachmentData> DRONE_ATTACHMENT = DataLoader.createData("sbw/drone_attachments", DroneAttachmentData.class);
    public static final DataLoader.DataMap<DefaultMobGunData> MOB_GUNS = DataLoader.createData("sbw/mob_guns", DefaultMobGunData.class, map -> MobGunData.dataCache.invalidateAll());

    public static final DataLoader.DataMap<DefaultGunResource> GUN_RESOURCE = DataLoader.createResource("sbw/guns", DefaultGunResource.class, map -> GunResource.RESOURCE_CACHE.invalidateAll());
    public static final DataLoader.DataMap<DefaultVehicleResource> VEHICLE_RESOURCE = DataLoader.createResource("sbw/vehicles", DefaultVehicleResource.class, map -> VehicleResource.RESOURCE_CACHE.invalidateAll());

    // 务必在Mod加载时调用该方法，确保上面的静态数据加载成功
    public static void load() {
    }
}
