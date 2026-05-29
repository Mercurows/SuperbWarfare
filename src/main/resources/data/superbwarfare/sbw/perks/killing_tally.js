// KILLING_TALLY: 持续击杀叠加伤害(最多3层)
function modifyProperty(pmc, level, perkTag) {
    if (!perkTag) return
    pmc.mul("Damage", 1 + (0.1 * level) * perkTag.getInt("KillingTally"))
}

function preReload(perkTag, level, gunData, entityProxy) {
    if (perkTag) {
        perkTag.remove("KillingTally")
    }
}

function onKill(perkTag, level, gunData, targetProxy, sourceProxy) {
    if (!perkTag) return
    if (sourceProxy.isGunDamage()) {
        var tally = Math.min(3, perkTag.getInt("KillingTally") + 1)
        perkTag.putInt("KillingTally", tally)
    }
}

function onChangeSlot(perkTag, level, gunData, entityProxy) {
    if (perkTag) {
        perkTag.remove("KillingTally")
    }
}
