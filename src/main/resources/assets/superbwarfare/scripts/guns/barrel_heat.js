/**
 * 枪管烧红：热量越高，`barrel_illuminated` 那几片枪口发光片沿 Z 轴（枪管轴线）拉得越长。
 *
 * 这几片原本是枪口处厚 0.25 单位的薄片，骨骼枢轴就压在它们最靠前的那一面（z = -20.26848），
 * 所以放大 zScale 只会朝 +Z（枪身方向）把它们拉回去。枪管本体（barrels / guan1~6）长 24.99 单位，
 * 于是"热量即缩放倍数"这个映射在满热（100 → 25 单位）时刚好铺满整根枪管。
 */
const HEAT_BONE = "barrel_illuminated"

/** 满热刻度，与 `HeatBarOverlay` 里的 `heat / 100` 保持一致。 */
const FULL_HEAT = 100

/** 每 1 点热量拉伸的倍数：1.0 就是"10 热量 10 倍、100 热量 100 倍"。 */
const SCALE_PER_HEAT = 1.0

function transformCustomModelPart(stack, model, transformType, partialTick, renderer) {
    const bone = model.getBone(HEAT_BONE)
    if (bone == null) {
        // LOD 模型里没有这根骨骼，远端渲染直接跳过
        return
    }

    // 热量是逐物品的属性，不区分是不是本地玩家手里那把枪
    const heat = renderer.scriptHeat(stack)
    if (heat <= 0) {
        // 冷枪不显示。renderer 每帧都会 resetPose，所以这里必须每帧重新隐藏一次，
        // 隐藏的是整棵子树，bone16~bone21 会跟着一起消失。
        bone.visible = false
        return
    }

    bone.visible = true
    // 钳在满热上，免得被改过 HEAT_PER_SHOOT 的数据包拉出一根比枪还长的光柱
    bone.zScale = JsMath.clamp(heat, 0.0, FULL_HEAT) * SCALE_PER_HEAT
}
