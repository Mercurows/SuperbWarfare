function modifyProperty(pmc, level, perkTag) {
    pmc.mul("Damage", 0.2 + 0.04 * level)
}

function modifyProjectile(projectile, level, isShotgun) {
    projectile.setPenetrating(true)
}
