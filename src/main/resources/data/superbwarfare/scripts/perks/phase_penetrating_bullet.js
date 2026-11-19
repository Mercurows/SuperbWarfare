function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    pmc.mul("Damage", 0.2 + 0.04 * level)
    pmc.set("ProjectilePenetrating", true)
}
