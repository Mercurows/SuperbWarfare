function tick(tag, level, gunData, entity) {
    tag.reduceCooldown("FourthTimesCharmTick")
    const count = tag.getInt("FourthTimesCharmCount")

    if (count >= 4) {
        tag.remove("FourthTimesCharmTick")
        tag.remove("FourthTimesCharmCount")

        const mag = gunData.getMagazine()
        if (mag > 0) {
            gunData.setAmmo(Math.min(mag, gunData.getAmmo() + 2))
        } else {
            gunData.addVirtualAmmo(2)
        }
    }
}

function onHurtEntity(damage, tag, level, gunData, target, source) {
    const directEntity = source.getDirectEntity()
    if (!directEntity.isProjectile()) return

    const bypassArmorRate = directEntity.getBypassArmorRate();
    if ((bypassArmorRate >= 1 && source.isAbsoluteHeadshot()) || source.isHeadshot()) {
        const tick = tag.getInt("FourthTimesCharmTick")
        if (tick <= 0) {
            tag.putInt("FourthTimesCharmTick", 40 + 10 * level)
            tag.putInt("FourthTimesCharmCount", 1)
        } else {
            const count = tag.getInt("FourthTimesCharmCount")
            if (count < 4) {
                tag.putInt("FourthTimesCharmCount", Math.min(4, count + 1))
            }
        }
    }
}
