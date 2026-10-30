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

    /**
     * S2 split: the vehicle that owns a compiled running gear enters the physics
     * path, and every other tracked vehicle stays out.
     *
     * This replaces the D1 blanket assertion. Turning 99A on must not quietly
     * drag BMP-2 or T-90A in with it, because neither has a compiled tracked
     * definition yet.
     */
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void onlyTheVehicleWithACompiledTrackedRuntimeEntersPhysics(GameTestHelper helper) {
        // Centres at x = 0, 8 and 16. empty.nbt spaces parallel tests by about
        // 36 blocks, so a floor out to x=144 would land in the next cell.
        BlockPos floor = helper.absolutePos(BlockPos.ZERO);
        for (int x = -4; x <= 22; x++) for (int z = -6; z <= 6; z++)
            helper.getLevel().setBlock(floor.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3);
        VehicleEntity wired = spawn(helper, ModEntities.ZTZ_99A.get(), 1, 0);
        List<VehicleEntity> parked = new ArrayList<>();
        parked.add(spawn(helper, ModEntities.BMP_2.get(), 0, 0));
        parked.add(spawn(helper, ModEntities.T_90A.get(), 2, 0));
        helper.startSequence().thenIdle(40).thenExecute(() -> {
            helper.assertTrue(VehiclePhysics.INSTANCE.isManaged(wired),
                "ZTZ-99A is registered as TRACK_RUN and must enter the physics path");
            helper.assertTrue(VehiclePhysics.INSTANCE.hasBody(wired),
                "ZTZ-99A must receive a Spark chassis body");
            for (VehicleEntity vehicle : parked) {
                helper.assertFalse(VehiclePhysics.INSTANCE.isManaged(vehicle),
                    "Tracked vehicle without a compiled definition must stay outside physics: " + vehicle.getType());
                helper.assertFalse(VehiclePhysics.INSTANCE.hasBody(vehicle),
                    "Tracked vehicle without a compiled definition must not receive a Spark body: " + vehicle.getType());
            }
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
