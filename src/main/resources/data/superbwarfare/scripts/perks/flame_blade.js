function onMeleeAttack(perkTag, level, gunData, target, source) {
    const duration = getEffectDuration(level)
    target.setSecondsOnFire(Math.ceil(duration / 20))
    target.addEffect("superbwarfare:burn", duration, level - 1, source.getAttacker())
}

function addMeleeProjectileEffects(effects, level) {
    effects.addEffect("superbwarfare:burn", getEffectDuration(level), level - 1)
}

function getEffectDuration(level) {
    return 60 + 20 * level
}
