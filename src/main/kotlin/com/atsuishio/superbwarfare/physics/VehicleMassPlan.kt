package com.atsuishio.superbwarfare.physics

import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleSpec
import kotlin.math.abs

/**
 * Where every kilogram of the authored vehicle mass actually goes.
 *
 * The old code subtracted a wheel share and an articulation share from the
 * authored mass and then created extra running-gear bodies on top, so a tracked
 * vehicle could carry more mass in Bullet than its data declares. This plan is
 * the single place that answers "which body owns which share", and it is built
 * from the same compiled mechanism the runtime will assemble.
 *
 * Invariant: `chassis + turret + barrel + wheels + runs == total`, within
 * [TOLERANCE_KG]. A plan that cannot satisfy it reports [problems] instead of
 * silently clamping the hull to a positive number.
 */
internal data class VehicleMassPlan(
    val totalMassKg: Float,
    val chassisMassKg: Float,
    val turretMassKg: Float,
    val barrelMassKg: Float,
    val wheelMassKg: Float,
    val runMassKg: Float,
    /** Short description of how the split was produced, for logs and audits. */
    val source: String,
    val problems: List<String> = emptyList(),
) {
    val articulationMassKg: Float get() = turretMassKg + barrelMassKg

    /** Sum of every body this plan expects to exist. */
    val allocatedMassKg: Float get() = chassisMassKg + turretMassKg + barrelMassKg + wheelMassKg + runMassKg

    fun closes(): Boolean = abs(allocatedMassKg - totalMassKg) <= TOLERANCE_KG

    val valid: Boolean get() = problems.isEmpty() && closes()

    companion object {
        /** Mass closure tolerance, kg. Float has ~7 significant digits at 55 t. */
        const val TOLERANCE_KG = 0.5f

        /**
         * Tracked split under mass option A.
         *
         * The compiled spec already separates running gear from the sprung
         * assembly, so this plan must not invent another wheel share. Only the
         * sprung assembly is divided between the hull and the constrained
         * turret/barrel bodies.
         */
        fun tracked(
            totalMassKg: Float,
            spec: TrackedVehicleSpec,
            settings: VehiclePhysicsSettings,
            anchored: Boolean,
            turret: ArticulationSpec?,
            barrel: ArticulationSpec?,
        ): VehicleMassPlan {
            val problems = ArrayList<String>()
            val wheels = spec.runningGearMassKg - spec.sides.values.sumOf { it.runMassTotalKg }
            val runs = spec.sides.values.sumOf { it.runMassTotalKg }
            val sprung = spec.sprungAssemblyMassKg

            if (!sprung.isFinite() || sprung <= 0.0) {
                problems += "compiled sprung assembly mass is not positive: $sprung"
            }
            if (abs(spec.massKg - totalMassKg.toDouble()) > 1.0) {
                problems += "authored mass ${totalMassKg} kg differs from the tracked definition's " +
                    "${"%.1f".format(spec.massKg)} kg"
            }

            val wantsArticulation = !anchored && settings.articulationEnabled &&
                turret != null && turret.parts.isNotEmpty()
            val wantsBarrel = wantsArticulation && barrel != null && barrel.parts.isNotEmpty()
            val share = if (wantsArticulation) {
                (sprung * settings.articulationMassFraction.coerceIn(0f, .2f)).toFloat()
            } else {
                0f
            }
            val turretMass = if (wantsArticulation) (share * .7f).coerceAtLeast(.1f) else 0f
            val barrelMass = if (wantsBarrel) (share * .3f).coerceAtLeast(.1f) else 0f
            val chassis = (sprung - turretMass - barrelMass).toFloat()

            if (chassis <= 0f) {
                problems += "sprung assembly ${"%.1f".format(sprung)} kg cannot cover " +
                    "articulation ${"%.1f".format(turretMass + barrelMass)} kg"
            }

            return VehicleMassPlan(
                totalMassKg = totalMassKg,
                chassisMassKg = chassis.coerceAtLeast(.1f),
                turretMassKg = turretMass,
                barrelMassKg = barrelMass,
                wheelMassKg = wheels.toFloat(),
                runMassKg = runs.toFloat(),
                source = "tracked/option-A: compiled running gear ${"%.1f".format(wheels + runs)} kg, " +
                    "sprung ${"%.1f".format(sprung)} kg",
                problems = problems,
            )
        }

        /**
         * Wheeled split: unchanged behaviour, but expressed in one place.
         *
         * Wheel bodies take [VehiclePhysicsSettings.wheelMassFraction] of the
         * authored mass and the turret/barrel take the articulation share; the
         * hull keeps the remainder. A tracked vehicle must never use this path.
         */
        fun wheeled(
            totalMassKg: Float,
            settings: VehiclePhysicsSettings,
            anchored: Boolean,
            hasWheelStations: Boolean,
            turret: ArticulationSpec?,
            barrel: ArticulationSpec?,
        ): VehicleMassPlan {
            val wheelMass = if (anchored || !hasWheelStations) 0f
            else (totalMassKg * settings.wheelMassFraction.coerceIn(0f, .35f))
                .coerceIn(0f, totalMassKg * .35f)

            val wantsArticulation = !anchored && settings.articulationEnabled &&
                turret != null && turret.parts.isNotEmpty()
            val wantsBarrel = wantsArticulation && barrel != null && barrel.parts.isNotEmpty()
            val share = if (wantsArticulation) {
                (totalMassKg * settings.articulationMassFraction.coerceIn(0f, .2f))
                    .coerceIn(0f, totalMassKg * .2f)
            } else {
                0f
            }
            val turretMass = if (wantsArticulation) (share * .7f).coerceAtLeast(.1f) else 0f
            val barrelMass = if (wantsBarrel) (share * .3f).coerceAtLeast(.1f) else 0f

            val problems = ArrayList<String>()
            val chassis = totalMassKg - wheelMass - turretMass - barrelMass
            if (chassis <= 0f) problems += "authored mass is smaller than the reserved wheel/articulation share"

            return VehicleMassPlan(
                totalMassKg = totalMassKg,
                chassisMassKg = chassis.coerceAtLeast(.1f),
                turretMassKg = turretMass,
                barrelMassKg = barrelMass,
                wheelMassKg = wheelMass,
                runMassKg = 0f,
                source = "wheeled: wheel fraction ${settings.wheelMassFraction}, " +
                    "articulation fraction ${settings.articulationMassFraction}",
                problems = problems,
            )
        }
    }
}
