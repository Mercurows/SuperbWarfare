function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    pmc.mul("ExplosionDamage", 0.8 + level * 0.1)
    pmc.mul("ExplosionRadius", 0.5)
    pmc.set("Gravity", 0.0)
}

function modifyProjectile(projectile, level, isShotgun) {
    projectile.setNoGravity(true)
}
