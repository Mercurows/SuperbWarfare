function modifyProperty(pmc, level, perkTag, gunData) {
    pmc.add("AimAssist", 5 * Math.max(0, Math.min(level, 20)))
}
