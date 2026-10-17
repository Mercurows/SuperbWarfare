package com.atsuishio.superbwarfare.gametest;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.physics.VehiclePhysics;
import com.atsuishio.superbwarfare.physics.VehiclePhysicsDiagnostic;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Authored-strip fixtures shared by the tracked physics suite and the parked
 * wheeled suite, so the strip geometry, simulated driver and cleanup stay
 * identical when the parked suite is re-enabled.
 */
final class VehicleDriveFixtures {
    private VehicleDriveFixtures() { }

    /** Extra authored terrain applied after the strip, before the vehicle spawns. */
    interface TerrainCustomizer {
        void apply(GameTestHelper h, int x0, int faceZ);
    }

    static final List<Runnable> CLEANUP = new ArrayList<>();
    /** processInput bit for "forward held" (see VehicleEntity.processInput). */
    static final short FORWARD = (short) 0b00000100;

    /** Optional focused run: {@code -PphysicsTestGroup=drive} or {@code stability}. */
    static boolean skipGroup(String group) {
        String selected = System.getProperty("superbwarfare.physics.testGroup", "all");
        return !"all".equals(selected) && !selected.equals(group);
    }
    static final int FLOOR_Y = 64;
    static final int BASE_X = 2048;
    static final int BASE_Z = 1024;

    static void cleanup() {
        CLEANUP.forEach(Runnable::run);
        CLEANUP.clear();
    }

    /** Builds a flat strip whose far half rises [stepHeight] blocks at an
     * authored vertical face, then drives at it: a one-block step must be
     * climbable through the generated ramp, a two-block wall must not be. */
    static void climb(GameTestHelper h, EntityType<? extends VehicleEntity> type,
                      int fixture, int stepHeight, float power, int driveFrames) {
        climb(h, type, fixture, stepHeight, power, driveFrames, null);
    }

