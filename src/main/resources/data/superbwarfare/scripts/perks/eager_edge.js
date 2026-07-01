function onChangeSlot(perkTag, level, gunDataProxy, entityProxy) {
    if (!perkTag) return
    if (perkTag.getInt("EagerEdgeCooldown") > 0) return
    perkTag.putInt("EagerEdgeTime", 35 + level * 5)
}

function tick(perkTag, level, gunData, entityProxy) {
    if (!perkTag) return
    perkTag.reduceCooldown("EagerEdgeTime")
    perkTag.reduceCooldown("EagerEdgeCooldown")
}

function onMeleeSwing(perkTag, level, gunDataProxy, entityProxy) {
    if (!perkTag) return
    if (perkTag.getInt("EagerEdgeTime") > 0) {
        if (entityProxy && !entityProxy.isNull()) {
            entityProxy.pushForward(1.5 + 0.1 * level)
        }
        perkTag.remove("EagerEdgeTime")
        perkTag.putInt("EagerEdgeCooldown", Math.max(140 - 5 * level, 20))
    }
}
