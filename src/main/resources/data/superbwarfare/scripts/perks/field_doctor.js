function modifyProjectile(projectile, level) {
    if (projectile.isZoom()) return

    const ratio = Math.min(1.0, 0.25 + 0.05 * level)
    projectile.setAllyHealRatio(Math.max(projectile.getAllyHealRatio(), ratio))
}
