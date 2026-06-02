function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    if (!perkTag) return
    if (perkTag.getInt("OneTwoPunchTime") > 0) {
        pmc.mul("MeleeDamage", 1.5 + 0.75 * (level - 1))
    }
}

function onHit(attackerProxy, level, gunData, targetProxy, perkTag) {
    if (!perkTag || !gunData) return
    perkTag.putInt("OneTwoPunchCount", perkTag.getInt("OneTwoPunchCount") + 1)
    perkTag.putInt("OneTwoPunchCountTime", 2)

    var projectileAmount = gunData.getProjectileAmount()
    var needCount = Math.floor(projectileAmount * (1 - 0.05 * (level - 1)))

    if (perkTag.getInt("OneTwoPunchCount") >= needCount) {
        perkTag.putInt("OneTwoPunchTime", 60)
        perkTag.remove("OneTwoPunchCount")
        perkTag.remove("OneTwoPunchCountTime")
    }
}

function onChangeSlot(perkTag, level, gunData, entityProxy) {
    if (!perkTag) return
    perkTag.remove("OneTwoPunchTime")
    perkTag.remove("OneTwoPunchCount")
    perkTag.remove("OneTwoPunchCountTime")
}

function onMeleeAttack(perkTag, level, gunData, targetProxy, sourceProxy) {
    if (!perkTag) return
    perkTag.remove("OneTwoPunchTime")
    perkTag.remove("OneTwoPunchCount")
    perkTag.remove("OneTwoPunchCountTime")
}

function tick(perkTag, level, gunData, entityProxy) {
    if (!perkTag) return
    perkTag.reduceCooldown("OneTwoPunchTime")
    perkTag.reduceCooldown("OneTwoPunchCountTime")
}
