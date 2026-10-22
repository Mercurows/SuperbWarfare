package com.atsuishio.superbwarfare.physics.tracked

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Contract of the neutral Bedrock reader.
 *
 * The physics layer only needs pivots and cube extents, but it needs them to be
 * exact: a wrong basis or a missed parent turns every wheel body into a guess.
 */
class BedrockModelGeometryTest {

    private fun geometry(bones: String) = """
        {"format_version":"1.12.0","minecraft:geometry":[{"description":{},"bones":[$bones]}]}
    """.trimIndent()

    @Test
    fun aChildPivotIsComposedThroughTheParentChain() {
        val model = BedrockModelGeometry.parse(
            geometry(
                """
                {"name":"root","pivot":[0,10,0],"rotation":[0,90,0]},
                {"name":"child","parent":"root","pivot":[16,10,0]}
                """,
            ),
        )
        // Rotating the parent by +90 degrees about Y maps +X onto -Z.
        val pivot = model.worldPivot("child")
        assertEquals(0.0, pivot.x, 1e-9)
        assertEquals(10.0, pivot.y, 1e-9)
        assertEquals(-16.0, pivot.z, 1e-9)
    }

    @Test
    fun thePhysicsBasisFlipsZAndScalesPixels() {
        val model = BedrockModelGeometry.parse(geometry("""{"name":"root","pivot":[0,0,0]}"""))
        val local = model.toPhysicsLocal(org.joml.Vector3d(16.0, 32.0, -48.0), 1.0)
        assertEquals(1.0, local.x, 1e-12)
        assertEquals(2.0, local.y, 1e-12)
        assertEquals(3.0, local.z, 1e-12)
    }

    @Test
    fun aMissingParentIsRejected() {
        val failure = assertThrows(TrackedGeometryException::class.java) {
            BedrockModelGeometry.parse(geometry("""{"name":"child","parent":"ghost","pivot":[0,0,0]}"""))
        }
        assertTrue(failure.message!!.contains("ghost"))
    }

    @Test
    fun aBoneCycleIsRejectedInsteadOfRecursingForever() {
        val failure = assertThrows(TrackedGeometryException::class.java) {
            BedrockModelGeometry.parse(
                geometry(
                    """
                    {"name":"a","parent":"b","pivot":[0,0,0]},
                    {"name":"b","parent":"a","pivot":[0,0,0]}
                    """,
                ),
            )
        }
        assertTrue(failure.message!!.contains("cycle"))
    }

    @Test
    fun nonFiniteGeometryIsRejected() {
        assertThrows(TrackedGeometryException::class.java) {
            BedrockModelGeometry.parse(geometry("""{"name":"root","pivot":[0,0,0],"cubes":[{"origin":[0,0,0],"size":[1,1,1],"inflate":"NaN"}]}"""))
        }
    }

    @Test
    fun subtreeIncludesDescendantsInDeterministicOrder() {
        val model = BedrockModelGeometry.parse(
            geometry(
                """
                {"name":"root","pivot":[0,0,0]},
                {"name":"b","parent":"root","pivot":[0,0,0]},
                {"name":"a","parent":"root","pivot":[0,0,0]},
                {"name":"c","parent":"b","pivot":[0,0,0]}
                """,
            ),
        )
        assertEquals(listOf("root", "a", "b", "c"), model.subtree("root"))
    }
}
