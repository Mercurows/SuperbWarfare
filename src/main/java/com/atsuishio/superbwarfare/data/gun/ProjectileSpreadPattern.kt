package com.atsuishio.superbwarfare.data.gun

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ProjectileSpreadType {
    @SerialName("UniformCircle")
    UNIFORM_CIRCLE,

    @SerialName("UniformEllipse")
    UNIFORM_ELLIPSE,

    @SerialName("Grid")
    GRID,

    @SerialName("Triangle")
    TRIANGLE,

    @SerialName("InvertedTriangle")
    INVERTED_TRIANGLE,

    @SerialName("Star")
    STAR,

    @SerialName("Heart")
    HEART,

    @SerialName("XShape")
    X_SHAPE,

    @SerialName("ConcentricRings")
    CONCENTRIC_RINGS,

    @SerialName("Custom")
    CUSTOM,
}

@Serializable
data class ProjectileSpreadPattern(
    @SerialName("Type")
    val type: ProjectileSpreadType = ProjectileSpreadType.UNIFORM_CIRCLE,

    @SerialName("ScaleX")
    val scaleX: Double = 1.0,

    @SerialName("ScaleY")
    val scaleY: Double = 1.0,

    @SerialName("Rows")
    val rows: Int = 0,

    @SerialName("Columns")
    val columns: Int = 0,

    @SerialName("Rings")
    val rings: Int = 0,

    @SerialName("Jitter")
    val jitter: Double = 0.0,

    @SerialName("Points")
    val points: List<ProjectileSpreadPoint> = emptyList(),
)

@Serializable
data class ProjectileSpreadPoint(
    @SerialName("X")
    val x: Double = 0.0,

    @SerialName("Y")
    val y: Double = 0.0,
)
