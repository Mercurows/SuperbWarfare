function tick(tag, level, gunData, entity) {
    tag.reduceCooldown("HealClipTime")
}

function onKill(tag, level, gunData, target, source) {
    if (source.isGunDamage() && level !== 0) {
        tag.putInt("HealClipTime", 80 + level * 20)
    }
}

function preReload(tag, level, gunData, entity) {
    const time = tag.getInt("HealClipTime")
    if (time > 0) {
        tag.remove("HealClipTime")
        tag.putBoolean("HealClip", true)
    } else {
        tag.remove("HealClip")
    }
}

function postReload(tag, level, gunData, entity) {
    if (!entity.isLivingEntity()) return
    if (tag.has("HealClip")) return

    const lvl = level === 0 ? 1 : level
    const healAmount = 12 * (0.8 + 0.2 * lvl)
    entity.heal(healAmount)
    entity.absorbExtraHealth(healAmount, 0.3)
    entity.healNearbyAllies(5.0, 6.0 * (0.8 + 0.2 * lvl))
}
