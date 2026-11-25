function onMeleeDamage(damage, perkTag, level, gunData, target, source) {
    target.addEffect("superbwarfare:trauma", getEffectDuration(level), getEffectAmplifier(level), source.getAttacker())
}

function addMeleeProjectileEffects(effects, level) {
    effects.addEffect("superbwarfare:trauma", getEffectDuration(level), getEffectAmplifier(level))
}

function getEffectDuration(level) {
    return 70 + 30 * level
}

function getEffectAmplifier(level) {
    return Math.floor(level / 2)
}
