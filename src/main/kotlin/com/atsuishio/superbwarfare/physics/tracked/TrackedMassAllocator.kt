package com.atsuishio.superbwarfare.physics.tracked

/**
 * Splits one side's belt budget across its runs.
 *
 * This is SBW's own allocation policy for mass option A, not a recovery of
 * MCSP's `0.6 m` / `0.2 (m_i + m_next)` split. Under option A the authored wheel
 * mass stays on the wheel body and the old shoe budget becomes the run budget,
 * so the vehicle total does not move and the topology change can be measured on
 * its own.
 *
 * The share is proportional to the combined mass of each pair's two wheels. That
 * keeps a heavy pair heavier than a light one, conserves the budget exactly, and
 * does not depend on how many shoes the legacy chain happened to solve.
 *
 * [POLICY_ID] is written into the audit trail. Changing the formula is a new
 * policy with a new id, so an old result can never be silently reinterpreted.
 */
object TrackedMassAllocator {

    const val POLICY_ID = "sbw-adjacent-pair-mass-share-v1"

    /**
     * One mass per member, in traversal order.
     *
     * [members] must be in traversal order and every member's wheel must be in
     * [wheels]. The sum of the result equals [sideBudgetKg] to within floating
     * point error; callers that need exact conservation must check the sum
     * themselves rather than assume it.
     */
    fun allocateRuns(
        members: List<CompiledBeltMember>,
        wheels: Map<Int, CompiledWheel>,
        sideBudgetKg: Double,
    ): List<Double> {
        require(members.size >= 2) { "a belt needs at least two members, had ${members.size}" }
        require(sideBudgetKg > 0.0 && sideBudgetKg.isFinite()) {
            "run budget must be positive and finite, was $sideBudgetKg"
        }
        val pairMass = members.indices.map { i ->
            val first = wheels[members[i].wheelIndex]
                ?: throw TrackedGeometryException("belt member ${members[i].bone} has no compiled wheel")
            val next = members[(i + 1) % members.size]
            val second = wheels[next.wheelIndex]
                ?: throw TrackedGeometryException("belt member ${next.bone} has no compiled wheel")
            first.massKg + second.massKg
        }
        val total = pairMass.sum()
        require(total > 0.0 && total.isFinite()) { "belt members declare no usable mass" }
        return pairMass.map { sideBudgetKg * it / total }
    }

    /** The sum of an allocation, for the conservation check. */
    fun totalOf(runMasses: List<Double>): Double = runMasses.sum()
}
