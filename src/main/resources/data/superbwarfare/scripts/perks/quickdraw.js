function scaleDurationBySpeed(pmc, key, speedFactor) {
    const current = pmc.get(key)
    if (current > 0) {
        // The gun data stores duration. +X% speed means the duration is divided by 1+X%.
        pmc.set(key, Math.max(1, Math.round(current / speedFactor)))
    }
}

function modifyProperty(pmc, level, perkTag, gunData) {
    if (!pmc) return

    const reloadRate = 0.05 * level
    const reloadSpeedFactor = 1 + reloadRate
    const reloadDurations = [
        "NormalReloadTime",
        "EmptyReloadTime",
        "BoltActionTime",
        "PrepareTime",
        "PrepareLoadTime",
        "PrepareEmptyTime",
        "PrepareAmmoLoadTime",
        "IterativeTime",
        "IterativeAmmoLoadTime",
        "FinishTime"
    ]
    for (let i = 0; i < reloadDurations.length; i++) {
        scaleDurationBySpeed(pmc, reloadDurations[i], reloadSpeedFactor)
    }

    const drawRate = 0.8 + 0.06 * level
    scaleDurationBySpeed(pmc, "DrawTime", 1 + drawRate)
}
