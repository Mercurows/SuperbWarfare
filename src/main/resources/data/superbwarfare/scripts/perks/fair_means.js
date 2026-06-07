function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    if (!perkTag) return
    if (perkTag.getBoolean("FairMeans")) {
        pmc.mul("Damage", 1.5 + 0.225 * level)
    } else {
        pmc.mul("Damage", 0.2 + 0.04 * level)
    }
}

function onHurtEntity(damage, perkTag, level, gunData, targetProxy, sourceProxy) {
    if (!perkTag) return
    if (gunData.getBypassesArmor() > 0) {
        if (sourceProxy.isProjectileAbsolute()) {
            perkTag.putBoolean("FairMeans", !perkTag.getBoolean("FairMeans"))
        }
    } else if (sourceProxy.isProjectile()) {
        perkTag.putBoolean("FairMeans", !perkTag.getBoolean("FairMeans"))
    }
}
