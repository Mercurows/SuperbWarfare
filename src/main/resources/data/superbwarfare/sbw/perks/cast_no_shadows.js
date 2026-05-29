function onMeleeAttack(tag, level, gunData, target, source) {
    var attacker = source.getSourceEntity()
    if (!attacker.isPlayer()) return

    var rate = 0.2 + (level - 1) * 0.03
    attacker.heal(attacker.getMaxHealth() * rate / 2)

    var mag = gunData.getMagazine()
    var ammo = gunData.getAmmo()
    var ammoReload = Math.min(mag, Math.floor(mag * rate))
    var ammoNeed = Math.min(mag - ammo, ammoReload)

    var flag = attacker.isCreative() || attacker.hasCreativeAmmoBox()
    var ammoFinal = Math.min(gunData.countBackupAmmo(attacker), ammoNeed)

    if (flag) {
        ammoFinal = ammoNeed
    } else {
        gunData.consumeBackupAmmo(attacker, ammoFinal)
    }
    gunData.setAmmo(Math.min(mag, ammo + ammoFinal))
}
