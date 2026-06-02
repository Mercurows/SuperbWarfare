function onMeleeAttack(tag, level, gunData, target, source) {
    const attacker = source.getSourceEntity()
    if (!attacker.isPlayer()) return

    const rate = 0.2 + (level - 1) * 0.03
    attacker.heal(attacker.getMaxHealth() * rate / 2)

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
