const CHARGE_BAR_BONE = "charge_bar"
const LIGHTNING_MODE_BONE = "lightning_mode"
const LASER_MODE_BONE = "laser_mode"

const LIGHTNING_MODE = 1
const LASER_MODE = 2

const CHARGE_BAR_RATE = 0.9
const CHARGE_BAR_EPSILON = 0.002

function transformCustomModelPart(stack, model, transformType, partialTick, renderer) {
    const progress = renderer.scriptChargeProgress(stack)
    const rate = Math.min(renderer.scriptFrameDeltaSeconds() * CHARGE_BAR_RATE, 1)
    const scale = JsState.smooth(stack, "chargeBar", progress, rate, CHARGE_BAR_EPSILON)

    const showing = transformType.firstPerson() && progress > 0
    const ammoType = renderer.scriptAmmoType(stack)

    const bar = model.getBone(CHARGE_BAR_BONE)
    if (bar != null) {
        bar.visible = showing
        bar.zScale = showing ? scale : 0
    }

    const lightning = model.getBone(LIGHTNING_MODE_BONE)
    if (lightning != null) {
        lightning.visible = showing && ammoType === LIGHTNING_MODE
    }

    const laser = model.getBone(LASER_MODE_BONE)
    if (laser != null) {
        laser.visible = showing && ammoType === LASER_MODE
    }
}
