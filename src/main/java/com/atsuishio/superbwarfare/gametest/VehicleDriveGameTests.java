package com.atsuishio.superbwarfare.gametest;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.physics.VehiclePhysics;
import com.atsuishio.superbwarfare.physics.VehiclePhysicsDiagnostic;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Tracked-vehicle drive suite (ZTZ-99A is the reported failing vehicle): the one-block step contract through the
 * generated slope, the two-block wall refusal, the consecutive one-block
 * staircase and the unramped step through the climb assist, and the authored
 * yaw-rate limited pivot/turn behaviour. Driven with a real (non-player)
 * passenger over the authored strip. Wheeled vehicles are parked (see
 * GroundVehicleAllowlist).
 */
@GameTestHolder("superbwarfare_physics")
@PrefixGameTestTemplate(false)
public final class VehicleDriveGameTests {
    private VehicleDriveGameTests() { }

    @AfterBatch(batch = "driveBatch")
    public static void cleanup(net.minecraft.server.level.ServerLevel level) {
        VehicleDriveFixtures.cleanup();
    }

    @GameTest(template = "empty", timeoutTicks = 20000, batch = "driveBatch")
    public static void trackClimbsOneBlockStep(GameTestHelper h) {
        // Uniform pull-away throttle across the ground suite. The wedge is the
        // thing under test; the bare one-block face still cannot be mounted.
        VehicleDriveFixtures.climb(h, ModEntities.ZTZ_99A.get(), 1, 1, .6f, 1800);
    }

    @GameTest(template = "empty", timeoutTicks = 20000, batch = "driveBatch")
    public static void trackRefusesTwoBlockWall(GameTestHelper h) {
        VehicleDriveFixtures.climb(h, ModEntities.ZTZ_99A.get(), 3, 2, 1f, 900);
    }

    @GameTest(template = "empty", timeoutTicks = 20000, batch = "driveBatch")
    public static void trackClimbsOneBlockStaircase(GameTestHelper h) {
        // Consecutive one-block steps with single-block treads, built from
        // ordinary blocks like an in-game staircase. Only the first step can
        // ever receive a planner ramp, so the climb assist carries the rest.
        VehicleDriveFixtures.staircase(h, ModEntities.ZTZ_99A.get(), 5, 3, .6f, 2400);
    }

    @GameTest(template = "empty", timeoutTicks = 20000, batch = "driveBatch")
    public static void trackClimbsUnrampedOneBlockStep(GameTestHelper h) {
        // A one-block-deep pit one column inside the low run breaks the slope
        // planner's flat-run precondition, so the step keeps its raw collision
        // and only the climb assist can carry the vehicle over it.
        VehicleDriveFixtures.climb(h, ModEntities.ZTZ_99A.get(), 6, 1, .6f, 1800,
            (hh, x0, faceZ) -> hh.getLevel().setBlock(
                new BlockPos(x0 + 8, VehicleDriveFixtures.FLOOR_Y, faceZ - 3),
                Blocks.AIR.defaultBlockState(), 2));
    }

    @GameTest(template = "empty", timeoutTicks = 20000, batch = "driveBatch")
    public static void trackPivotAndTurnAreControlled(GameTestHelper h) {
        if (VehicleDriveFixtures.skipGroup("drive")) { h.succeed(); return; }
        int fixture = 7;
        int x0 = VehicleDriveFixtures.BASE_X + fixture * 64;
        VehicleEntity vehicle = VehicleDriveFixtures.newFixture(h, ModEntities.ZTZ_99A.get(), fixture, x0, 0, 0);
        long[] phaseFrame = {0};
        int[] phase = {0};
        int[] settled = {0};
        long[] settleBase = {-1};
        Vec3[] origin = {null};
        boolean[] done = {false};
        h.onEachTick(() -> {
            if (done[0]) return;
            VehiclePhysicsDiagnostic d = VehiclePhysics.INSTANCE.diagnostic(vehicle);
            if (d == null) return;
            if (phase[0] == 0) {
                if (settleBase[0] < 0) settleBase[0] = d.getFrame();
                if (d.getWaitingForTerrain()) { settled[0] = 0; settleBase[0] = d.getFrame(); return; }
                boolean calm = d.getVelocity().horizontalDistance() < .05 && Math.abs(d.getVelocity().y) < .05;
                settled[0] = calm ? settled[0] + 1 : 0;
                h.assertTrue(d.getFrame() - settleBase[0] <= 1800, "ztz_99a never settled: " + d);
                if (settled[0] >= 20) {
                    origin[0] = d.getPosition();
                    phase[0] = 1;
                    phaseFrame[0] = d.getFrame();
                    VehiclePhysics.INSTANCE.observeStability(vehicle, 600);
                }
                return;
            }
            // Right input for the pivot; a small forward power plus right for
            // the moving turn, so the arc stays inside the authored corridor
            // (the BMP authors MaxForwardSpeedRate 0.82, so 0.15 power is
            // already a ~2.5 m/s belt target).
            vehicle.setPower(phase[0] == 1 ? 0f : .15f);
            vehicle.processInput(phase[0] == 1 ? (short) 0b00000010 : (short) 0b00000110);
            var observation = VehiclePhysics.INSTANCE.stabilityObservation(vehicle);
            int required = phase[0] == 1 ? 600 : 240;
            if (observation == null || observation.getSamples() < required) {
                h.assertTrue(d.getFrame() - phaseFrame[0] <= 1600, "ztz_99a drive phase never observed: " + d);
                return;
            }
            if (phase[0] == 1) {
                double drift = d.getPosition().subtract(origin[0]).horizontalDistance();
                Mod.LOGGER.info("[drive-test] ztz_99a pivot observation={} drift={}", observation, drift);
                h.assertTrue(observation.getMaxAngularSpeed() <= 1.35,
                    "ztz_99a pivot exceeded the authored yaw-rate limit: " + observation);
                h.assertTrue(observation.getAngularJitterSteps() <= 6 && observation.getMaxAngularDelta() <= .08,
                    "ztz_99a pivot relay-chattered: " + observation);
                h.assertTrue(drift <= 1.2, "ztz_99a pivot left the spot: drift=" + drift);
                phase[0] = 2;
                phaseFrame[0] = d.getFrame();
                VehiclePhysics.INSTANCE.observeStability(vehicle, 240);
                return;
            }
            done[0] = true;
            double speed = d.getVelocity().horizontalDistance();
            Mod.LOGGER.info("[drive-test] ztz_99a turn observation={} speed={}", observation, speed);
            h.assertTrue(speed > .5, "ztz_99a moving turn lost speed: " + d);
            h.assertTrue(observation.getMaxAngularSpeed() <= 1.35,
                "ztz_99a moving turn exceeded the authored yaw-rate limit: " + observation);
            h.assertTrue(observation.getAngularJitterSteps() <= 12 && observation.getMaxAngularDelta() <= .08,
                "ztz_99a moving turn relay-chattered: " + observation);
            h.assertTrue(observation.getMinSupportContacts() == 2 && !observation.getTerrainLost(),
                "ztz_99a moving turn left the ground: " + observation);
            h.succeed();
        });
    }
}
