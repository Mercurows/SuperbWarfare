package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class Ah6Entity extends GeoVehicleEntity {

    public Ah6Entity(EntityType<Ah6Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> {
                    damage *= getHealth() > 0.1f ? 1 : 0.05f;
                    return damage;
                });
    }

    @Override
    public double getMouseSensitivity() {
        return 0.25;
    }
}
