function modifyProperty(pmc, level, perkTag) {
    pmc.set("Magazine", 0)
    pmc.add("HeatPerShoot", (20 - level) * 0.15)
}
