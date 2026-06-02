function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    if (perkTag && perkTag.getInt("DesperadoTimePost") > 0) {
        pmc.mul("RPM", 1.285 + 0.015 * level)
    }
}

function tick(perkTag, level, gunData, entityProxy) {
    if (perkTag) {
        perkTag.reduceCooldown("DesperadoTime")
        perkTag.reduceCooldown("DesperadoTimePost")
    }
}

function onKill(perkTag, level, gunData, targetProxy, sourceProxy) {
    if (perkTag && sourceProxy.isHeadshotDamage()) {
        perkTag.putInt("DesperadoTime", 90 + level * 10)
    }
}

function preReload(perkTag, level, gunData, entityProxy) {
    if (!perkTag) return
    const time = perkTag.getInt("DesperadoTime")
    if (time > 0) {
        perkTag.remove("DesperadoTime")
        perkTag.putBoolean("Desperado", true)
    } else {
        perkTag.remove("Desperado")
    }
}

function postReload(perkTag, level, gunData, entityProxy) {
    if (!perkTag) return
    if (!perkTag.getBoolean("Desperado")) return
    perkTag.putInt("DesperadoTimePost", 110 + level * 10)
}
