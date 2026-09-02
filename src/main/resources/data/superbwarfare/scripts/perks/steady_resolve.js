function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    if (perkTag && perkTag.getInt("SteadyResolveTime") > 0) {
        pmc.mul("Damage", 1 + 0.25 * level)
    }
}

function onHurtEntity(damage, perkTag, level, gunData, targetProxy, sourceProxy) {
    if (!perkTag || !gunData || !sourceProxy || !sourceProxy.isGunDamage()) return
    if (perkTag.getDouble("SteadyResolveFocus") < 100) return

    perkTag.putDouble("SteadyResolveFocus", 0)
    perkTag.putInt("SteadyResolveTime", Math.round((3 + 0.1 * level) * 20))
    gunData.invalidateProperties()
}

function tick(perkTag, level, gunData, entityProxy) {
    if (!perkTag) return

    if (perkTag.getInt("SteadyResolveTime") > 0) {
        perkTag.reduceCooldown("SteadyResolveTime")
        if (!perkTag.has("SteadyResolveTime")) {
            gunData.invalidateProperties()
        }
        return
    }

    const focus = Math.min(100, perkTag.getDouble("SteadyResolveFocus") + (20 + level) / 20.0)
    perkTag.putDouble("SteadyResolveFocus", focus)
}

function onChangeSlot(perkTag, level, gunData, entityProxy) {
    if (!perkTag) return

    const wasActive = perkTag.getInt("SteadyResolveTime") > 0
    perkTag.remove("SteadyResolveTime")
    if (wasActive && gunData) {
        gunData.invalidateProperties()
    }
}
