function onMeleeDamage(damage, perkTag, level, gunData, target, source) {
    const duration = 70 + 30 * level
    const amplifier = Math.floor(level / 2)
    target.addEffect("superbwarfare:trauma", duration, amplifier, source.getAttacker())
}
