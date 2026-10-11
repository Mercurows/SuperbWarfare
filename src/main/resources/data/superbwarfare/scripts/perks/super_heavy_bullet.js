function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    pmc.add("Gravity", 0.75 + level * 0.02)
    pmc.add("BypassesArmor", level * 0.05)
    pmc.mul("Damage", 1 + level * 0.05)
    pmc.mul("ProjectileLife", 3)
    pmc.add("Knockback", 0.5 + level * 0.025)
}

function modifyProjectile(projectile, level, isShotgun) {
    projectile.forceKnockback()
}
