function getModifiedDamage(damage, target, level) {
    if (target.isUndead) {
        return damage * (1 + 0.5 * level)
    }
    return damage
}
