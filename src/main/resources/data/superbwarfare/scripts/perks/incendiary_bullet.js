function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    if (pmc.isShotgun()) {
        pmc.set("Velocity", 4.5)
    }
}

function modifyProjectile(projectile, level, isShotgun) {
    projectile.fireBullet(level, isShotgun)
}

function getEffectDuration(level) {
    return 60 + 20 * level
}
