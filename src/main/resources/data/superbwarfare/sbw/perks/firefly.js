// FIREFLY: 爆头击杀产生爆炸
function onKill(perkTag, level, gunData, targetProxy, sourceProxy) {
    if (!sourceProxy.isHeadshotDamage()) return
    var attacker = sourceProxy.getAttackingPlayer()
    if (attacker.isNull()) return
    targetProxy.createExplosion(
        6 + level * 2,
        2 + level * 0.5,
        attacker,
        true,
        3 + Math.floor(level / 3)
    )
}
