package com.atsuishio.superbwarfare.data

import com.atsuishio.superbwarfare.data.drone_attachment.DroneAttachmentData
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.ProjectileInfo
import com.atsuishio.superbwarfare.data.mob_guns.DefaultMobGunData
import com.atsuishio.superbwarfare.data.mob_guns.MobGunData
import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData
import com.atsuishio.superbwarfare.data.vehicle.VehicleData
import com.atsuishio.superbwarfare.data.vehicle_skin.VehicleSkin
import com.atsuishio.superbwarfare.data.vehicle_skin.VehicleSkinData
import com.atsuishio.superbwarfare.resource.gun.DefaultGunResource
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.resource.vehicle.DefaultVehicleResource
import com.atsuishio.superbwarfare.resource.vehicle.VehicleResource

object CustomData {

    // Data

    @JvmField
    val LAUNCHABLE_ENTITY = DataLoader.createData("sbw/launchable", ProjectileInfo::class.java)

    @JvmField
    val VEHICLE_DATA = DataLoader.createData(
        "sbw/vehicles", DefaultVehicleData::class.java, true, isKtData = true
    ) { _ -> VehicleData.dataCache.invalidateAll() }

    @JvmField
    val GUN_DATA = DataLoader.createData(
        "sbw/guns", DefaultGunData::class.java, true, isKtData = true
    ) { _ -> GunData.DATA_CACHE.invalidateAll() }

    @JvmField
    val DRONE_ATTACHMENT = DataLoader.createData("sbw/drone_attachments", DroneAttachmentData::class.java)

    @JvmField
    val MOB_GUNS = DataLoader.createData(
        "sbw/mob_guns", DefaultMobGunData::class.java
    ) { _ -> MobGunData.dataCache.invalidateAll() }

    @JvmField
    val VEHICLE_SKINS = DataLoader.createData(
        "sbw/vehicle_skins", VehicleSkinData::class.java, true, isKtData = true
    ) { _ -> VehicleSkin.DATA_CACHE.invalidateAll() }

    // Resource

    @JvmField
    val GUN_RESOURCE = DataLoader.createResource(
        "sbw/guns", DefaultGunResource::class.java
    ) { _ -> GunResource.RESOURCE_CACHE.invalidateAll() }

    @JvmField
    val VEHICLE_RESOURCE = DataLoader.createResource(
        "sbw/vehicles", DefaultVehicleResource::class.java, isKtData = true
    ) { _ -> VehicleResource.RESOURCE_CACHE.invalidateAll() }

    // 务必在Mod加载时调用该方法，确保上面的静态数据加载成功
    fun load() {}
}
