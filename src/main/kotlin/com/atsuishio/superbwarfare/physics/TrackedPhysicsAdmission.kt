package com.atsuishio.superbwarfare.physics

import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleSpec

/**
 * Server admission for a tracked vehicle.
 *
 * A registered id is not enough. The compiled running gear and the mass plan
 * must both be usable, or the vehicle stays on its previous movement path
 * instead of receiving a chassis-only Bullet body.
 */
internal object TrackedPhysicsAdmission {
    fun rejectReason(spec: TrackedVehicleSpec?, plan: VehicleMassPlan?): String? {
        if (spec == null) return "missing compiled running gear"
        if (plan == null) return "missing mass plan"
        if (plan.valid) return null
        return plan.problems.joinToString("; ").ifEmpty {
            "mass plan does not close: allocated ${plan.allocatedMassKg} kg, authored ${plan.totalMassKg} kg"
        }
    }
}
