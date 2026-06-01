function getModifiedDamage(damage, target, level) {
    if (target.isRaider) {
        return damage * (1 + 0.5 * level)
    }
    return damage
}

function getEffectAmplifier(level) {
    return Math.floor(level / 4)
}

function getEffectDuration(level) {
    return 20 + level * 10
}
