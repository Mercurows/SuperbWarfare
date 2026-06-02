// MAGNIFICENT_HOWL: 爆头击杀累积层数，换弹后获得伤害加成
function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    if (!perkTag) return
    if (perkTag.getInt("MagnificentHowlDamageCount") > 0) {
        pmc.mul("Damage", 1.5)
    }
}

function onKill(perkTag, level, gunData, targetProxy, sourceProxy) {
    if (!perkTag) return
    if (sourceProxy.isHeadshotDamage()) {
        var count = perkTag.getInt("MagnificentHowlCount")
        var newCount = count + 1 + Math.floor(level / 5)
        var maxCount = 9 + level
        perkTag.putInt("MagnificentHowlCount", Math.min(newCount, maxCount))
    }
}

function preReload(perkTag, level, gunData, entityProxy) {
    if (!perkTag) return
    perkTag.putInt("MagnificentHowlDamageCount", perkTag.getInt("MagnificentHowlCount"))
    perkTag.remove("MagnificentHowlCount")
}

function onHurtEntity(damage, perkTag, level, gunData, targetProxy, sourceProxy) {
    if (!perkTag) return
    if (perkTag.getInt("MagnificentHowlDamageCount") > 0) {
        perkTag.reduceCooldown("MagnificentHowlDamageCount")
    }
}
