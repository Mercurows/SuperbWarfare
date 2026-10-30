function onMeleeAttack(perkTag, level, gunData, target, source) {
    const duration = 60 + 20 * level
    target.setSecondsOnFire(Math.ceil(duration / 20))
    target.addEffect("superbwarfare:burn", duration, level - 1, source.getAttacker())
}
