package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.data.gun.ProjectileSpreadPattern
import com.atsuishio.superbwarfare.data.gun.ProjectileSpreadType
import net.minecraft.util.RandomSource
import net.minecraft.world.phys.Vec3
import kotlin.math.*

object ProjectileSpreadTool {
    private val WORLD_UP = Vec3(0.0, 1.0, 0.0)
    private const val INNER_STAR_RADIUS = 0.382
    private const val EPSILON = 1.0E-6

    @JvmStatic
    @JvmOverloads
    fun generateDirections(
        rng: RandomSource,
        direction: Vec3,
        spread: Double,
        amount: Int,
        pattern: ProjectileSpreadPattern?,
        rotationDegrees: Double = 0.0
    ): List<Vec3> {
        if (amount <= 0) return emptyList()

        val forward = direction.normalize()
        if (spread <= 0.0) return List(amount) { forward }

        val safeSpread = spread.coerceAtMost(85.0)
        val maxTangent = tan(Math.toRadians(safeSpread))
        if (maxTangent <= EPSILON) return List(amount) { forward }

        val upCandidate = WORLD_UP.subtract(forward.scale(forward.dot(WORLD_UP)))
        val up = if (upCandidate.lengthSqr() < EPSILON) {
            Vec3(0.0, 0.0, 1.0)
        } else {
            upCandidate.normalize()
        }
        val right = forward.cross(up).normalize()

        val effectivePattern = pattern ?: ProjectileSpreadPattern()
        val rotation = Math.toRadians(((rotationDegrees % 360.0) + 360.0) % 360.0)
        val cosRotation = cos(rotation)
        val sinRotation = sin(rotation)
        return generateOffsets(rng, effectivePattern, amount).map { offset ->
            val x = offset.x * cosRotation - offset.y * sinRotation
            val y = offset.x * sinRotation + offset.y * cosRotation
            val point = forward
                .add(right.scale(maxTangent * x))
                .add(up.scale(maxTangent * y))
            point.normalize()
        }
    }

    private fun generateOffsets(
        rng: RandomSource,
        pattern: ProjectileSpreadPattern,
        amount: Int
    ): List<Vec3> {
        return when (pattern.type) {
            ProjectileSpreadType.UNIFORM_CIRCLE,
            ProjectileSpreadType.UNIFORM_ELLIPSE -> List(amount) { randomEllipseOffset(rng, pattern) }

            ProjectileSpreadType.GRID -> gridOffsets(rng, pattern, amount)
            ProjectileSpreadType.TRIANGLE -> triangleOffsets(rng, pattern, amount)
            ProjectileSpreadType.STAR -> starOffsets(rng, pattern, amount)
            ProjectileSpreadType.HEART -> heartOffsets(rng, pattern, amount)
            ProjectileSpreadType.X_SHAPE -> xShapeOffsets(rng, pattern, amount)
            ProjectileSpreadType.CONCENTRIC_RINGS -> concentricRingOffsets(rng, pattern, amount)
            ProjectileSpreadType.CUSTOM -> customOffsets(rng, pattern, amount)
        }
    }

    private fun randomEllipseOffset(
        rng: RandomSource,
        pattern: ProjectileSpreadPattern
    ): Vec3 {
        val theta = rng.nextDouble() * PI * 2.0
        val radius = sqrt(rng.nextDouble())
        return Vec3(
            radius * cos(theta) * scaleX(pattern),
            radius * sin(theta) * scaleY(pattern),
            0.0
        )
    }

