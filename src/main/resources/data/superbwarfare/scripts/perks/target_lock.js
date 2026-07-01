function tick(perkTag, level, gunDataProxy, entityProxy) {
    if (!perkTag) return

    const isZooming = gunDataProxy.isZooming()
    const hasStacks = perkTag.getInt("TargetLockStacks") > 0
    let hasTarget = false

    if (isZooming && !entityProxy.isNull()) {
        let maxDistance = Math.min(20 + (level - 1) * 20, 200)

        const target = entityProxy.findLookingEntity(maxDistance)
        hasTarget = !target.isNull()
    }

    if (hasTarget) {
        perkTag.reduceCooldown("TargetLockTick")
        if (!perkTag.has("TargetLockTick")) {
            const stacks = perkTag.getInt("TargetLockStacks")
            perkTag.putInt("TargetLockStacks", Math.max(5, stacks + 1))
            perkTag.putInt("TargetLockTick", 50)
        }
    } else if (hasStacks) {
        perkTag.reduceCooldown("TargetLockTick")
        if (!perkTag.has("TargetLockTick")) {
            perkTag.remove("TargetLockStacks")
        }
    } else {
        perkTag.remove("TargetLockStacks")
        perkTag.remove("TargetLockTick")
    }
}

function modifyProperty(pmcProxy, level, perkTag, gunDataProxy) {
    if (!perkTag) return

    const stacks = perkTag.getInt("TargetLockStacks")
    if (stacks <= 0) return

    pmcProxy.mul("Damage", 1 + stacks * level * 0.1)
}
