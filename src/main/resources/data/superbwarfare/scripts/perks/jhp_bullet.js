function getModifiedDamage(damage, target, level, perkTag, sourceProxy) {
    const armor = target.armor
    const factor = (400 / (Math.pow(Math.max(0, armor - 5), 4) + 400)) + 0.2
    return damage * (1 + 0.15 * level) * factor
}
