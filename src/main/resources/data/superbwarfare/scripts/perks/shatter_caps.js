function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    var speedPenalty = 0.4 * (20 - level) / 19
    pmc.mul("Velocity", 1 - speedPenalty)

    pmc.mul("BypassesArmor", 0.5)

    if (gunDataProxy.isZooming()) return

    pmc.set("Spread", 2)

    var pellets = 4 + Math.floor((level - 1) / 3)

    var totalRate = 1 + 0.2 * (level - 1)

    var originalAmount = pmc.get("ProjectileAmount")
    pmc.mul("Damage", totalRate * originalAmount / pellets)
    pmc.set("ProjectileAmount", pellets)
}
