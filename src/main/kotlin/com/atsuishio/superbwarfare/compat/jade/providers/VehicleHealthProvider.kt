package com.atsuishio.superbwarfare.compat.jade.providers

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.compat.jade.elements.WrenchHealthElement
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.resources.ResourceLocation
import snownee.jade.api.EntityAccessor
import snownee.jade.api.IEntityComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig

object VehicleHealthProvider : IEntityComponentProvider {
    private val ID = loc("vehicle_health")

    override fun appendTooltip(tooltip: ITooltip, accessor: EntityAccessor, config: IPluginConfig?) {
        // 对EntityHealthProvider的拙劣模仿罢了
        val vehicle = accessor.entity as VehicleEntity
        val health = vehicle.health
        val maxHealth = vehicle.getMaxHealth()
        tooltip.add(WrenchHealthElement(maxHealth, health))
    }

    override fun getUid(): ResourceLocation {
        return ID
    }

    override fun getDefaultPriority(): Int {
        return -4501
    }
}

