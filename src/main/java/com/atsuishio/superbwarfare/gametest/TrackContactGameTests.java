package com.atsuishio.superbwarfare.gametest;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.physics.VehiclePhysics;
import com.atsuishio.superbwarfare.physics.VehiclePhysicsDiagnostic;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Isolated, opt-in native regression suite: -PgameTestNamespaces=superbwarfare_physics. */
@GameTestHolder("superbwarfare_physics")
@PrefixGameTestTemplate(false)
public final class TrackContactGameTests {
    private TrackContactGameTests() { }
    private static final List<Runnable> CLEANUP = new ArrayList<>();

    @AfterBatch(batch = "defaultBatch")
    public static void cleanup(net.minecraft.server.level.ServerLevel level) {
        CLEANUP.forEach(Runnable::run);
        CLEANUP.clear();
    }

    @GameTest(template = "empty", timeoutTicks = 10000)
    public static void ztzFourHeadings(GameTestHelper h) { stability(h, ModEntities.ZTZ_99A.get(), 6, 0); }
    @GameTest(template = "empty", timeoutTicks = 10000)
    public static void bmpFourHeadings(GameTestHelper h) { stability(h, ModEntities.BMP_2.get(), 6, 1); }
    @GameTest(template = "empty", timeoutTicks = 10000)
    public static void bradleyFourHeadings(GameTestHelper h) { stability(h, ModEntities.BRADLEY.get(), 6, 2); }
    @GameTest(template = "empty", timeoutTicks = 10000)
    public static void abramsFourHeadings(GameTestHelper h) { stability(h, ModEntities.M_1A_2.get(), 7, 3); }
    @GameTest(template = "empty", timeoutTicks = 10000)
    public static void plzFourHeadings(GameTestHelper h) { stability(h, ModEntities.PLZ_05.get(), 6, 4); }
    @GameTest(template = "empty", timeoutTicks = 10000)
    public static void prismFourHeadings(GameTestHelper h) { stability(h, ModEntities.PRISM_TANK.get(), 5, 5); }
    @GameTest(template = "empty", timeoutTicks = 10000)
    public static void t90FourHeadings(GameTestHelper h) { stability(h, ModEntities.T_90A.get(), 6, 6); }
    @GameTest(template = "empty", timeoutTicks = 10000)
    public static void yxFourHeadings(GameTestHelper h) { stability(h, ModEntities.YX_100.get(), 7, 7); }

    private static void stability(GameTestHelper h, EntityType<? extends VehicleEntity> type, int perSide, int fixture) {
        if (VehicleDriveFixtures.skipGroup("stability")) { h.succeed(); return; }
        try {
            stabilityFixture(h, type, perSide, fixture);
        } catch (Throwable error) {
            Mod.LOGGER.error("[track-test] Could not construct fixture for " + type, error);
            throw new RuntimeException("Fixture construction failed for " + type, error);
        }
    }

