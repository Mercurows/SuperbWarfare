function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    pmc.add("ProjectileSplitCount", 1)
    pmc.add("ProjectileSplitAmount", level / 5 + 1)
    pmc.mul("ExplosionDamage", 1.1 + level * 0.05)
}
