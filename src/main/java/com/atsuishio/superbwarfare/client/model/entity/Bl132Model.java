package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.Bl132Entity;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class Bl132Model extends VehicleModel<Bl132Entity> {

    @Override
    public ResourceLocation getTextureResource(Bl132Entity entity) {
        UUID uuid = entity.getUUID();
        if (uuid.getLeastSignificantBits() % 50 == 0) {
            return Mod.loc("textures/entity/bl_132_black.png");
        }
        return Mod.loc("textures/entity/bl_132.png");
    }

    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }
}
