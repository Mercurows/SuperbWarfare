function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    if (!gunDataProxy) return

    const ammo = gunDataProxy.getAmmo()
    let magazine = gunDataProxy.getMagazine()
    if (magazine <= 0) magazine = 1
    const rate = ammo / magazine
    const limit = 0.5 + (level - 1) * 0.02

    if (rate <= limit) {
        const min1 = 0.12
        const max1 = 0.25
        const min20 = 0.75
        const max20 = 1.5
        const t = (level - 1) / 19.0
        const minOutput = min1 + t * (min20 - min1)
        const maxOutput = max1 + t * (max20 - max1)
        pmc.mul("Damage", 1 + (1 - (rate / limit)) * (maxOutput - minOutput) + minOutput)
    }
}
