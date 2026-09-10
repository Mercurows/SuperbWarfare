package com.atsuishio.superbwarfare.resource.vehicle

import com.atsuishio.superbwarfare.data.CustomData
import com.atsuishio.superbwarfare.data.DefaultDataSupplier
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.minecraft.world.entity.EntityType

class VehicleResource private constructor(val id: String) : DefaultDataSupplier<DefaultVehicleResource> {
    private var cache: DefaultVehicleResource? = null

    fun compute(): DefaultVehicleResource {
        if (cache != null) return cache!!

        // TODO 正确实现属性计算
        // The datapack default is shared read-only (no per-instance modification exists yet), so it
        // is returned directly instead of GSON-deep-copying it. Once properties are computed this
        // must become a projection over the default, like GunProp/PMC.
        val defaultResource = getDefault()

        cache = defaultResource
        return defaultResource
    }

    fun update() {
        this.cache = null
    }

    override fun getDefault(): DefaultVehicleResource {
        return getDefault(this.id)
    }

    companion object {
        /**
         * Keyed by vehicle type id, not by [VehicleEntity] identity: the resource is per type, and an
         * identity-keyed cache was re-resolved (and re-copied) for every entity.
         */
        val RESOURCE_CACHE: LoadingCache<String, VehicleResource> = CacheBuilder.newBuilder()
            .maximumSize(512)
            .build(object : CacheLoader<String, VehicleResource>() {
                override fun load(id: String): VehicleResource {
                    return VehicleResource(id)
                }
            })

        @JvmStatic
        fun compute(vehicle: VehicleEntity): DefaultVehicleResource {
            return from(vehicle).compute()
        }

        @JvmStatic
        fun getDefault(id: String): DefaultVehicleResource {
            return CustomData.VEHICLE_RESOURCE.getOrElseGet(id) { DefaultVehicleResource() }
        }

        @JvmStatic
        fun getDefault(vehicle: VehicleEntity): DefaultVehicleResource {
            return getDefault(vehicle.type)
        }

        @JvmStatic
        fun getDefault(type: EntityType<*>): DefaultVehicleResource {
            return getDefault(getRegistryId(type))
        }

        @JvmStatic
        fun from(vehicle: VehicleEntity): VehicleResource {
            return RESOURCE_CACHE.getUnchecked(getRegistryId(vehicle.type))
        }

        @JvmStatic
        fun getRegistryId(type: EntityType<*>): String {
            return EntityType.getKey(type).toString()
        }
    }
}
