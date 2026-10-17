package com.atsuishio.superbwarfare.gametest;

import com.atsuishio.superbwarfare.init.ModEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Parked wheeled-vehicle drive suite. Wheeled ground physics is registered but
 * disabled while the tracked probe suspension completes acceptance, so this
 * holder is intentionally outside the standard physics namespace and does not
 * run from the usual command. Re-enable the wheeled vehicles in
 * GroundVehicleAllowlist, then run it with
 * {@code -PgameTestNamespaces=superbwarfare_physics_wheel}.
 */
@GameTestHolder("superbwarfare_physics_wheel")
@PrefixGameTestTemplate(false)
public final class WheeledDriveGameTests {
    private WheeledDriveGameTests() { }

    @GameTest(template = "empty", timeoutTicks = 20000, batch = "driveBatch")
    public static void wheelClimbsOneBlockStep(GameTestHelper h) {
        VehicleDriveFixtures.climb(h, ModEntities.LAV_25.get(), 2, 1, .6f, 1800);
    }

    @GameTest(template = "empty", timeoutTicks = 20000, batch = "driveBatch")
    public static void wheelDrivesCoastsAndReverses(GameTestHelper h) {
        VehicleDriveFixtures.drive(h, ModEntities.LAV_25.get(), 4);
    }
}
