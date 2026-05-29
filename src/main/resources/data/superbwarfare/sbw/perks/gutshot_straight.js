// GUTSHOT_STRAIGHT: 开镜射击伤害加成
function getModifiedDamage(damage, target, level, perkTag, sourceProxy) {
    if (sourceProxy && sourceProxy.isGunFireDamage()) {
        var directEntity = sourceProxy.getDirectEntity()
        if (directEntity.isProjectile() && directEntity.isZoom()) {
            return damage * (1.15 + 0.05 * level)
        }
    }
    return damage
}
