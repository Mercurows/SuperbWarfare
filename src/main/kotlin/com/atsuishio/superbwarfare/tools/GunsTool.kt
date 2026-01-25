package com.atsuishio.superbwarfare.tools

import net.minecraft.nbt.CompoundTag
import java.util.*

object GunsTool {
    @JvmStatic
    fun getGunDoubleTag(tag: CompoundTag, name: String): Double {
        return getGunDoubleTag(tag, name, 0.0)
    }

    fun getGunDoubleTag(tag: CompoundTag, name: String, defaultValue: Double): Double {
        val data = tag.getCompound("GunData")
        if (!data.contains(name)) return defaultValue
        return data.getDouble(name)
    }

    fun getGunUUID(tag: CompoundTag): UUID? {
        if (!tag.contains("GunData")) return null

        val data = tag.getCompound("GunData")
        if (!data.hasUUID("UUID")) return null
        return data.getUUID("UUID")
    }
}