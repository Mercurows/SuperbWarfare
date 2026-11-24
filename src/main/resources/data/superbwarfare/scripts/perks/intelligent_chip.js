function modifyProperty(pmc, level, perkTag, gunData) {
    if (level <= 0) return

    pmc.add("AimAssist", 5 * Math.min(level, 20))
    pmc.set("AutoAim", true)
    pmc.add("AutoAimRange", 32 + 8 * (level - 1))
}
