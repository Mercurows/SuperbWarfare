function getModifiedCustomRPM(rpm, level, gunData) {
    return Math.min(1200, rpm + 5 + 3 * level)
}
