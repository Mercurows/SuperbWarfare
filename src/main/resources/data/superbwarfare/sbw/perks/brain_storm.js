// BRAIN_STORM: 每级额外爆头伤害加成
function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    pmc.add("Headshot", 0.25 * level)
}
