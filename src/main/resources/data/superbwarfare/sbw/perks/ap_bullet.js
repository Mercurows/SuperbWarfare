// AP_BULLET: 属性修改 — 每级额外穿甲
function modifyProperty(pmc, level, perkTag, gunDataProxy) {
    // 基础 AmmoPerk 配置 (bypassArmorRate 0.4, damageRate 0.9, speedRate 1.2, slug=true)
    // 已在 JsPerk 中处理，这里只追加等级缩放
    // APBullet 原逻辑: modifier[BYPASSES_ARMOR] += 0.05 * (level - 1), 下限 0
    pmc.add("BypassesArmor", Math.max(0, 0.05 * (level - 1)))
}
