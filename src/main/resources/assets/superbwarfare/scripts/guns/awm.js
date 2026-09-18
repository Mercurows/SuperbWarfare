const BIPOD_BONES = ["bipod_l", "bipod_r"]

/**
 * 展开时两腿绕 X 轴转过的角度。
 *
 * 模型里两脚架平放着指向 -Z（枪口方向），所以只有负角度会把它们向下翻到 -Y。
 * 与旧 GeckoLib 实现里的 `setRotX(-90)` 是同一个坐标系约定，数值可以直接沿用。
 */
const BIPOD_DEPLOY_X_DEG = -90

function transformCustomModelPart(stack, model, transformType, partialTick, renderer) {
    const progress = renderer.scriptBipodProgress()
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

        // 两条腿各自带 ±22.5° 的 Z 轴外张角，必须一并保留，否则展开后两腿会并拢成一条
        let bindY = bone.rotationInEuler.y
        let bindZ = bone.rotationInEuler.z

        // 等价于模型加载时的 rotateZYX(z, y, x)，即 Rz * Ry * Rx
        let rotation = JsMath.Axis.ZP.rotation(bindZ)
        rotation.mul(JsMath.Axis.YP.rotation(bindY))
        rotation.mul(JsMath.Axis.XP.rotation(deployX))

        bone.rotation.set(rotation)
        bone.rotationInEuler.x = deployX
    }
}
