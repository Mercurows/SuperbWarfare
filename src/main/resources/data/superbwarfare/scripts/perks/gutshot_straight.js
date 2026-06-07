function getModifiedDamage(damage, target, level, perkTag, sourceProxy) {
    if (sourceProxy && sourceProxy.isGunFireDamage()) {
        const directEntity = sourceProxy.getDirectEntity()
        if (directEntity.isProjectile() && directEntity.isZoom()) {
            return damage * (1.15 + 0.05 * level)
        }
    }
    return damage
}
