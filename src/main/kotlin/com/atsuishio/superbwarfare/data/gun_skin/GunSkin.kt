package com.atsuishio.superbwarfare.data.gun_skin

import com.atsuishio.superbwarfare.data.CustomData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.minecraft.world.item.ItemStack

/** Datapack skins keyed by gun item registry id, shared by all stacks of that gun. */
class GunSkin private constructor(val gunId: String) {
    // Read through the data map so even a retained GunSkin sees reloads and server synchronization.
    fun compute(): GunSkinData = CustomData.GUN_SKINS.getOrElseGet(gunId) { GunSkinData() }

    fun getSkin(skinId: String): GunSkinInfo? =
        if (skinId.isBlank()) null else compute().skins.find { it.id == skinId }

    companion object {
        @JvmField
        val DATA_CACHE: LoadingCache<String, GunSkin> = CacheBuilder.newBuilder()
            .build(object : CacheLoader<String, GunSkin>() {
                override fun load(id: String) = GunSkin(id)
            })

        @JvmStatic
        fun from(gunId: String): GunSkin = DATA_CACHE.getUnchecked(gunId)

        @JvmStatic
        fun from(data: GunData): GunSkin = from(data.id)

        @JvmStatic
        fun getSkins(data: GunData): GunSkinData = from(data).compute()

        @JvmStatic
        fun getSkins(gunId: String): GunSkinData = from(gunId).compute()

        @JvmStatic
        fun getSkin(data: GunData): GunSkinInfo? = from(data).getSkin(data.skinId.get())

        @JvmStatic
        fun getSkin(stack: ItemStack): GunSkinInfo? =
            if (stack.item is GunItem) getSkin(GunData.from(stack)) else null
    }
}
