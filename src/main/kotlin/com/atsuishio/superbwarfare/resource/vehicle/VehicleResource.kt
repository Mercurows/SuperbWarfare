package com.atsuishio.superbwarfare.resource.vehicle

import com.atsuishio.superbwarfare.data.CustomData
import com.atsuishio.superbwarfare.data.DefaultDataSupplier
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.minecraft.world.entity.EntityType

class VehicleResource private constructor(val vehicle: VehicleEntity) : DefaultDataSupplier<DefaultVehicleResource> {
    val id: String = getRegistryId(vehicle.type)

    private var cache: DefaultVehicleResource? = null

    fun compute(): DefaultVehicleResource {
        if (cache != null) return cache!!

        val defaultResource = getDefault().copy()

        // TODO 正确实现属性计算
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
        val RESOURCE_CACHE: LoadingCache<VehicleEntity, VehicleResource> = CacheBuilder.newBuilder()
            .weakKeys()
            .weakValues()
            .build(object : CacheLoader<VehicleEntity, VehicleResource>() {
                override fun load(vehicle: VehicleEntity): VehicleResource {
                    return VehicleResource(vehicle)
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
        fun from(stack: VehicleEntity): VehicleResource {
            return RESOURCE_CACHE.getUnchecked(stack)
        }

        @JvmStatic
        fun getRegistryId(type: EntityType<*>): String {
            return EntityType.getKey(type).toString()
        }
    }
}
