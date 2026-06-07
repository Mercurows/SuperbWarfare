function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    pmc.set("ExplosionDamage", 0.9 * pmc.get("Damage") * 2 * (1 + 0.1 * level))
    pmc.set("ExplosionRadius", 1.7 + 0.3 * level)
}
