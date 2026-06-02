function modifyProperty(pmc, level, perkTag, gunDataProxy) {
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
        const tally = Math.min(3, perkTag.getInt("KillingTally") + 1)
        perkTag.putInt("KillingTally", tally)
    }
}

function onChangeSlot(perkTag, level, gunData, entityProxy) {
    if (perkTag) {
        perkTag.remove("KillingTally")
    }
}
