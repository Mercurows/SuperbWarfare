// 涡轮增压器：每次开火后叠加的 RPM 增量（5 + 3 * 等级）。
//
// 增量会和枪械数据里自身的 RpmAddAfterShoot 相加，实际射速 = RPM + 累加值，
// 累加值由 MinCustomRpm / MaxCustomRpm 限定上下限，所以这里同时把上限也抬高：
// 默认上限只有 600，不抬高的话涡轮的加成根本没地方发挥。
// 每级 +30，满级 +600，正好把默认的 600 顶到 1200（旧版涡轮自己写死的那个上限）。
function modifyProperty(pmc, level, perkTag, gunData) {
    if (!pmc) return
    pmc.add("RpmAddAfterShoot", 5 + 3 * level)
    pmc.add("MaxCustomRpm", 30 * level)
}
