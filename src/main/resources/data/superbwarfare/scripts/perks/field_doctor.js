function onHurtEntity(damage, tag, level, gunData, target, source) {
    if (!source.shouldHealTarget(target)) return
    if (target.isLivingEntity()) {
        target.heal(damage * Math.min(1.0, 0.25 + 0.05 * level))
    }
}