    private fun gridOffsets(
        rng: RandomSource,
        pattern: ProjectileSpreadPattern,
        amount: Int
    ): List<Vec3> {
        val rows = max(1, pattern.rows)
        val columns = if (pattern.columns > 0) {
            pattern.columns
        } else {
            max(1, (amount + rows - 1) / rows)
        }

        val offsets = ArrayList<Vec3>(amount)
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                if (offsets.size >= amount) return offsets

                val sx = scaleX(pattern)
                val sy = scaleY(pattern)
                val x = if (columns == 1) {
                    0.0
                } else {
                    -sx + 2.0 * sx * column / (columns - 1)
                }
                val y = if (rows == 1) {
                    0.0
                } else {
                    sy - 2.0 * sy * row / (rows - 1)
                }
                offsets += jitteredOffset(rng, pattern, x, y)
            }
        }

        while (offsets.size < amount) {
            offsets += randomEllipseOffset(rng, pattern)
        }
        return offsets
    }

    private fun triangleOffsets(
        rng: RandomSource,
        pattern: ProjectileSpreadPattern,
        amount: Int
    ): List<Vec3> {
        val rows = if (pattern.rows > 0) pattern.rows else triangleRowsFor(amount)
        val offsets = ArrayList<Vec3>(amount)
        val sx = scaleX(pattern)
        val sy = scaleY(pattern)
        val xStep = if (rows <= 1) 0.0 else 2.0 * sx / (rows - 1)
        val height = sqrt(3.0) * sx * sy
        val yStep = if (rows <= 1) 0.0 else height / (rows - 1)
        val startY = -height / 2.0

        for (layer in 0 until rows) {
            if (offsets.size >= amount) return offsets

            val rowLength = rows - layer
            val remaining = amount - offsets.size
            val take = minOf(rowLength, remaining)
            val startX = -xStep * (take - 1) / 2.0
            val y = startY + yStep * layer

            for (index in 0 until take) {
                offsets += jitteredOffset(rng, pattern, startX + xStep * index, y)
            }
        }

        while (offsets.size < amount) {
            offsets += randomEllipseOffset(rng, pattern)
        }
        return offsets
    }

    private fun starOffsets(
        rng: RandomSource,
        pattern: ProjectileSpreadPattern,
        amount: Int
    ): List<Vec3> {
        if (amount <= 0) return emptyList()

        val vertices = List(10) { index ->
            val angle = -PI / 2.0 + index * PI * 2.0 / 10.0
            val radius = if (index % 2 == 0) 1.0 else INNER_STAR_RADIUS
            Vec3(cos(angle) * radius, sin(angle) * radius, 0.0)
        }

        val offsets = ArrayList<Vec3>(amount)
        for (index in 0 until amount) {
            val position = index.toDouble() / amount * vertices.size
            val segment = minOf(vertices.lastIndex, floor(position).toInt())
            val t = position - segment
            val from = vertices[segment]
            val to = vertices[(segment + 1) % vertices.size]
            offsets += jitteredOffset(
                rng,
                pattern,
                from.x + (to.x - from.x) * t,
                from.y + (to.y - from.y) * t
            )
        }
        return offsets
    }

    private fun heartOffsets(
        rng: RandomSource,
        pattern: ProjectileSpreadPattern,
        amount: Int
    ): List<Vec3> {
        if (amount <= 0) return emptyList()

        val offsets = ArrayList<Vec3>(amount)
        for (index in 0 until amount) {
            val t = index.toDouble() / amount * PI * 2.0
            val sinT = sin(t)
            val rawX = 16.0 * sinT * sinT * sinT
            val rawY = 13.0 * cos(t) - 5.0 * cos(2.0 * t) - 2.0 * cos(3.0 * t) - cos(4.0 * t)
            offsets += jitteredOffset(
                rng,
                pattern,
                rawX / 16.0,
                (rawY + 6.0) / 16.0
            )
        }
        return offsets
    }

    private fun xShapeOffsets(
        rng: RandomSource,
        pattern: ProjectileSpreadPattern,
        amount: Int
    ): List<Vec3> {
        if (amount <= 0) return emptyList()

        val offsets = ArrayList<Vec3>(amount)
        offsets += jitteredOffset(rng, pattern, 0.0, 0.0)

        val remaining = amount - 1
        val armLengths = IntArray(4) { remaining / 4 }
        repeat(remaining % 4) { armLengths[it]++ }

        val signs = listOf(
            1.0 to 1.0,
            1.0 to -1.0,
            -1.0 to -1.0,
            -1.0 to 1.0
        )
        for (arm in 0 until 4) {
            for (step in 1..armLengths[arm]) {
                val t = step.toDouble() / armLengths[arm]
                offsets += jitteredOffset(
                    rng,
                    pattern,
                    signs[arm].first * t,
                    signs[arm].second * t
                )
            }
        }
        return offsets
    }

    private fun concentricRingOffsets(
        rng: RandomSource,
        pattern: ProjectileSpreadPattern,
        amount: Int
    ): List<Vec3> {
        if (amount <= 0) return emptyList()

        val requestedRings = if (pattern.rings > 0) pattern.rings else 1
        val ringCount = minOf(max(1, requestedRings), amount)
        val ringCounts = distributeEvenly(amount, ringCount)
        val offsets = ArrayList<Vec3>(amount)

        for (ring in 0 until ringCount) {
            val count = ringCounts[ring]
            if (count <= 0) continue

            val radius = (ring + 1).toDouble() / ringCount
            val phase = if (count <= 1) 0.0 else ring * PI / count
            for (index in 0 until count) {
                val angle = phase + index.toDouble() / count * PI * 2.0
                offsets += jitteredOffset(
                    rng,
                    pattern,
                    radius * cos(angle),
                    radius * sin(angle)
                )
            }
        }
        return offsets
    }

    private fun distributeEvenly(total: Int, buckets: Int): IntArray {
        val base = total / buckets
        val extra = total % buckets
        return IntArray(buckets) { index -> base + if (index < extra) 1 else 0 }
    }

    private fun customOffsets(
        rng: RandomSource,
        pattern: ProjectileSpreadPattern,
        amount: Int
    ): List<Vec3> {
        val offsets = ArrayList<Vec3>(amount)
        val points = pattern.points

        for (index in 0 until amount) {
            if (index < points.size) {
                offsets += jitteredOffset(
                    rng,
                    pattern,
                    points[index].x * scaleX(pattern),
                    points[index].y * scaleY(pattern)
                )
            } else {
                offsets += randomEllipseOffset(rng, pattern)
            }
        }
        return offsets
    }

    private fun jitteredOffset(
        rng: RandomSource,
        pattern: ProjectileSpreadPattern,
        x: Double,
        y: Double
    ): Vec3 {
        val jitter = max(0.0, pattern.jitter)
        if (jitter <= 0.0) return Vec3(x, y, 0.0)

        return Vec3(
            x + rng.triangle(0.0, jitter),
            y + rng.triangle(0.0, jitter),
            0.0
        )
    }

    private fun scaleX(pattern: ProjectileSpreadPattern): Double = max(0.0, pattern.scaleX)

    private fun scaleY(pattern: ProjectileSpreadPattern): Double = max(0.0, pattern.scaleY)

    private fun triangleRowsFor(amount: Int): Int {
        var rows = 1
        while (rows * (rows + 1) / 2 < amount) {
            rows++
        }
        return rows
    }
}
