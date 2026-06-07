function onKill(tag, level, gunData, target, source) {
    if (!source.isGunDamage()) return

    const attacker = source.getAttackingPlayer()
    if (attacker.isNull()) return

    const gunType = gunData.getGunType()
    const typeBonus = (gunType === "SMG" || gunType === "RIFLE") ? 0.07 : 0
    const rate = level * (0.1 + typeBonus)

    const mag = gunData.getMagazine()
    const ammo = gunData.getAmmo()
    const ammoReload = Math.min(mag, Math.floor(mag * rate))
    const ammoNeed = Math.min(mag - ammo, ammoReload)

    const flag = attacker.isCreative() || attacker.hasCreativeAmmoBox()
    let ammoFinal = Math.min(gunData.countBackupAmmo(attacker), ammoNeed)

    if (flag) {
        ammoFinal = ammoNeed
    } else {
        gunData.consumeBackupAmmo(attacker, ammoFinal)
    }
    gunData.setAmmo(Math.min(mag, ammo + ammoFinal))
}
