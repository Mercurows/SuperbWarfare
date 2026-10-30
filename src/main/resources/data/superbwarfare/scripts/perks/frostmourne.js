function getModifiedMeleeDamage(damage, target, level, perkTag, source) {
    if (target.hasEffect("minecraft:slowness")) {
        return damage * (1 + 0.15 * level)
    }
    return damage
}

function onMeleeAttack(perkTag, level, gunData, target, source) {
    const duration = 60 + 20 * level
    target.freeze(duration)
    target.addEffect("minecraft:slowness", duration, 0, source.getAttacker())
}
