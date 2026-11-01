function getModifiedDamage(damage, target, level, perkTag, sourceProxy) {
    if (!sourceProxy || !sourceProxy.isGunDamage()) return damage
    return modifyDamage(damage, target, level)
}

function getModifiedMeleeDamage(damage, target, level, perkTag, sourceProxy) {
    return modifyDamage(damage, target, level)
}

function modifyDamage(damage, target, level) {
    const health = target.getHealth()
    if (health >= 100) {
        return damage + health * 0.00002 * Math.pow(level, 2)
    }
    return damage
}
