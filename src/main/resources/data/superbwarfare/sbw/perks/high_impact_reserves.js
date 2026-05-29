// HIGH_IMPACT_RESERVES: 弹匣弹药越低伤害越高
function modifyProperty(pmc, level, perkTag, gunData) {
    if (!gunData) return

    var ammo = gunData.getAmmo()
    var magazine = gunData.getMagazine()
    if (magazine <= 0) magazine = 1
    var rate = ammo / magazine
    var limit = 0.5 + (level - 1) * 0.02

    if (rate <= limit) {
        var min1 = 0.12
        var max1 = 0.25
        var min20 = 0.75
        var max20 = 1.5
        var t = (level - 1) / 19.0
        var minOutput = min1 + t * (min20 - min1)
        var maxOutput = max1 + t * (max20 - max1)
        pmc.mul("Damage", 1 + (1 - (rate / limit)) * (maxOutput - minOutput) + minOutput)
    }
}
