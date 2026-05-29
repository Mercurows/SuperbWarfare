// MONSTER_HUNTER: 对怪物额外伤害
function getModifiedDamage(damage, target, level, perkTag, sourceProxy) {
    if (target.isMonster) {
        return damage * (1.1 + 0.1 * level)
    }
    return damage
}
