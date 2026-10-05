function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    pmc.add("Gravity", 0.5 + level * 0.02)
    pmc.mul("Damage", 1 + level * 0.05)
    pmc.mul("ProjectileLife", 3)
}
