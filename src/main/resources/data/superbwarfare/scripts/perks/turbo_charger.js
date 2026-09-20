function modifyProperty(pmc, level, perkTag, gunData) {
    if (!pmc) return
    pmc.add("RpmAddAfterShoot", 5 + 3 * level)
}
