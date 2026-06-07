function tick(tag, level, gunData, entity) {
    const maxEnergy = gunData.getMaxEnergyStored()
    if (maxEnergy > 0) {
        gunData.receiveEnergy(Math.floor(level * maxEnergy / 2000))
    }
}
