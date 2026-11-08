function modifyProjectile(projectile, level, isShotgun) {
    var progress = (Math.max(1, Math.min(20, level)) - 1) / 19
    // Range in blocks; turn limit per tick and acquisition half-angle in degrees.
    projectile.setHoming(32 + 32 * progress, 4 + 8 * progress, 25 + 15 * progress)
}
