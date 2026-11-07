var FIRST_TARGET = "FirstTargetUUID"

// Zero the spawned arrow too, after gun property modifiers have been applied.
function modifyProjectile(projectile, level, isShotgun) {
    projectile.setDamage(0)
}

function onHit(attacker, level, gunData, target, perkTag, projectile) {
    if (!projectile || !target.isLivingEntity() || !target.isAlive()) return
    attacker.showHitFeedback()

    // Use the arrow's firing state, not the shooter's zoom state at impact.
    if (!projectile.isZoom()) return

    var uuid = String(target.getUUID())
    var firstUUID = String(perkTag.getString(FIRST_TARGET))
    if (uuid === firstUUID) return

    var first = target.findByUUID(firstUUID)
    var range = 32 + 224 * (Math.max(1, Math.min(20, level)) - 1) / 19
    // Invalid or distant marks are replaced, as are pairs with no Mob to control.
    if (!first.isLivingEntity() || !first.isAlive() || first.distanceToSqr(target) > range * range
        || (!first.isMob() && !target.isMob())) {
        perkTag.putString(FIRST_TARGET, uuid)
        return
    }

    first.setAttackTarget(target)
    target.setAttackTarget(first)
    perkTag.remove(FIRST_TARGET)
}
