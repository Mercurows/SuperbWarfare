package com.atsuishio.superbwarfare.tools

import net.minecraft.world.item.ItemStack
import java.util.*

object GunsTool {
    @JvmStatic
    fun getGunDoubleTag(stack: ItemStack, name: String): Double {
        return getGunDoubleTag(stack, name, 0.0)
    }

    fun getGunDoubleTag(stack: ItemStack, name: String, defaultValue: Double): Double {
        val data = stack.getOrCreateTag().getCompound("GunData")
        if (!data.contains(name)) return defaultValue
        return data.getDouble(name)
    }

    fun getGunUUID(stack: ItemStack): UUID? {
        val tag = stack.tag ?: return null
        if (!tag.contains("GunData")) return null

        val data = tag.getCompound("GunData")
        if (!data.hasUUID("UUID")) return null
        return data.getUUID("UUID")
    }
}