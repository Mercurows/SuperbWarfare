package com.atsuishio.superbwarfare.resource.gun

import com.atsuishio.superbwarfare.data.CustomData
import com.atsuishio.superbwarfare.data.DefaultDataSupplier
import com.atsuishio.superbwarfare.item.gun.EmptyGunItem
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

class GunResource private constructor(val id: String) : DefaultDataSupplier<DefaultGunResource> {
    private var cache: DefaultGunResource? = null

    fun compute(): DefaultGunResource {
        if (cache != null) return cache!!

        // TODO 正确实现属性计算
        // The datapack default is shared read-only (no per-instance modification exists yet), so it
        // is returned directly instead of GSON-deep-copying it. If resources ever depend on gun NBT,
        // the cache key must be widened to those fields as well.
        val defaultResource = getDefault()

        cache = defaultResource
        return defaultResource
    }

    fun update() {
        this.cache = null
    }

    override fun getDefault(): DefaultGunResource {
        return getDefault(id)
    }

    companion object {
        /**
         * Keyed by item registry id, not by [ItemStack] identity: the resource is per item type, and
         * an identity-keyed cache was re-resolved (and re-copied) on every client-side item resync.
         */
        val RESOURCE_CACHE: LoadingCache<String, GunResource> = CacheBuilder.newBuilder()
            .maximumSize(1024)
            .build(object : CacheLoader<String, GunResource>() {
                override fun load(id: String): GunResource {
                    return GunResource(id)
                }
            })

        @JvmStatic
        fun compute(stack: ItemStack): DefaultGunResource {
            return from(stack).compute()
        }

        @JvmStatic
        fun getDefault(id: String?): DefaultGunResource {
            return CustomData.GUN_RESOURCE.getOrElseGet(id) { DefaultGunResource() }
        }

        @JvmStatic
        fun getDefault(stack: ItemStack): DefaultGunResource {
            return getDefault(stack.item)
        }

        @JvmStatic
        fun getDefault(item: Item): DefaultGunResource {
            return getDefault(getRegistryId(item))
        }

        @JvmStatic
        fun create(item: Item): GunResource {
            return from(ItemStack(item))
        }

        @JvmStatic
        fun from(stack: ItemStack): GunResource {
            return RESOURCE_CACHE.getUnchecked(idOf(stack))
        }

        /** Resolves the resource cache key of [stack], matching the id used by the constructor. */
        @JvmStatic
        fun idOf(stack: ItemStack): String {
            val gunItem = stack.item as? GunItem
            return if (gunItem == null || stack.isEmpty) EmptyGunItem.EMPTY_GUN_ID else getRegistryId(stack.item)
        }

        @JvmStatic
        fun getRegistryId(item: Item): String {
            var id = item.descriptionId
            id = id.substring(id.indexOf(".") + 1).replace('.', ':')
            return id
        }
    }
}
