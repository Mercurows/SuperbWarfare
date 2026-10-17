package com.atsuishio.superbwarfare.gametest;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.TurretWreckEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.physics.VehiclePhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Acceptance tests for the server-authoritative Spark/Bullet physics of the
 * conventional ground roster. Special engines, aircraft, ships, TOM6, FIXED
 * and WHEELCHAIR vehicles are intentionally outside the physics layer and are
 * verified to stay outside it rather than falling back to legacy movement.
 */
@GameTestHolder(Mod.MODID)
@PrefixGameTestTemplate(false)
public final class VehiclePhysicsGameTests {
    private VehiclePhysicsGameTests() { }

    private static <T extends Entity> T spawn(GameTestHelper helper, EntityType<T> type) {
        return spawn(helper, type, 0, 0);
    }

    private static <T extends Entity> T spawn(GameTestHelper helper, EntityType<T> type, int offsetX, int offsetZ) {
        T entity = type.create(helper.getLevel());
        helper.assertTrue(entity != null, "Vehicle entity could not be created");
        BlockPos floor = helper.absolutePos(BlockPos.ZERO);
        entity.setPos(floor.getX() + .5 + offsetX * 8.0, floor.getY() + 4, floor.getZ() + .5 + offsetZ * 8.0);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static double terrainTop(GameTestHelper helper) {
        BlockPos anchor = helper.absolutePos(BlockPos.ZERO);
        int top = Integer.MIN_VALUE;
        for (int x = anchor.getX() - 2; x <= anchor.getX() + 2; x++)
            for (int z = anchor.getZ() - 2; z <= anchor.getZ() + 2; z++)
                for (int y = anchor.getY() - 4; y <= anchor.getY() + 8; y++) {
                    var state = helper.getLevel().getBlockState(new BlockPos(x, y, z));
                    if (!state.isAir() && !state.is(Blocks.STRUCTURE_BLOCK)) top = Math.max(top, y + 1);
                }
        return top == Integer.MIN_VALUE ? anchor.getY() + 1.0 : top;
    }

    private static void prepareFlatFloor(GameTestHelper helper, int radius) {
        BlockPos anchor = helper.absolutePos(BlockPos.ZERO);
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            helper.getLevel().setBlock(anchor.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3);
            helper.getLevel().setBlock(anchor.offset(x, 1, z), Blocks.AIR.defaultBlockState(), 3);
            helper.getLevel().setBlock(anchor.offset(x, 2, z), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static void assertLanded(GameTestHelper helper, VehicleEntity vehicle, String label) {
        double floorTop = terrainTop(helper);
        double minY = vehicle.getBoundingBox().minY;
        helper.assertTrue(minY > floorTop - .25 && minY < floorTop + 4.0,
            label + " did not settle on the terrain: y=" + vehicle.getY() + ", minY=" + minY + ", floorTop=" + floorTop);
    }

    private static void mountRider(GameTestHelper helper, VehicleEntity vehicle) {
        Pig rider = EntityType.PIG.create(helper.getLevel());
        helper.assertTrue(rider != null, "Test rider could not be created");
        rider.setPos(vehicle.getX(), vehicle.getY(), vehicle.getZ());
        helper.getLevel().addFreshEntity(rider);
        helper.assertTrue(rider.startRiding(vehicle, true), "Test rider could not mount vehicle");
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void everyGroundVehicleGetsReadyBody(GameTestHelper helper) {
        BlockPos floor = helper.absolutePos(BlockPos.ZERO);
        for (int x = -16; x <= 144; x++) for (int z = -8; z <= 8; z++)
            helper.getLevel().setBlock(floor.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3);
        List<VehicleEntity> vehicles = new ArrayList<>();
        vehicles.add(spawn(helper, ModEntities.LAV_150.get(), 0, 0));
        vehicles.add(spawn(helper, ModEntities.LAV_25.get(), 1, 0));
        vehicles.add(spawn(helper, ModEntities.BMP_2.get(), 2, 0));
        vehicles.add(spawn(helper, ModEntities.BRADLEY.get(), 3, 0));
        vehicles.add(spawn(helper, ModEntities.ZTZ_99A.get(), 4, 0));
        vehicles.add(spawn(helper, ModEntities.T_90A.get(), 5, 0));
        vehicles.add(spawn(helper, ModEntities.M_1A_2.get(), 6, 0));
        vehicles.add(spawn(helper, ModEntities.YX_100.get(), 7, 0));
        vehicles.add(spawn(helper, ModEntities.PRISM_TANK.get(), 8, 0));
        vehicles.add(spawn(helper, ModEntities.PLZ_05.get(), 9, 0));
        vehicles.add(spawn(helper, ModEntities.TRUCK.get(), 10, 0));
        vehicles.add(spawn(helper, ModEntities.LAV_AD.get(), 11, 0));
        vehicles.add(spawn(helper, ModEntities.FH_77BW.get(), 12, 0));
        vehicles.add(spawn(helper, ModEntities.SODAYO_PICK_UP.get(), 13, 0));
        vehicles.add(spawn(helper, ModEntities.SODAYO_PICK_UP_HMG.get(), 14, 0));
        vehicles.add(spawn(helper, ModEntities.SODAYO_PICK_UP_ROCKET.get(), 15, 0));
        vehicles.add(spawn(helper, ModEntities.SODAYO_PICK_UP_TOW.get(), 16, 0));
        helper.startSequence().thenWaitUntil(() -> {
            for (VehicleEntity vehicle : vehicles) {
                helper.assertTrue(VehiclePhysics.INSTANCE.hasBody(vehicle),
                    "Vehicle did not become READY: " + vehicle.getType());
            }
        }).thenWaitUntil(() -> {
            for (VehicleEntity vehicle : vehicles) {
                assertLanded(helper, vehicle, vehicle.getType().toString());
            }
        }).thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 320)
    public static void wheeledVehicleFallsOntoTerrain(GameTestHelper helper) {
        prepareFlatFloor(helper, 5);
        VehicleEntity vehicle = spawn(helper, ModEntities.SODAYO_PICK_UP.get());
        double startY = vehicle.getY();
        double floorTop = terrainTop(helper);
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(VehiclePhysics.INSTANCE.hasBody(vehicle), "Wheeled vehicle has no Spark body");
            helper.assertTrue(vehicle.getY() < floorTop + .25, "Gravity did not move the wheeled vehicle: y="
                + vehicle.getY() + ", floorTop=" + floorTop + ", start=" + startY);
            helper.assertTrue(vehicle.getY() > floorTop - .2, "Wheeled vehicle passed through terrain");
        }).thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 320)
    public static void trackedVehicleFallsOntoTerrain(GameTestHelper helper) {
        prepareFlatFloor(helper, 5);
        VehicleEntity vehicle = spawn(helper, ModEntities.BMP_2.get());
        double startY = vehicle.getY();
        double floorTop = terrainTop(helper);
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(VehiclePhysics.INSTANCE.hasBody(vehicle), "Tracked vehicle has no Spark body");
            helper.assertTrue(vehicle.getY() < floorTop + .25, "Gravity did not move the tracked vehicle: y="
                + vehicle.getY() + ", floorTop=" + floorTop + ", start=" + startY);
            helper.assertTrue(vehicle.getY() > floorTop - .2, "Tracked vehicle passed through terrain");
        }).thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void groundVehicleSettlesWithoutBounceOrSpin(GameTestHelper helper) {
        prepareFlatFloor(helper, 6);
        VehicleEntity vehicle = spawn(helper, ModEntities.ZTZ_99A.get());
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(VehiclePhysics.INSTANCE.hasBody(vehicle), "Tracked vehicle has no Spark body");
            helper.assertTrue(vehicle.onGround(), "Vehicle is still falling, not settled");
            helper.assertTrue(vehicle.getY() < terrainTop(helper) + .5, "Vehicle did not reach terrain");
            helper.assertTrue(Math.abs(vehicle.getDeltaMovement().y) < .15,
                "Suspension did not settle vertically: " + vehicle.getDeltaMovement().y);
            helper.assertTrue(Math.abs(vehicle.getRoll()) < 5f,
                "Vehicle acquired a persistent roll: " + vehicle.getRoll());
        }).thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 320)
    public static void turretWreckFallsOntoTerrain(GameTestHelper helper) {
        prepareFlatFloor(helper, 5);
        TurretWreckEntity wreck = spawn(helper, ModEntities.TURRET_WRECK.get());
        double startY = wreck.getY();
        double floorTop = terrainTop(helper);
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(VehiclePhysics.INSTANCE.hasBody(wreck), "Turret wreck has no Spark body");
            helper.assertTrue(wreck.getY() < floorTop + .25, "Gravity did not move the turret wreck: y="
                + wreck.getY() + ", floorTop=" + floorTop + ", start=" + startY);
            helper.assertTrue(wreck.getY() > floorTop - .2, "Turret wreck passed through terrain");
        }).thenSucceed();
    }

    @GameTest(template = "empty", batch = "vehicle_drive", timeoutTicks = 400)
    public static void inputAcceleratesWheeledVehicle(GameTestHelper helper) {
        BlockPos floor = helper.absolutePos(BlockPos.ZERO);
        prepareFlatFloor(helper, 6);
        VehicleEntity vehicle = spawn(helper, ModEntities.SODAYO_PICK_UP.get());
        vehicle.setEnergy(vehicle.getMaxEnergy());
        vehicle.setPos(floor.getX() + .5, terrainTop(helper) + .5, floor.getZ() + .5);
        mountRider(helper, vehicle);
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(vehicle.onGround(), "Wheeled vehicle was not grounded before acceleration: y="
                + vehicle.getY() + ", vy=" + vehicle.getDeltaMovement().y);
            vehicle.processInput((short) 0b00000100);
        }).thenWaitUntil(() -> {
            double horizontal = Math.hypot(vehicle.getX() - floor.getX() - .5, vehicle.getZ() - floor.getZ() - .5);
            helper.assertTrue(horizontal > .5, "Input did not accelerate the wheeled Spark rigid body: " + horizontal);
        }).thenSucceed();
    }

    @GameTest(template = "empty", batch = "vehicle_drive", timeoutTicks = 400)
    public static void inputAcceleratesTrackedVehicle(GameTestHelper helper) {
        BlockPos floor = helper.absolutePos(BlockPos.ZERO);
        prepareFlatFloor(helper, 6);
        VehicleEntity vehicle = spawn(helper, ModEntities.BMP_2.get());
        vehicle.setEnergy(vehicle.getMaxEnergy());
        vehicle.setPos(floor.getX() + .5, terrainTop(helper) + .5, floor.getZ() + .5);
        mountRider(helper, vehicle);
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(vehicle.onGround(), "Tracked vehicle was not grounded before acceleration: y="
                + vehicle.getY() + ", vy=" + vehicle.getDeltaMovement().y);
            vehicle.processInput((short) 0b00000100);
        }).thenWaitUntil(() -> {
            double horizontal = Math.hypot(vehicle.getX() - floor.getX() - .5, vehicle.getZ() - floor.getZ() - .5);
            helper.assertTrue(horizontal > .5, "Input did not accelerate the tracked Spark rigid body: " + horizontal);
        }).thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void outOfScopeVehicleStaysOutsidePhysics(GameTestHelper helper) {
        prepareFlatFloor(helper, 5);
        VehicleEntity vehicle = spawn(helper, ModEntities.WHEEL_CHAIR.get());
        helper.startSequence().thenIdle(20).thenExecute(() -> {
            helper.assertFalse(VehiclePhysics.INSTANCE.isManaged(vehicle),
                "Wheelchair must not be claimed by the conventional ground physics layer");
            helper.assertFalse(VehiclePhysics.INSTANCE.hasBody(vehicle),
                "Wheelchair must not receive a Spark body");
        }).thenSucceed();
    }
}
