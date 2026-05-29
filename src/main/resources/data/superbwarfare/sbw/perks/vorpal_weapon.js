// VORPAL_WEAPON: 对高血量目标额外伤害
function getModifiedDamage(damage, target, level, perkTag, sourceProxy) {
    if (sourceProxy && sourceProxy.isGunDamage() && target.health >= 100) {
        return damage + target.health * 0.00002 * Math.pow(level, 2)
    }
    return damage
}
