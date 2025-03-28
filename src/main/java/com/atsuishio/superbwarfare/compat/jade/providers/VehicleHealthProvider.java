package com.atsuishio.superbwarfare.compat.jade.providers;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.compat.jade.elements.WrenchHealthElement;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum VehicleHealthProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final ResourceLocation ID = Mod.loc("vehicle_health");

    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        // 对EntityHealthAndArmorProvider的拙劣模仿罢了

        var vehicle = (VehicleEntity) accessor.getEntity();
        float health = vehicle.getHealth();
        float maxHealth = vehicle.getMaxHealth();
        tooltip.add(new WrenchHealthElement(maxHealth, health));
    }

    public ResourceLocation getUid() {
        return ID;
    }

    public int getDefaultPriority() {
        return -4501;
    }
}

