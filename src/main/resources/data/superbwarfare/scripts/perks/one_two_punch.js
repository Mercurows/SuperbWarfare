function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    if (!perkTag) return
    if (perkTag.getInt("OneTwoPunchTime") > 0) {
        pmc.mul("MeleeDamage", 1.5 + 0.75 * (level - 1))
    }
}

function onHit(attackerProxy, level, gunData, targetProxy, perkTag) {
    if (!perkTag || !gunData) return

    const count = perkTag.getInt("OneTwoPunchCountTime") > 0 ? perkTag.getInt("OneTwoPunchCount") + 1 : 1
    const projectileAmount = gunData.getProjectileAmount()
    const needCount = Math.max(1, Math.floor(projectileAmount * (1 - 0.05 * (level - 1))))

    if (count >= needCount) {
        const wasActive = perkTag.getInt("OneTwoPunchTime") > 0
        perkTag.putInt("OneTwoPunchTime", 60)
        perkTag.remove("OneTwoPunchCount")
        perkTag.remove("OneTwoPunchCountTime")
        if (!wasActive) gunData.invalidateProperties()
    } else {
        perkTag.putInt("OneTwoPunchCount", count)
        perkTag.putInt("OneTwoPunchCountTime", 2)
    }
}

function onChangeSlot(perkTag, level, gunData, entityProxy) {
    clearOneTwoPunch(perkTag, gunData)
}

function onMeleeDamage(damage, perkTag, level, gunData, targetProxy, sourceProxy) {
    if (damage > 0) clearOneTwoPunch(perkTag, gunData)
}

function clearOneTwoPunch(perkTag, gunData) {
    if (!perkTag) return
    const wasActive = perkTag.getInt("OneTwoPunchTime") > 0
    perkTag.remove("OneTwoPunchTime")
    perkTag.remove("OneTwoPunchCount")
    perkTag.remove("OneTwoPunchCountTime")
    if (wasActive && gunData) gunData.invalidateProperties()
}

function tick(perkTag, level, gunData, entityProxy) {
    if (!perkTag) return
    const wasActive = perkTag.getInt("OneTwoPunchTime") > 0
    perkTag.reduceCooldown("OneTwoPunchTime")
    if (wasActive && !perkTag.has("OneTwoPunchTime") && gunData) gunData.invalidateProperties()

    perkTag.reduceCooldown("OneTwoPunchCountTime")
    if (!perkTag.has("OneTwoPunchCountTime")) perkTag.remove("OneTwoPunchCount")
}
