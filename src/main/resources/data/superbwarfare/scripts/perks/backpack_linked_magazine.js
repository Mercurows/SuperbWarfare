function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    pmc.set("Magazine", 0)
    pmc.add("HeatPerShoot", (20 - level) * 0.15)
}
