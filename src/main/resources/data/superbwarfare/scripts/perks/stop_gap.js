// 权宜之计（Stop Gap）：涡轮增压器的反面。
//
// 只有"持续开火"期间才生效：层数越高射速越低、单发伤害越高；停枪就把层数清空。
//
// 层数由 afterShoot 每打出一发 +1（封顶 8 层），tick 里递减"停火窗口"，
// 窗口归零就说明已经不在持续开火 → 清空层数（换弹/切枪也会兜底清空）。
// 属性完全由层数推导：
//   * 每层把全局射速倍率 RpmMultiplier 乘 0.9（8 层见底 = 20% 射速，固定值、和等级无关）
//   * 每层伤害加成由等级决定：1 级 +93.75%，每级 +15.625%
// 射速走"倍率"而不是直接改 RPM/低 CustomRPM 那套：最终射速 = (RPM + 每发累加值) * 倍率，
// 所以不会被涡轮的 +1200 加法累加值稀释。

// 层数上限
function stopGapMaxStacks() {
    return 8
}

// 停火多久算"没在持续开火"：一发实际间隔（1200 / 实际射速）的两倍，最少 2 tick。
// 必须用"乘了全局倍率之后"的射速：本 perk 自己就在压倍率，用基础 RPM 算的话
// 窗口会比真实间隔短，层数会在两发之间过期清空，表现为射速在慢/不慢之间反复跳。
function stopGapWindow(rpm) {
    return Math.max(2, Math.round(1200 / rpm) * 2)
}

function modifyProperty(pmc, level, perkTag, gunData) {
    if (!pmc || !perkTag) return

    const stacks = Math.min(stopGapMaxStacks(), perkTag.getInt("StopGapShots"))
    if (stacks <= 0) return

    // 射速：每层 -10%，8 层 = 20%
    // 改的是全局射速倍率（作用于"基础 RPM + 每发累加值"的最终结果），
    // 所以不会被涡轮那套 +1200 的加法累加值稀释
    pmc.mul("RpmMultiplier", 1 - 0.1 * stacks)

    // 伤害：每层 +93.75%（1 级），每级多 +15.625%
    let rate = 1 + (0.9375 + 0.15625 * (level - 1)) * stacks
    pmc.mul("Damage", rate)
    pmc.mul("ExplosionDamage", rate)
}

// 服务端每打出一发
function afterShoot(perkTag, level, gunData, entityProxy) {
    if (!perkTag || !gunData) return

    const stacks = Math.min(stopGapMaxStacks(), perkTag.getInt("StopGapShots") + 1)
    perkTag.putInt("StopGapShots", stacks)
    perkTag.putInt("StopGapWindow", stopGapWindow(gunData.getEffectiveRpm()))
    // 层数变了，PMC 缓存必须失效，否则属性不会刷新
    gunData.invalidateProperties()
}

// 服务端每 tick：递减停火窗口，窗口归零说明已经停枪
function tick(perkTag, level, gunData, entityProxy) {
    if (!perkTag || !gunData) return
    if (!perkTag.has("StopGapWindow")) return

    const left = perkTag.getInt("StopGapWindow") - 1
    if (left > 0) {
        perkTag.putInt("StopGapWindow", left)
        return
    }

    clearStopGap(perkTag, gunData)
}

function preReload(perkTag, level, gunData, entityProxy) {
    clearStopGap(perkTag, gunData)
}

function postReload(perkTag, level, gunData, entityProxy) {
    clearStopGap(perkTag, gunData)
}

function onChangeSlot(perkTag, level, gunData, entityProxy) {
    clearStopGap(perkTag, gunData)
}

function clearStopGap(perkTag, gunData) {
    if (!perkTag) return
    if (!perkTag.has("StopGapShots") && !perkTag.has("StopGapWindow")) return

    perkTag.remove("StopGapShots")
    perkTag.remove("StopGapWindow")
    if (gunData) gunData.invalidateProperties()
}
