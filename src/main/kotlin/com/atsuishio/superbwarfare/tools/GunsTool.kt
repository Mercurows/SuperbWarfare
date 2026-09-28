package com.atsuishio.superbwarfare.tools

import net.minecraft.world.item.ItemStack
import java.util.*

object GunsTool {
    @JvmStatic
    fun getGunDoubleTag(stack: ItemStack, name: String): Double {
        return getGunDoubleTag(stack, name, 0.0)
    }

    fun getGunDoubleTag(stack: ItemStack, name: String, defaultValue: Double): Double {
        val data = stack.getTagElement("GunData") ?: return defaultValue
        if (!data.contains(name)) return defaultValue
        return data.getDouble(name)
    }

    fun getGunUUID(stack: ItemStack): UUID? {
        val data = stack.getTagElement("GunData") ?: return null
        if (!data.hasUUID("UUID")) return null
        return data.getUUID("UUID")
    }
}