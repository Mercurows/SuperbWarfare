function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    pmc.add("BypassesArmor", -Math.max(0, 1 - 0.05 * (level - 1)))
}

function getEffectAmplifier(level) {
    return Math.floor(level / 2)
}
