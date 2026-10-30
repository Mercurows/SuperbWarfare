package com.atsuishio.superbwarfare.physics.tracked

import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TrackedTerrainSamplerTest {
    @Test
    fun yawRotatesAndNeverShrinksTheRunningGearWindow() {
        val envelope = TerrainEnvelope(-3.0, 3.0, -1.0, 1.0, -1.0, 1.0)
        val terrain = CompiledTerrain(false, 1.0, 1.0, 10, 3, 4)
        val straight = TrackedTerrainSampler.window(Vec3.ZERO, envelope, terrain)
        val shifted = TrackedTerrainSampler.window(Vec3(0.1, 0.0, -0.1), envelope, terrain)
        assertTrue(straight.minX <= envelope.minX && straight.maxX >= envelope.maxX)
        assertTrue(straight.minZ <= envelope.minZ && straight.maxZ >= envelope.maxZ)
        assertEquals(straight, shifted, "small movements inside the snap cell keep the same mesh window")
    }

    @Test
    fun anEnvelopeThatExceedsWindowRadiusIsRejectedInsteadOfClipped() {
        val envelope = TerrainEnvelope(-7.0, 7.0, -1.0, 1.0, -1.0, 1.0)
        val terrain = CompiledTerrain(false, 1.0, 1.0, 6, 3, 4)
        val error = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            TrackedTerrainSampler.window(Vec3.ZERO, envelope, terrain)
        }
        assertTrue(error.message.orEmpty().contains("WindowRadius"))
    }

    @Test
    fun snappedWindowContainsTheWholeRunningGearAcrossItsSnapCell() {
        val envelope = TerrainEnvelope(-3.1, 3.1, -1.2, 1.2, -5.0, 5.0)
        val terrain = CompiledTerrain(false, 1.0, 1.0, 10, 3, 8)
        val window = TrackedTerrainSampler.window(Vec3.ZERO, envelope, terrain)
        for (x in listOf(-0.49, 0.49)) for (z in listOf(-0.49, 0.49)) {
            assertTrue(window.minX <= x - 3.1 && window.maxX >= x + 3.1)
            assertTrue(window.minZ <= z - 5.0 && window.maxZ >= z + 5.0)
            assertTrue(window.minY <= envelope.minY - 0.49 && window.maxY >= envelope.maxY + 0.49)
        }
    }
}
