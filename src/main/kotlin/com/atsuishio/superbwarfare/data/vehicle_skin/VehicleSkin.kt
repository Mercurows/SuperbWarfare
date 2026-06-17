package com.atsuishio.superbwarfare.data.vehicle_skin

import com.atsuishio.superbwarfare.data.CustomData
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.minecraft.world.entity.EntityType

class VehicleSkin private constructor(val vehicleId: String) {

    private var cache: VehicleSkinData? = null

    fun compute(): VehicleSkinData {
        if (cache != null) return cache!!
        val data = CustomData.VEHICLE_SKINS.getOrElseGet(vehicleId) { VehicleSkinData() }
        cache = data
        return data
    }

    fun update() {
        cache = null
    }

    fun getSkin(skinId: String): SkinInfo? {
        if (skinId.isBlank()) return null
        return compute().skins.find { it.id == skinId }
    }

    companion object {
        @JvmStatic
        fun getSkin(entity: VehicleEntity): SkinInfo? {
            if (entity.skinId.isBlank()) return null
            return from(entity.type).getSkin(entity.skinId)
        }

        @JvmStatic
        fun getSkin(entityType: EntityType<*>, skinId: String): SkinInfo? {
            if (skinId.isBlank()) return null
            return from(entityType).getSkin(skinId)
        }

        @JvmStatic
        fun getSkins(entityType: EntityType<*>): VehicleSkinData {
            return from(entityType).compute()
        }

        @JvmStatic
        fun getSkins(vehicleId: String): VehicleSkinData {
            return from(vehicleId).compute()
        }

        @JvmStatic
        fun from(entityType: EntityType<*>): VehicleSkin {
            return from(EntityType.getKey(entityType).toString())
        }

        @JvmStatic
        fun from(entity: VehicleEntity): VehicleSkin {
            return from(entity.type)
        }

        @JvmField
        val DATA_CACHE: LoadingCache<String, VehicleSkin> = CacheBuilder.newBuilder()
            .build(object : CacheLoader<String, VehicleSkin>() {
                override fun load(id: String): VehicleSkin {
                    return VehicleSkin(id)
                }
            })

        @JvmStatic
        fun from(vehicleId: String): VehicleSkin {
            return DATA_CACHE.getUnchecked(vehicleId)
        }
    }
}
