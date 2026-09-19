const SIGHT_FOLD_BONES = ["sight1fold", "sight2fold"]

/** 装上瞄准镜后机械瞄具向后倒下的角度。 */
const SIGHT_FOLD_DEG = 90

/** 每秒向目标角度靠近的比例，乘上帧时长得到这一帧的插值比例。 */
const SIGHT_FOLD_RATE = 0.9

/** 与目标差距小于这个值就直接吸附过去，否则插值只会无限逼近而永远到不了终点。 */
const SIGHT_FOLD_EPSILON = 0.01

function transformCustomModelPart(stack, model, transformType, partialTick, renderer) {
    const targetDeg = renderer.scriptHasScope(stack) ? SIGHT_FOLD_DEG : 0
    const rate = Math.min(renderer.scriptFrameDeltaSeconds() * SIGHT_FOLD_RATE, 1)

    // 折叠角度必须"每把枪一份"。以前这里是一个顶层的 let，而顶层变量是每个枪械 id 一份作用域，
    // 会被世界上所有同型号的枪共用——手里的枪和地上的枪会互相把对方拉向自己的目标，
    // 于是所有 M4 / QBZ-191 的机械瞄具一起动。JsState 按 stack 身份分别记忆。
    const deg = JsState.smooth(stack, "sightFold", targetDeg, rate, SIGHT_FOLD_EPSILON)

    const rotation = JsMath.Axis.XP.rotationDegrees(deg)

    // 注意：循环体内必须用 let。这个 Rhino 版本的 const 不是块级作用域，
    // 循环里重复声明会保留上一轮的绑定。
    for (let i = 0; i < SIGHT_FOLD_BONES.length; i++) {
        let bone = model.getBone(SIGHT_FOLD_BONES[i])
        if (bone == null) {
            continue
        }

        bone.rotation.set(rotation)
        bone.rotationInEuler.x = deg * JsMath.DEG_TO_RAD
    }
}