    static void climb(GameTestHelper h, EntityType<? extends VehicleEntity> type,
                      int fixture, int stepHeight, float power, int driveFrames,
                      TerrainCustomizer customizer) {
        if (skipGroup("drive")) { h.succeed(); return; }
        int x0 = BASE_X + fixture * 64;
        int faceZ = BASE_Z + 8;
        // Short run-up so the support probes meet the ramp at a modest speed.
        VehicleEntity vehicle = newFixture(h, type, fixture, x0, faceZ, stepHeight, BASE_Z - 2, customizer);
        long[] driveBase = {0};
        Vec3[] origin = {null};
        int[] settled = {0};
        int[] phase = {0};
        long[] settleBase = {-1};
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
                h.assertTrue(d.getFrame() - settleBase[0] <= 1800,
                    type + " never settled on the authored strip: " + d);
                if (settled[0] >= 20) {
                    origin[0] = d.getPosition();
                    driveBase[0] = d.getFrame();
                    phase[0] = 1;
                    vehicle.setPower(power);
                    vehicle.processInput(FORWARD);
                }
                return;
            }
            // Hold the throttle the way a driver does. Setting only `power`
            // lets the legacy engine bleed it away (power *= 0.96 per tick
            // without a forward input), which silently removed the drive from
            // any climb longer than a second and made a stalled vehicle look
            // like a terrain failure.
            vehicle.setPower(power);
            vehicle.processInput(FORWARD);
            double rise = d.getPosition().y - origin[0].y;
            double advance = d.getPosition().z - origin[0].z;
            double lateral = Math.abs(d.getPosition().x - origin[0].x);
            double speed = d.getVelocity().horizontalDistance();
            if (stepHeight == 1) {
                // Score the climb at the crest: once the chassis is up on the
                // plateau past the face, the ramp has been driven and the
                // vehicle must still be rolling. Driving on afterwards only
                // measures the far edge of the authored strip.
                if (rise >= .75 && d.getPosition().z >= faceZ + 1) {
                    done[0] = true;
                    Mod.LOGGER.info("[drive-test] {} fixture={} step={} climbed rise={} advance={} speed={} pos={}",
                        type, fixture, stepHeight, rise, advance, speed, d.getPosition());
                    h.assertTrue(speed > .2, type + " stalled instead of climbing the ramp: speed=" + speed
                        + " diagnostic=" + d);
                    h.assertTrue(lateral < 2.5, type + " left the authored strip: lateral=" + lateral);
                    h.assertTrue(advance > 4, type + " never advanced: " + advance);
                    h.succeed();
                    return;
                }
                if (d.getFrame() - driveBase[0] >= driveFrames) {
                    done[0] = true;
                    Mod.LOGGER.info("[drive-test] {} fixture={} step={} never-climbed rise={} advance={} speed={} pos={}",
                        type, fixture, stepHeight, rise, advance, speed, d.getPosition());
                    h.assertTrue(false, type + " did not climb the one-block step: rise=" + rise
                        + " z=" + d.getPosition().z + " face=" + faceZ + " diagnostic=" + d);
                }
                return;
            }
            if (d.getFrame() - driveBase[0] < driveFrames) return;
            done[0] = true;
            Mod.LOGGER.info("[drive-test] {} fixture={} step={} rise={} advance={} speed={} pos={}",
                type, fixture, stepHeight, rise, advance, speed, d.getPosition());
            h.assertTrue(rise < .75, type + " climbed a two-block wall without an authored ramp: rise=" + rise
                + " diagnostic=" + d);
            h.assertTrue(d.getPosition().z < faceZ + .5, type + " passed a two-block wall: z="
                + d.getPosition().z + " face=" + faceZ + " diagnostic=" + d);
            h.assertTrue(lateral < 2.5, type + " left the authored strip: lateral=" + lateral);
            h.assertTrue(advance > 4, type + " never advanced: " + advance);
            h.succeed();
        });
    }

    /** Authored staircase of consecutive one-block steps (1-block treads):
     * the tracked vehicle must climb to the top. The slope planner only ramps
     * the first step, because every later low side is a single tread block, so
     * the climb-assist surface is what the test actually exercises. */
    static void staircase(GameTestHelper h, EntityType<? extends VehicleEntity> type,
                          int fixture, int steps, float power, int driveFrames) {
        if (skipGroup("drive")) { h.succeed(); return; }
        int x0 = BASE_X + fixture * 64;
        int faceZ = BASE_Z + 8;
        VehicleEntity vehicle = newFixture(h, type, fixture, x0, 0, 0, BASE_Z - 2,
            (hh, fx, unusedFaceZ) -> {
                for (int dx = -3; dx <= 18; dx++)
                    for (int k = 0; k < steps; k++) {
                        int zStart = faceZ + k;
                        // The last tread extends into a plateau, so the crest
                        // can be scored before the vehicle rolls off again.
                        int zEnd = (k == steps - 1) ? faceZ + steps + 4 : faceZ + k + 1;
                        for (int z = zStart; z < zEnd; z++)
                            for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 1 + k; y++)
                                hh.getLevel().setBlock(new BlockPos(fx + dx, y, z),
                                    Blocks.STONE.defaultBlockState(), 2);
                    }
            });
        long[] driveBase = {0};
        Vec3[] origin = {null};
        int[] settled = {0};
        int[] phase = {0};
        long[] settleBase = {-1};
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
                h.assertTrue(d.getFrame() - settleBase[0] <= 1800,
                    type + " never settled on the authored strip: " + d);
                if (settled[0] >= 20) {
                    origin[0] = d.getPosition();
                    driveBase[0] = d.getFrame();
                    phase[0] = 1;
                    vehicle.setPower(power);
                    vehicle.processInput(FORWARD);
                }
                return;
            }
            // Hold the throttle the way a driver does. Setting only `power`
            // lets the legacy engine bleed it away (power *= 0.96 per tick
            // without a forward input), which silently removed the drive from
            // any climb longer than a second and made a stalled vehicle look
            // like a terrain failure.
            vehicle.setPower(power);
            vehicle.processInput(FORWARD);
            double rise = d.getPosition().y - origin[0].y;
            double advance = d.getPosition().z - origin[0].z;
            double lateral = Math.abs(d.getPosition().x - origin[0].x);
            double speed = d.getVelocity().horizontalDistance();
            if (rise >= steps - .25 && d.getPosition().z >= faceZ + steps + .5) {
                done[0] = true;
                Mod.LOGGER.info("[drive-test] {} fixture={} staircase={} climbed rise={} advance={} speed={} pos={}",
                    type, fixture, steps, rise, advance, speed, d.getPosition());
                h.assertTrue(speed > .2, type + " stalled instead of climbing the staircase: speed=" + speed
                    + " diagnostic=" + d);
                h.assertTrue(lateral < 2.5, type + " left the authored strip: lateral=" + lateral);
                h.assertTrue(advance > 4, type + " never advanced: " + advance);
                h.succeed();
                return;
            }
            if (d.getFrame() - driveBase[0] >= driveFrames) {
                done[0] = true;
                Mod.LOGGER.info("[drive-test] {} fixture={} staircase={} never-climbed rise={} advance={} speed={} pos={}",
                    type, fixture, steps, rise, advance, speed, d.getPosition());
                h.assertTrue(false, type + " did not climb the staircase: rise=" + rise
                    + " z=" + d.getPosition().z + " face=" + faceZ + " diagnostic=" + d);
            }
        });
    }

    /** Flat strip: forward drive, throttle release (coast), then reverse demand. */
    static void drive(GameTestHelper h, EntityType<? extends VehicleEntity> type, int fixture) {
        int x0 = BASE_X + fixture * 64;
        VehicleEntity vehicle = newFixture(h, type, fixture, x0, 0, 0);
        long[] phaseFrame = {0};
        Vec3[] origin = {null};
        Vec3[] releaseVelocity = {Vec3.ZERO};
        int[] settled = {0};
        int[] phase = {0};
        long[] settleBase = {-1};
        boolean[] probed = {false};
        boolean[] done = {false};
        h.onEachTick(() -> {
            if (done[0]) return;
            VehiclePhysicsDiagnostic d = VehiclePhysics.INSTANCE.diagnostic(vehicle);
            if (d == null) return;
            if (phase[0] == 0 && !probed[0]) {
                probed[0] = true;
                logWheels(vehicle, type, "settle");
            }
            if (phase[0] == 0) {
                if (settleBase[0] < 0) {
                    settleBase[0] = d.getFrame();
                    Mod.LOGGER.info("[drive-test] {} settle start pos={}", type, d.getPosition());
                }
                if (d.getWaitingForTerrain()) { settled[0] = 0; settleBase[0] = d.getFrame(); return; }
                boolean calm = d.getVelocity().horizontalDistance() < .05 && Math.abs(d.getVelocity().y) < .05;
                settled[0] = calm ? settled[0] + 1 : 0;
                h.assertTrue(d.getFrame() - settleBase[0] <= 1800,
                    type + " never settled on the authored strip: " + d);
                if (settled[0] >= 20) {
                    origin[0] = d.getPosition();
                    phaseFrame[0] = d.getFrame();
                    phase[0] = 1;
                    Mod.LOGGER.info("[drive-test] {} settle end pos={} frames={}", type, d.getPosition(),
                        d.getFrame() - settleBase[0]);
                    vehicle.setPower(.6f);
                    vehicle.processInput(FORWARD);
                }
                return;
            }
            if (phase[0] == 1) {
                vehicle.setPower(.6f);
                vehicle.processInput(FORWARD);
                if (d.getFrame() - phaseFrame[0] < 180) return;
                logWheels(vehicle, type, "after-drive");
                double advance = d.getPosition().z - origin[0].z;
                double lateral = Math.abs(d.getPosition().x - origin[0].x);
                h.assertTrue(advance >= 4, type + " did not drive forward: " + advance + " diagnostic=" + d);
                h.assertTrue(lateral < 2.5, type + " did not drive straight: lateral=" + lateral);
                h.assertTrue(d.getVelocity().z > .5, type + " forward drive produced no forward speed: " + d);
                releaseVelocity[0] = d.getVelocity();
                phaseFrame[0] = d.getFrame();
                phase[0] = 2;
                vehicle.setPower(0f);
                return;
            }
            if (phase[0] == 2) {
                vehicle.setPower(0f);
                if (d.getFrame() - phaseFrame[0] < 45) return;
                double speed = d.getVelocity().horizontalDistance();
                Mod.LOGGER.info("[drive-test] {} coast before={} after={}",
                    type, releaseVelocity[0].horizontalDistance(), speed);
                h.assertTrue(speed >= releaseVelocity[0].horizontalDistance() * .5,
                    type + " lost speed after throttle release instead of coasting: before="
                        + releaseVelocity[0].horizontalDistance() + " after=" + speed + " diagnostic=" + d);
                phaseFrame[0] = d.getFrame();
                phase[0] = 3;
                vehicle.setPower(-1f);
                return;
            }
            vehicle.setPower(-1f);
            if (d.getFrame() - phaseFrame[0] < 240) return;
            done[0] = true;
            Mod.LOGGER.info("[drive-test] {} reverse velocity={} pos={}", type, d.getVelocity(), d.getPosition());
            h.assertTrue(d.getVelocity().z < -.1, type + " reverse demand did not brake and reverse: "
                + d.getVelocity() + " diagnostic=" + d);
            var end = d.getPosition();
            h.assertTrue(end.x > x0 - 3 - 1 && end.x < x0 + 18 + 1
                    && end.z > BASE_Z - 16 - 1 && end.z < BASE_Z + 56 + 1,
                type + " drove outside the authored strip: " + end);
            h.succeed();
        });
    }

    /** Temporary diagnostic for wheel load and drive state. */
    private static void logWheels(VehicleEntity vehicle, EntityType<?> type, String stage) {
        var rs = VehiclePhysics.INSTANCE.renderState(vehicle);
        Mod.LOGGER.info("[drive-probe] {} {} wheels={} grounded={} travels={} spins={} wheelGrounded={}",
            type, stage, rs.getWheels().size(), rs.getGrounded(),
            rs.getWheels().stream().map(w -> String.format("%.3f", w.getSuspensionTravel())).toList(),
            rs.getWheels().stream().map(w -> String.format("%.3f", w.getSpin())).toList(),
            rs.getWheels().stream().map(com.atsuishio.superbwarfare.physics.WheelRenderState::getGrounded).toList());
        if (stage.equals("settle")) {
            try {
                new com.jme3.bullet.collision.shapes.CylinderCollisionShape(
                    new com.jme3.math.Vector3f(.3f, .6f, .6f), 0);
                Mod.LOGGER.info("[drive-probe] {} cylinder shape OK", type);
            } catch (Throwable t) {
                Mod.LOGGER.error("[drive-probe] {} cylinder shape FAILED: {}", type, t.toString());
            }
        }
    }

    static VehicleEntity newFixture(GameTestHelper h, EntityType<? extends VehicleEntity> type,
                                    int fixture, int x0, int faceZ, int stepHeight) {
        return newFixture(h, type, fixture, x0, faceZ, stepHeight, BASE_Z - 8.5);
    }

    static VehicleEntity newFixture(GameTestHelper h, EntityType<? extends VehicleEntity> type,
                                    int fixture, int x0, int faceZ, int stepHeight, double spawnZ) {
        return newFixture(h, type, fixture, x0, faceZ, stepHeight, spawnZ, null);
    }

    static VehicleEntity newFixture(GameTestHelper h, EntityType<? extends VehicleEntity> type,
                                    int fixture, int x0, int faceZ, int stepHeight, double spawnZ,
                                    TerrainCustomizer customizer) {
        int xz0 = BASE_Z - 16;
        int zEnd = BASE_Z + 56;
        CLEANUP.add(() -> {
            for (int cx = (x0 >> 4) - 1; cx <= (x0 >> 4) + 1; cx++)
                for (int cz = (xz0 >> 4) - 1; cz <= (zEnd >> 4) + 1; cz++)
                    h.getLevel().setChunkForced(cx, cz, false);
        });
        for (int cx = (x0 >> 4) - 1; cx <= (x0 >> 4) + 1; cx++)
            for (int cz = (xz0 >> 4) - 1; cz <= (zEnd >> 4) + 1; cz++)
                h.getLevel().setChunkForced(cx, cz, true);
        for (int dx = -3; dx <= 18; dx++) for (int dz = 0; dz <= zEnd - xz0; dz++) {
            int x = x0 + dx;
            int z = xz0 + dz;
            for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 13; y++)
                h.getLevel().setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            for (int y = FLOOR_Y - 3; y <= FLOOR_Y; y++)
                h.getLevel().setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 2);
            if (stepHeight > 0 && faceZ > 0 && z >= faceZ)
                for (int y = FLOOR_Y + 1; y <= FLOOR_Y + stepHeight; y++)
                    h.getLevel().setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 2);
        }
        if (customizer != null) customizer.apply(h, x0, faceZ);
        VehicleEntity vehicle = type.create(h.getLevel());
        h.assertTrue(vehicle != null, "Could not create " + type);
        vehicle.setPos(x0 + 8.5, FLOOR_Y + 4, spawnZ);
        vehicle.setYRot(0f);
        vehicle.yRotO = 0f;
        h.getLevel().addFreshEntity(vehicle);
        vehicle.setEnergy(100000);
        vehicle.setEngineStartOver(true);
        ArmorStand driver = EntityType.ARMOR_STAND.create(h.getLevel());
        h.assertTrue(driver != null, "Could not create the driver seat stand");
        driver.moveTo(vehicle.getX(), vehicle.getY(), vehicle.getZ(), vehicle.getYRot(), 0f);
        h.getLevel().addFreshEntity(driver);
        driver.startRiding(vehicle, true);
        CLEANUP.add(() -> {
            driver.discard();
            vehicle.discard();
        });
        h.assertTrue(vehicle.getFirstPassenger() == driver, "Non-player driver did not take the driver seat");
        return vehicle;
    }
}
