/**
 * Insidious 的两处自定义效果：
 *
 * 1. `rot1` / `rot2` 两根骨骼绕 Z 轴匀速反向自转——模型里它们是**双螺旋**：
 *    各 17 片 1.2×1.2×0.3 的薄板，全都以 (x=0, y=3.2) 为中心、沿 Z 从 -4.3 排到 -14.9，
 *    每片再各自绕 Z 转 45°→-175°（两片相差 0.14 单位交错咬合）。所以只有绕 **Z**（枪管轴线）转
 *    才是原地自转，绕 X / Y 会把整条螺旋甩出去。
 * 2. `sight_illuminated`（准星照明，一个 poly_mesh 发光片）在瞄准推进度超过门槛后才出现。
 */

const SPIN_BONES = ["rot1", "rot2"]

/**
 * 两根骨骼的自转方向，与 [SPIN_BONES] 一一对应。
 *
 * 符号按**玩家视角**定：枪口指向 -Z，所以玩家在 +Z 那一侧往回看，右手边是 +X、上方是 +Y；
 * 绕 +Z 的正角度把 +X 转向 +Y，看上去是逆时针。于是 rot1 顺时针取负、rot2 逆时针取正。
 * 想整体反过来（或者从枪口那头看过去是对的）就把这两个符号一起取反。
 */
const SPIN_SIGNS = [-1, 1]

/** 自转速度，度每秒。 */
const SPIN_DEG_PER_SECOND = 90

/**
 * 每 tick 转过的角度。`scriptGameTime()` 的单位是 tick（20 tick = 1 秒），这里换算一次。
 * ⚠ 不要拿 `scriptFrameDeltaSeconds()` 来推：那个名字是历史遗留，它返回的其实是
 * `Minecraft.deltaFrameTime`，单位同样是 tick（它自己的 clamp 上限 0.8 也是照 tick 写的）。
 */
const SPIN_DEG_PER_TICK = SPIN_DEG_PER_SECOND / 20

const SIGHT_BONE = "sight_illuminated"

/**
 * 瞄准推进度超过这个值才显示准星照明。
 *
 * 刻度是 `zoomTime` 的**线性**原值（0 腰射 → 1 完全瞄准），不是 `aimingProgress` 那条缓动曲线上的值。
 */
const SIGHT_ILLUMINATED_ZOOM = 0.3

function transformCustomModelPart(stack, model, transformType, partialTick, renderer) {
    // 自转角度**只是时间的函数**，不需要 JsState 记住上一帧转到哪。
    // 顶层变量是全世界同型号枪共用的一份；JsState 又按 ItemStack 对象身份记，而服务端每次同步手持槽
    // （开枪改弹药、热量、计时器）都会把客户端那把枪换成新对象，攒下来的角度会被反复清零。
    // 算成时间的函数还顺带免疫"同一帧被画几次就走几倍"。
    //
    // 先对 360 取余：世界跑久了 gameTime 是天文数字，几百万度的角度丢进 float 的四元数里会抖。
    const spinDeg = (renderer.scriptGameTime() * SPIN_DEG_PER_TICK) % 360

    // 注意：循环体内必须用 let。这个 Rhino 版本的 const 不是块级作用域，
    // 循环里重复声明会保留上一轮的绑定。
    for (let i = 0; i < SPIN_BONES.length; i++) {
        let bone = model.getBone(SPIN_BONES[i])
        if (bone == null) {
            // LOD 模型里只有一根 bone2，没有这两根，远端渲染直接跳过
            continue
        }

        // 这两根骨骼自己都没有 bind 旋转（角度写在方块上），所以直接覆盖即可，不用像脚架那样
        // 把 bind 的 Y / Z 重新乘回去。
        let deg = spinDeg * SPIN_SIGNS[i]

        bone.rotation.set(JsMath.Axis.ZP.rotationDegrees(deg))
        // rotation 只管画，动画系统读回去的是 rotationInEuler，两个都得写
        bone.rotationInEuler.z = deg * JsMath.DEG_TO_RAD
    }

    const sight = model.getBone(SIGHT_BONE)
    if (sight != null) {
        // 每帧都要写：renderer 每帧都会 resetPose 把 visible 拨回 true。
        // scriptZoomTime 只对**本地玩家自己手里**那把枪返回非 0，所以世界上的同型号枪不会跟着一起亮。
        sight.visible = renderer.scriptZoomTime(stack) > SIGHT_ILLUMINATED_ZOOM
    }
}
