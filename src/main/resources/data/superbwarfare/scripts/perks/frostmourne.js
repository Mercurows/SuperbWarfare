function getModifiedMeleeDamage(damage, target, level, perkTag, source) {
    if (target.hasEffect("minecraft:slowness")) {
        return damage * (1 + 0.15 * level)
    }
    return damage
}

function onMeleeAttack(perkTag, level, gunData, target, source) {
    const duration = getEffectDuration(level)
    target.freeze(duration)
    target.addEffect("minecraft:slowness", duration, 0, source.getAttacker())
}

function addMeleeProjectileEffects(effects, level) {
    effects.addEffect("minecraft:slowness", getEffectDuration(level), 0)
}

function getEffectDuration(level) {
    return 60 + 20 * level
}
