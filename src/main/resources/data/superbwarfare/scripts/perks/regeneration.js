function tick(tag, level, gunData, entity) {
    const maxEnergy = gunData.getMaxEnergyStored()
    if (maxEnergy > 0) {
        gunData.receiveEnergy(Math.floor((1 + level * 0.4) * maxEnergy / 4000))
    }
}
