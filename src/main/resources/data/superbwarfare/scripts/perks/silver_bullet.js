function getModifiedDamage(damage, target, level, perkTag, sourceProxy) {
    if (target.isUndead) {
        return damage * (1 + 0.5 * level)
    }
    return damage
}
