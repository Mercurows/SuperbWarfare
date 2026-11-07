// 每级、每格下落距离增加的伤害。
const DAMAGE_PER_BLOCK_PER_LEVEL = 1

function getModifiedMeleeDamage(damage, target, level, perkTag, source) {
    let attacker = source.getAttacker()
    if (!attacker.isPlayer()) return damage
    return damage + Math.max(0, attacker.getFallDistance()) * level * DAMAGE_PER_BLOCK_PER_LEVEL
}

function onMeleeDamage(damage, perkTag, level, gunData, target, source) {
    let attacker = source.getAttacker()
    if (damage > 0 && attacker.isPlayer()) {
        attacker.resetFallDistance()
    }
}
