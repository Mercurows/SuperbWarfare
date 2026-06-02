function getModifiedDamage(damage, target, level, perkTag, sourceProxy) {
    if (!perkTag) return damage
    if (sourceProxy && sourceProxy.isHeadshotDamage() && perkTag.getInt("HeadSeeker") > 0) {
        return damage * (1.095 + 0.0225 * level)
    }
    return damage
}

function tick(perkTag, level, gunData, entityProxy) {
    if (perkTag) {
        perkTag.reduceCooldown("HeadSeeker")
    }
}

function onHurtEntity(damage, perkTag, level, gunData, targetProxy, sourceProxy) {
    if (!perkTag) return
    if (sourceProxy.isGunFireDamage()) {
        perkTag.putInt("HeadSeeker", 11 + level * 2)
    }
}
