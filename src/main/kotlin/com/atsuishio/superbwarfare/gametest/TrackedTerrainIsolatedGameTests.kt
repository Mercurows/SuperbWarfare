package com.atsuishio.superbwarfare.gametest

import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate

/**
 * Focused entry points for terrain/drive regressions. Each test uses its own
 * template namespace, so `-PgameTestNamespaces=<namespace>` registers exactly
 * one original scenario in a fresh GameTest server/world.
 */
@GameTestHolder("sbw_physics_isolated")
@PrefixGameTestTemplate(false)
object TrackedTerrainIsolatedGameTests {
    @JvmStatic
    @GameTest(template = "empty", templateNamespace = "sbw_physics_forward_drive", timeoutTicks = 900)
    fun forwardDrive(helper: GameTestHelper) =
        TrackedTerrainAndDriveGameTests.contactMotionDrivesTheHullForward(helper)

    @JvmStatic
    @GameTest(template = "empty", templateNamespace = "sbw_physics_under_deck", timeoutTicks = 900)
    fun underDeckDrive(helper: GameTestHelper) =
        TrackedTerrainAndDriveGameTests.theBeltDrivesUnderABridgeDeck(helper)

    @JvmStatic
    @GameTest(template = "empty", templateNamespace = "sbw_physics_ramp_step", timeoutTicks = 1200)
    fun rampStep(helper: GameTestHelper) =
        TrackedTerrainAndDriveGameTests.aOneBlockStepClimbsOnTheBulletRamp(helper)

    @JvmStatic
    @GameTest(template = "empty", templateNamespace = "sbw_physics_right_steer", timeoutTicks = 900)
    fun rightSteer(helper: GameTestHelper) =
        TrackedTerrainAndDriveGameTests.steeringYawsTheHull(helper)

    @JvmStatic
    @GameTest(template = "empty", templateNamespace = "sbw_physics_left_steer", timeoutTicks = 900)
    fun leftSteer(helper: GameTestHelper) =
        TrackedTerrainAndDriveGameTests.aLeftSteerYawsTheOtherWay(helper)

    @JvmStatic
    @GameTest(template = "empty", templateNamespace = "sbw_physics_coast", timeoutTicks = 900)
    fun coast(helper: GameTestHelper) =
        TrackedTerrainAndDriveGameTests.coastRemovesTheConveyorTargetInsteadOfDriving(helper)
}