    private static void stabilityFixture(GameTestHelper h, EntityType<? extends VehicleEntity> type, int perSide, int fixture) {
        List<VehicleEntity> vehicles = new ArrayList<>();
        // Remote fixtures retain surrounding natural terrain/random updates;
        // only the authored stone floor and its immediate clearance are fixed.
        int floorY = 64;
        int x = 1024 + fixture * 96;
        CLEANUP.add(() -> {
            vehicles.forEach(VehicleEntity::discard);
            for (int cx = (x - 12) >> 4; cx <= (x + 60) >> 4; cx++)
                for (int cz = 63; cz <= 64; cz++) h.getLevel().setChunkForced(cx, cz, false);
        });
        // Fixtures must not overlap the tiny "empty" template or another test.
        for (int cx = (x - 12) >> 4; cx <= (x + 60) >> 4; cx++)
            for (int cz = 63; cz <= 64; cz++) h.getLevel().setChunkForced(cx, cz, true);
        for (int heading = 0; heading < 4; heading++) {
            int px = x + heading * 16;
            for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
                // Thick slab for local-hole and complete-side excavation.
                for (int dy = -4; dy <= 0; dy++)
                    h.getLevel().setBlock(new BlockPos(px + dx, floorY + dy, 1024 + dz), Blocks.STONE.defaultBlockState(), 2);
                for (int dy = 1; dy <= 10; dy++)
                    h.getLevel().setBlock(new BlockPos(px + dx, floorY + dy, 1024 + dz), Blocks.AIR.defaultBlockState(), 2);
            }
            VehicleEntity vehicle = type.create(h.getLevel());
            h.assertTrue(vehicle != null, "Could not create " + type);
            vehicle.setPos(px + .5, floorY + 4, 1024.5);
            vehicle.setYRot(heading * 90f);
            vehicle.yRotO = vehicle.getYRot();
            h.getLevel().addFreshEntity(vehicle);
            vehicles.add(vehicle);
        }
        Vec3[] origins = new Vec3[4];
        int[] settled = {0};
        long[] last = {-1};
        long[] firstReadyFrame = {-1};
        boolean[] observing = {false};
        int[] phase = {0};
        long[] phaseFrame = {0};
        Vec3[] impulseOrigin = {Vec3.ZERO};
        double[] impulseMovement = {0};
        long[] lastEquivalentEdit = {-1};
        int[] equivalentEdits = {0};
        com.atsuishio.superbwarfare.data.vehicle.subdata.WheelSide[] unsupportedSide = {null};
        BlockPos[] dugMid = {null};
        Runnable observe = () -> {
            if (phase[0] > 0) {
                var d = VehiclePhysics.INSTANCE.diagnostic(vehicles.get(0));
                if (d == null || d.getWaitingForTerrain()) return;
                if (phase[0] == 1) {
                    impulseMovement[0] = Math.max(impulseMovement[0], d.getPosition().subtract(impulseOrigin[0]).horizontalDistance());
                    if (d.getFrame() - phaseFrame[0] < 60) return;
                    h.assertTrue(impulseMovement[0] >= .05, type + " unmanned chassis ignored external impulse");
                    // First remove a small interior patch. A continuous face
                    // must bridge this hole on its remaining real terrain.
                    var contact = d.getContacts().stream().filter(c -> c.getTouching() && c.getForce() > 0).findFirst().orElseThrow();
                    unsupportedSide[0] = contact.getSide();
                    h.assertTrue(contact.getFootprint().size() == 4, "Missing continuous side footprint");
                    Vec3 point = contact.getFootprint().stream().reduce(Vec3.ZERO, Vec3::add).scale(.25);
                    int localChanged = 0;
                    for (int dy = -4; dy <= 0; dy++)
                        if (h.getLevel().setBlock(BlockPos.containing(point.x, floorY + dy, point.z),
                            Blocks.AIR.defaultBlockState(), 2)) localChanged++;
                    Mod.LOGGER.info("[dig-probe-1] {} local hole at {} changed={}", type,
                        BlockPos.containing(point.x, floorY, point.z), localChanged);
                    phase[0] = 2; phaseFrame[0] = d.getFrame();
                } else if (phase[0] == 2) {
                    if (d.getFrame() - phaseFrame[0] < 12) return;
                    var supported = d.getContacts().stream().filter(c -> c.getSide() == unsupportedSide[0]).findFirst().orElseThrow();
                    h.assertTrue(supported.getTouching() && supported.getForce() > 0,
                        type + " local interior hole disabled the whole continuous face: " + supported);
                    h.assertTrue(d.getContacts().stream().allMatch(c -> c.getTouching() && c.getForce() > 0),
                        type + " local hole lost the opposite side");
                    // Then remove every block intersecting the selected face,
                    // plus travel/motion clearance. No centre-point substitute.
                    double minX = supported.getFootprint().stream().mapToDouble(p -> p.x).min().orElseThrow() - .2;
                    double maxX = supported.getFootprint().stream().mapToDouble(p -> p.x).max().orElseThrow() + .2;
                    double minZ = supported.getFootprint().stream().mapToDouble(p -> p.z).min().orElseThrow() - .2;
                    double maxZ = supported.getFootprint().stream().mapToDouble(p -> p.z).max().orElseThrow() + .2;
                    dugMid[0] = new BlockPos((int)Math.floor((minX + maxX) * .5), floorY,
                        (int)Math.floor((minZ + maxZ) * .5));
                    var before = h.getLevel().getBlockState(dugMid[0]);
                    int changed = 0;
                    for (int bx = (int)Math.floor(minX); bx <= (int)Math.floor(maxX); bx++)
                        for (int bz = (int)Math.floor(minZ); bz <= (int)Math.floor(maxZ); bz++)
                            for (int dy = -4; dy <= 0; dy++)
                                if (h.getLevel().setBlock(new BlockPos(bx, floorY + dy, bz),
                                    Blocks.AIR.defaultBlockState(), 2)) changed++;
                    String readiness = cn.solarmoon.spark_core.physics.SparkPhysics.get(h.getLevel())
                        .getTerrainManager().readinessIssue(new AABB(
                            Math.floor(minX), floorY - 4, Math.floor(minZ),
                            Math.floor(maxX) + 1, floorY + 1, Math.floor(maxZ) + 1));
                    Mod.LOGGER.info("[dig-probe] {} dug x={}..{} z={}..{} y={}..{} mid={} before={} state={} changed={} terrain={}",
                        type, (int)Math.floor(minX), (int)Math.floor(maxX), (int)Math.floor(minZ),
                        (int)Math.floor(maxZ), floorY - 4, floorY, dugMid[0],
                        before, h.getLevel().getBlockState(dugMid[0]), changed, readiness);
                    Mod.LOGGER.info("[dig-probe-mgr] {} mid={} mgr={}", type, dugMid[0],
                        System.identityHashCode(cn.solarmoon.spark_core.physics.SparkPhysics
                            .get(h.getLevel()).getTerrainManager()));
                    phase[0] = 3; phaseFrame[0] = d.getFrame();
                } else {
                    if (d.getFrame() - phaseFrame[0] < 12) return;
                    String lateReadiness = cn.solarmoon.spark_core.physics.SparkPhysics.get(h.getLevel())
                        .getTerrainManager().readinessIssue(new AABB(
                            dugMid[0].getX() - 2, floorY - 4, dugMid[0].getZ() - 2,
                            dugMid[0].getX() + 3, floorY + 1, dugMid[0].getZ() + 3));
                    Mod.LOGGER.info("[dig-late] {} mid={} state={} terrain={}", type, dugMid[0],
                        h.getLevel().getBlockState(dugMid[0]), lateReadiness);
                    h.assertTrue(h.getLevel().getBlockState(dugMid[0]).isAir(),
                        type + " dig never reached the world at " + dugMid[0] + ": " + h.getLevel().getBlockState(dugMid[0]));
                    var unsupported = d.getContacts().stream().filter(c -> c.getSide() == unsupportedSide[0]).findFirst().orElseThrow();
                    h.assertTrue(!unsupported.getTouching() && unsupported.getForce() == 0,
                        type + " completely unsupported face retained contact/load: " + unsupported);
                    h.assertTrue(d.getContacts().stream().anyMatch(c -> c.getSide() != unsupportedSide[0]
                        && c.getTouching() && c.getForce() > 0), type + " opposite face lost real load");
                    vehicles.forEach(VehicleEntity::discard);
                    for (int cx = (x - 12) >> 4; cx <= (x + 60) >> 4; cx++)
                        for (int cz = 63; cz <= 64; cz++) h.getLevel().setChunkForced(cx, cz, false);
                    h.succeed();
                }
                return;
            }
            boolean allSettled = true;
            for (int i = 0; i < 4; i++) {
                VehiclePhysicsDiagnostic d = VehiclePhysics.INSTANCE.diagnostic(vehicles.get(i));
                if (d == null || d.getWaitingForTerrain()) { allSettled = false; continue; }
                h.assertTrue(d.getContacts().size() == 2, "Expected exactly two continuous side suspensions: " + type);
                long roadWheels = vehicles.get(i).computed().getWheels().stream()
                    .filter(w -> w.getRole() == com.atsuishio.superbwarfare.data.vehicle.subdata.WheelRole.SUPPORT).count();
                h.assertTrue(roadWheels == perSide * 2, "Wrong authored visual road-wheel count: " + type);
                long touching = d.getContacts().stream().filter(c -> c.getTouching()).count();
                boolean landed = touching == 2 && Math.abs(d.getVelocity().y) < .02
                    && d.getPosition().y > floorY + .5 && d.getPosition().y < floorY + 1.5;
                allSettled &= landed && d.getVelocity().horizontalDistance() < .02;
                if (observing[0]) {
                    double displacement = d.getPosition().subtract(origins[i]).horizontalDistance();
                    h.assertTrue(displacement <= .05, type + " heading=" + i * 90 + " drift=" + displacement
                        + " diagnostic=" + d + " observation=" + VehiclePhysics.INSTANCE.stabilityObservation(vehicles.get(i)));
                    h.assertTrue(d.getVelocity().horizontalDistance() <= .02,
                        type + " heading=" + i * 90 + " speed=" + d.getVelocity()
                            + " diagnostic=" + d + " observation=" + VehiclePhysics.INSTANCE.stabilityObservation(vehicles.get(i)));
                    h.assertTrue(landed, type + " heading=" + i * 90
                        + " lost stable terrain contact during observation: " + d
                        + " observation=" + VehiclePhysics.INSTANCE.stabilityObservation(vehicles.get(i)));
                }
            }
            VehiclePhysicsDiagnostic first = VehiclePhysics.INSTANCE.diagnostic(vehicles.get(0));
            if (first == null || first.getFrame() == last[0]) return;
            last[0] = first.getFrame();
            if (observing[0] && first.getFrame() - lastEquivalentEdit[0] >= 30) {
                // Same section as the road wheels, but no change in collision:
                // unrelated state/material updates must not drop ready support.
                // Do not suppress random ticks or relax the observation checks.
                for (int i = 0; i < 4; i++) h.getLevel().setBlock(
                    new BlockPos(x + i * 16 + 3, floorY, 1027),
                    ((equivalentEdits[0] & 1) == 0 ? Blocks.DIRT : Blocks.STONE).defaultBlockState(), 2);
                equivalentEdits[0]++;
                lastEquivalentEdit[0] = first.getFrame();
            }
            if (!first.getWaitingForTerrain() && firstReadyFrame[0] < 0) firstReadyFrame[0] = first.getFrame();
            if (!observing[0]) {
                settled[0] = allSettled ? settled[0] + 1 : 0;
                if (settled[0] >= 20) {
                    for (int i = 0; i < 4; i++) {
                        VehiclePhysicsDiagnostic d = VehiclePhysics.INSTANCE.diagnostic(vehicles.get(i));
                        origins[i] = d.getPosition();
                        VehiclePhysics.INSTANCE.observeStability(vehicles.get(i), 600);
                        Mod.LOGGER.info("[track-test] {} heading={} start frame={} position={}", type, i * 90, d.getFrame(), d.getPosition());
                    }
                    observing[0] = true;
                    lastEquivalentEdit[0] = first.getFrame();
                }
                // The worker can lag the unthrottled GameTest tick loop. Count
                // real 1/60 s steps, never pretend MC ticks are physics frames.
                h.assertTrue(firstReadyFrame[0] < 0 || first.getFrame() - firstReadyFrame[0] < 1800,
                    type + " did not settle within 30 simulated seconds: " + vehicles.stream()
                        .map(v -> VehiclePhysics.INSTANCE.diagnostic(v)).toList());
            } else {
                for (int i = 0; i < 4; i++) {
                    var observation = VehiclePhysics.INSTANCE.stabilityObservation(vehicles.get(i));
                    if (observation == null || observation.getSamples() < 600) return;
                    h.assertTrue(observation.getMaxDisplacement() <= .05, type + " substep drift=" + observation);
                    h.assertTrue(observation.getMaxHorizontalSpeed() <= .02, type + " substep speed=" + observation);
                    h.assertTrue(observation.getMinSupportContacts() == 2, type + " missing continuous-side load=" + observation);
                    h.assertTrue(!observation.getTerrainLost(), type + " terrain lost during 600-step observation");
                    Mod.LOGGER.info("[track-test] {} heading={} stability={}", type, i * 90, observation);
                }
                h.assertTrue(equivalentEdits[0] >= 10, type + " insufficient unchanged-collision edits=" + equivalentEdits[0]);
                Mod.LOGGER.info("[track-test] {} unchanged-collision edits={}", type, equivalentEdits[0]);
                var d = VehiclePhysics.INSTANCE.diagnostic(vehicles.get(0));
                impulseOrigin[0] = d.getPosition(); phaseFrame[0] = d.getFrame(); phase[0] = 1;
                // Authored mass is tonnes, impulses use SI kg*m/s. No driver.
                VehiclePhysics.INSTANCE.impulse(vehicles.get(0), new Vec3(0, 0, vehicles.get(0).getMass() * 2000), null);
            }
        };
        h.onEachTick(() -> {
            try {
                observe.run();
            } catch (RuntimeException | AssertionError failure) {
                // Capture the live world on the game thread, before batch cleanup.
                // Never remove intrusions or change dynamics to make a flat-floor
                // assertion pass in flowing water or under falling terrain.
                for (int i = 0; i < vehicles.size(); i++) {
                    Mod.LOGGER.error("[track-test-environment] {} heading={} frame={} {}", type, i * 90,
                        VehiclePhysics.INSTANCE.diagnostic(vehicles.get(i)),
                        environment(h, vehicles.get(i), x + i * 16, floorY));
                }
                throw failure;
            }
        });
    }

    private static String environment(GameTestHelper h, VehicleEntity vehicle, int px, int floorY) {
        int fluids = 0, solids = 0;
        List<String> fluidExamples = new ArrayList<>(), solidExamples = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // The exact volume initially cleared by the fixture, not a larger
        // sanitized region. Counts include every intrusion; examples are bounded.
        for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++)
            for (int dy = 1; dy <= 10; dy++) {
                pos.set(px + dx, floorY + dy, 1024 + dz);
                var state = h.getLevel().getBlockState(pos);
                var fluid = state.getFluidState();
                if (!fluid.isEmpty()) {
                    fluids++;
                    if (fluidExamples.size() < 24) fluidExamples.add(pos.toShortString() + "=" + state
                        + " height=" + fluid.getHeight(h.getLevel(), pos) + " flow=" + fluid.getFlow(h.getLevel(), pos));
                }
                if (!state.getCollisionShape(h.getLevel(), pos).isEmpty()) {
                    solids++;
                    if (solidExamples.size() < 24) solidExamples.add(pos.toShortString() + "=" + state);
                }
            }
        var falling = h.getLevel().getEntitiesOfClass(FallingBlockEntity.class,
            new AABB(px - 7, floorY + 1, 1017, px + 8, h.getLevel().getMaxBuildHeight(), 1032));
        return "entityBounds=" + vehicle.getBoundingBox() + " inWater=" + vehicle.isInWater()
            + " fluids=" + fluids + " fluidExamples=" + fluidExamples
            + " solids=" + solids + " solidExamples=" + solidExamples
            + " fallingBlocks=" + falling.size() + " fallingExamples=" + falling.stream().limit(16)
                .map(e -> e.getBlockState() + "@" + e.position()).toList();
    }
}
