var BONUS_PER_LEVEL = 0.0005
var MIN_DURATION = 3

function reduceDuration(pmc, key, factor) {
    var duration = Number(pmc.get(key))
    if (duration >= MIN_DURATION) {
        pmc.set(key, Math.max(MIN_DURATION, Math.round(duration * factor)))
    }
}

function reduceSprintDuration(pmc, gunData, factor) {
    var weight = Number(pmc.get("Weight"))
    var handlingWeight = Math.max(1, Math.min(50, weight + 4 * (weight - gunData.getBaseWeight())))
    var speed = Math.max(0.1, Math.min(10, Number(pmc.get("SprintTransitionSpeed"))))
    var duration = 10 * Math.pow(handlingWeight / 6, 0.25) / speed
    if (duration >= MIN_DURATION) {
        pmc.set("SprintTransitionSpeed", speed * duration / Math.max(MIN_DURATION, duration * factor))
    }
}

function modifyProperty(pmc, level, perkTag, gunData) {
    var bonus = Math.max(0, gunData.getLevel()) * Math.max(0, level) * BONUS_PER_LEVEL
    if (bonus <= 0) return

    var increases = ["Damage", "ExplosionDamage", "ExplosionRadius", "MeleeDamage"]
    for (var i = 0; i < increases.length; i++) {
        pmc.mul(increases[i], 1 + bonus)
    }

    var reduction = Math.max(0, 1 - bonus * 0.5)
    pmc.mul("RecoilX", reduction)
    pmc.mul("RecoilY", reduction)

    var durations = ["NormalReloadTime", "EmptyReloadTime", "DrawTime", "ZoomTime"]
    for (var j = 0; j < durations.length; j++) {
        reduceDuration(pmc, durations[j], reduction)
    }
    reduceSprintDuration(pmc, gunData, reduction)
}
