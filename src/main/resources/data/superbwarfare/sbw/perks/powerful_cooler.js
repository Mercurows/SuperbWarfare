function modifyProperty(pmc, level, perkTag) {
    pmc.mul("NaturalCooldown", 1 + 0.05 * level)
    pmc.mul("HeatPerShoot", 1 - 0.02 * level)
}
