package com.atsuishio.superbwarfare.entity.vehicle.base;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleEngineCoastingTest {
    @Test
    void keepsMostSpeedDuringFirstSecondWithEngineOff() {
        var velocity = new VehicleEngineCoasting.Velocity(1.0, -0.25, 0.5);
        for (int tick = 0; tick < 20; tick++) {
            velocity = VehicleEngineCoasting.coast(
                    velocity.getX(), velocity.getY(), velocity.getZ(), true
            );
        }

        assertTrue(
                velocity.getX() >= 0.8,
                "an engine-off vehicle should coast instead of stopping immediately"
        );
        assertEquals(-0.25, velocity.getY(), 0.0001, "ground coasting must preserve vertical motion");
    }

    @Test
    void rollingResistanceStillBringsVehicleToAStop() {
        var velocity = new VehicleEngineCoasting.Velocity(1.0, 0.0, 0.0);
        for (int tick = 0; tick < 300; tick++) {
            velocity = VehicleEngineCoasting.coast(
                    velocity.getX(), velocity.getY(), velocity.getZ(), true
            );
        }

        assertTrue(
                velocity.getX() < 0.1,
                "coasting must still lose speed over time"
        );
    }

    @Test
    void airborneCoastingDampsEveryAxis() {
        var velocity = VehicleEngineCoasting.coast(1.0, -1.0, 0.5, false);
        assertEquals(0.99, velocity.getX(), 0.0001);
        assertEquals(-0.99, velocity.getY(), 0.0001);
        assertEquals(0.495, velocity.getZ(), 0.0001);
    }
}
