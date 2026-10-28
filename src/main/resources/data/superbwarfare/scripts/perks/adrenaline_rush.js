function setActive(perkTag, gunData, boost) {
    const oldBoost = perkTag.getDouble("AdrenalineRushBoost")
    if (Math.abs(oldBoost - boost) < 0.0001) return

    if (boost > 0) {
        perkTag.putDouble("AdrenalineRushBoost", boost)
    } else {
        perkTag.remove("AdrenalineRushBoost")
    }
    gunData.invalidateProperties()
}

function tick(perkTag, level, gunData, entity) {
    if (!perkTag || !gunData || !entity || !entity.isLivingEntity()) {
        if (perkTag && gunData && perkTag.has("AdrenalineRushBoost")) {
            perkTag.remove("AdrenalineRushBoost")
            gunData.invalidateProperties()
        }
        return
    }

    const maxHealth = entity.getMaxHealth()
    const health = entity.getHealth()
    if (maxHealth <= 0 || health >= maxHealth) {
        setActive(perkTag, gunData, 0)
        return
    }

    const lostHealth = Math.max(0, Math.min(0.9, 1 - health / maxHealth))
    const boost = lostHealth * (0.4 + 0.03 * level)
    setActive(perkTag, gunData, boost)
}

function scaleDurationBySpeed(pmc, key, speedFactor) {
    const current = pmc.get(key)
    if (current > 0) {
        pmc.set(key, Math.max(1, Math.round(current / speedFactor)))
    }
}

function modifyProperty(pmc, level, perkTag, gunData) {
    if (!pmc || !perkTag) return

    const boost = perkTag.getDouble("AdrenalineRushBoost")
    if (boost <= 0) return

    const speedFactor = 1 + boost
    pmc.mul("RpmMultiplier", speedFactor)

    const reloadDurations = [
        "NormalReloadTime",
        "EmptyReloadTime",
        "PrepareTime",
        "PrepareLoadTime",
        "PrepareEmptyTime",
        "PrepareAmmoLoadTime",
        "IterativeTime",
        "IterativeAmmoLoadTime",
        "FinishTime"
    ]
    for (let i = 0; i < reloadDurations.length; i++) {
        scaleDurationBySpeed(pmc, reloadDurations[i], speedFactor)
    }
    scaleDurationBySpeed(pmc, "DrawTime", speedFactor)
    scaleDurationBySpeed(pmc, "ZoomTime", speedFactor)
}
