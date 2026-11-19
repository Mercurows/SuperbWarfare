function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    pmc.set("ProjectilePenetrating", true)
}

function modifyProjectile(projectile, level, isShotgun) {
    projectile.beast()
}
