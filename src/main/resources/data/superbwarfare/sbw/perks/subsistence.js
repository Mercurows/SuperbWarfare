function onKill(tag, level, gunData, target, source) {
    if (!source.isGunDamage()) return

    var attacker = source.getAttackingPlayer()
    if (attacker.isNull()) return

    var gunType = gunData.getGunType()
    var typeBonus = (gunType == "SMG" || gunType == "RIFLE") ? 0.07 : 0
    var rate = level * (0.1 + typeBonus)

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
