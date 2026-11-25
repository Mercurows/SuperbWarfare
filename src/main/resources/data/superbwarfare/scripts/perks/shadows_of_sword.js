function onMeleeLaunch(level, maxLevel, launcher) {
    const progress = Math.max(0, Math.min(1, (level - 1) / Math.max(1, maxLevel - 1)))
    const damageMultiplier = 0.8 + 0.4 * progress
    const lifeTicks = Math.round(20 + 20 * progress)
    launcher.launch("superbwarfare:sword_wave", damageMultiplier, lifeTicks)
}
