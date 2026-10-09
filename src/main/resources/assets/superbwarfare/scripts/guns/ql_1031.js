const SIGHT_FOLD_BONES = ["sight1fold", "sight2fold"]
const SIGHT_FOLD_DEG = 90
const SIGHT_FOLD_RATE = 0.9
const SIGHT_FOLD_EPSILON = 0.01
const ENERGY_BAR_BONE = "energy_bar"
const HUD_BONES = ["front_hud_pos_illuminated", "back_hud_pos_illuminated"]
const HUD_SHOW_ZOOM = 0.8
const BIPOD_BONES = ["bipod_l", "bipod_r"]

const BIPOD_DEPLOY_X_DEG = -90

const CHARGE_BAR_PREFIX = "charge_bar_"
// 40 段（一边 20 段，一档 4.5°）：比蓄力模式 20 tick 的 `Duration` 细一倍，是为了让弧走得平滑
const CHARGE_BAR_SEGMENTS = 40

function transformCustomModelPart(stack, model, transformType, partialTick, renderer) {
    const energyBone = model.getBone(ENERGY_BAR_BONE)
    if (energyBone != null) {
        const firstPerson = transformType.firstPerson()
        energyBone.visible = firstPerson

        if (firstPerson) {
            energyBone.xScale = renderer.scriptEnergyRatio(stack)
        }
    }

    const hudVisible = !renderer.scriptHasScope(stack)
        && renderer.scriptZoomTime(stack) > HUD_SHOW_ZOOM
    for (let i = 0; i < HUD_BONES.length; i++) {
        let hudBone = model.getBone(HUD_BONES[i])
        if (hudBone == null) {
            continue
        }

        hudBone.visible = hudVisible
    }

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

    // 后准星上的蓄力进度条：按蓄力点亮 `charge_bar_1`..`charge_bar_40` 里的前若干段。
    //
    // 段号是**按离正下方的距离**排的（1、2 是中点两侧那两段，见 build/verify/gen_charge_bar.py），
    // 所以"点亮前 N 段"得到的就是以正下方为中点、向两侧对称长出来的弧，和参照图一致。
    //
    // ⚠ 必须每帧都写、包括不亮的那一支：resetPose() 每帧会把 visible 复位成 true，只在亮起来
    // 的那几帧里写，其余时间整条弧都会露着。模型那一遍本就按白色画所有可见骨骼，所以这里
    // 除了 visible 什么都不用给——白色就是"不上色"。
    const chargeProgress = renderer.scriptChargeProgress(stack)
    const litChargeSegments = Math.ceil(chargeProgress * CHARGE_BAR_SEGMENTS)

    for (let i = 1; i <= CHARGE_BAR_SEGMENTS; i++) {
        let chargeBone = model.getBone(CHARGE_BAR_PREFIX + i)
        if (chargeBone == null) {
            continue
        }

        chargeBone.visible = i <= litChargeSegments
    }

    const progress = renderer.scriptBipodProgress(stack)
    if (progress <= 0) {
        return
    }

    const deployX = BIPOD_DEPLOY_X_DEG * JsMath.DEG_TO_RAD * progress

    for (let i = 0; i < BIPOD_BONES.length; i++) {
        let bone = model.getBone(BIPOD_BONES[i])
        if (bone == null) {
            continue
        }

        let bindY = bone.rotationInEuler.y
        let bindZ = bone.rotationInEuler.z

        let legRotation = JsMath.Axis.ZP.rotation(bindZ)
        legRotation.mul(JsMath.Axis.YP.rotation(bindY))
        legRotation.mul(JsMath.Axis.XP.rotation(deployX))

        bone.rotation.set(legRotation)
        bone.rotationInEuler.x = deployX
    }
}
