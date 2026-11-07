var MAX_LEVEL = 20

function chainLimits(level) {
    level = Math.max(1, Math.min(MAX_LEVEL, level))
    var progress = (level - 1) / (MAX_LEVEL - 1)
    return {
        rounds: level >= 20 ? 3 : level >= 10 ? 2 : 1,
        targets: 1 + Math.floor(progress * 4),
        range: 3 + 5 * progress,
        damageRetention: 0.5 + 0.5 * progress,
        shockRetention: 0.5 + 0.5 * progress
    }
}

function onHit(attacker, level, gunData, target, perkTag, projectile) {
    if (!projectile || !projectile.canEmitLightning() || !target.isLivingEntity() || target.isCreative()) return
    if (attacker.isNull()) return
    var limits = chainLimits(level)
    var visited = {}
    visited[target.getId()] = true
    visited[attacker.getId()] = true
    var origins = [target]
    var volt = projectile.getVolt()
    for (var round = 1; round <= limits.rounds && origins.length > 0; round++) {
        var candidates = []
        for (var i = 0; i < origins.length; i++) {
            var nearby = origins[i].getNearbyEnemies(attacker, limits.range)
            for (var j = 0; j < nearby.length; j++) {
                var next = nearby[j]
                if (!visited[next.getId()]) {
                    candidates.push({from: origins[i], to: next, distance: origins[i].distanceToSqr(next)})
                }
            }
        }
        candidates.sort(function(a, b) { return a.distance - b.distance || a.to.getId() - b.to.getId() })
        origins = []
        for (var k = 0; k < candidates.length && origins.length < limits.targets; k++) {
            var arc = candidates[k]
            if (visited[arc.to.getId()]) continue
            visited[arc.to.getId()] = true
            arc.from.lightningTo(arc.to)
            var effect = Math.pow(limits.shockRetention, round)
            arc.to.shock(projectile.getDamage() * Math.pow(limits.damageRetention, round),
                Math.max(1, Math.floor((100 + volt * 30) * effect)), Math.floor(volt * effect), attacker)
            // 下一轮从本轮命中的生物继续弹射，而不是始终从最初命中点发出。
            origins.push(arc.to)
        }
    }
}
