function getModifiedDamage(damage, target, level, perkTag, sourceProxy) {
    if (!perkTag || !sourceProxy || !sourceProxy.isGunDamage()) return damage

    const maxHealth = target.getMaxHealth()
    if (maxHealth <= 0) return damage

    const health = target.getHealth()
    if (health >= maxHealth * (80 + level) / 100) return damage

    return damage + health * (4 + 0.8 * level) / 100
}
