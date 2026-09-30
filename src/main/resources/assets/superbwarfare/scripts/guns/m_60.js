const SIGHT_FOLD_BONES = ["sight1fold", "sight2fold"]

/** 装上瞄准镜后机械瞄具向前倒下的角度。 */
const SIGHT_FOLD_DEG = -90

/** 每秒向目标角度靠近的比例，乘上帧时长得到这一帧的插值比例。 */
const SIGHT_FOLD_RATE = 0.9

/** 与目标差距小于这个值就直接吸附过去，否则插值只会无限逼近而永远到不了终点。 */
const SIGHT_FOLD_EPSILON = 0.01

const BIPOD_BONES = ["bipod_l", "bipod_r"]

/**
 * 脚架弹出时两条腿绕 X 轴转过的角度。
 *
 * 模型里脚架收起时贴着枪管指向 +Z（枪托方向：bone4/bone14 的方块从铰链 z≈-26.8 一直伸到 z≈-11.9），
 * 所以只有**正**角度会把它们往前甩下去——90° 正好把腿从 +Z 转到 -Y，脚落到铰链正下方并向两侧张开。
 * 负角度是往枪管上方翻（y 一路涨到 +13），方向是反的。
 * 与旧 GeckoLib 实现里的 `setRotX(1.5f)`（≈86°）同向，数值取整到 90°。
 */
const BIPOD_DEPLOY_X_DEG = 90

function transformCustomModelPart(stack, model, transformType, partialTick, renderer) {
    const targetDeg = renderer.scriptHasScope(stack) ? SIGHT_FOLD_DEG : 0
    const rate = Math.min(renderer.scriptFrameDeltaSeconds() * SIGHT_FOLD_RATE, 1)

    const deg = JsState.smooth(stack, "sightFold", targetDeg, rate, SIGHT_FOLD_EPSILON)

    const rotation = JsMath.Axis.XP.rotationDegrees(deg)

    for (let i = 0; i < SIGHT_FOLD_BONES.length; i++) {
        let bone = model.getBone(SIGHT_FOLD_BONES[i])
        if (bone == null) {
            continue
        }

        bone.rotation.set(rotation)
        bone.rotationInEuler.x = deg * JsMath.DEG_TO_RAD
    }

    // 脚架：进度本身就是 0→1 的平滑过渡（[ClientEventHandler.bipodViewTime]，卧姿且枪自带脚架时才涨），
    // 所以这里不需要像机械瞄具那样再过一遍 JsState。
    //
    // 必须把 stack 传进去：scriptBipodProgress 按对象身份判断这是不是本地玩家自己手里那把枪，
    // 地面掉落物 / 展示框 / 其他玩家手里的枪一律得到 0，保持收起。
    const progress = renderer.scriptBipodProgress(stack)
    if (progress <= 0) {
        // 收起状态保持 bind 姿态即可，renderer 每帧都会 resetPose
        return
    }

    const deployX = BIPOD_DEPLOY_X_DEG * JsMath.DEG_TO_RAD * progress

    // 注意：循环体内必须用 let。这个 Rhino 版本的 const 不是块级作用域，
    // 循环里重复声明会保留上一轮的绑定，导致第二条腿被跳过、第一条腿被处理两次。
    for (let i = 0; i < BIPOD_BONES.length; i++) {
        let bone = model.getBone(BIPOD_BONES[i])
        if (bone == null) {
            continue
        }

        // 两条腿各自带 ±45° 的 Z 轴外张角（父级 bone2/bone3 上还有 ∓15°），必须一并保留，
        // 否则展开后两腿会并拢成一条、且落点偏到枪管中线上。
        let bindY = bone.rotationInEuler.y
        let bindZ = bone.rotationInEuler.z

        // 等价于模型加载时的 rotateZYX(z, y, x)，即 Rz * Ry * Rx
        // 名字不能叫 rotation：上面机械瞄具那段有一个函数级的 `const rotation`，Rhino 的 const/let
        // 都不是块级作用域，这里再声明一个同名变量会直接编译报错（"重新声明了常量 rotation"）。
        let legRotation = JsMath.Axis.ZP.rotation(bindZ)
        legRotation.mul(JsMath.Axis.YP.rotation(bindY))
        legRotation.mul(JsMath.Axis.XP.rotation(deployX))

        bone.rotation.set(legRotation)
        bone.rotationInEuler.x = deployX
    }
}
