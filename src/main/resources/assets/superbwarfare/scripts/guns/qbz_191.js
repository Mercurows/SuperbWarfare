const SIGHT_FOLD_BONES = ["sight1fold", "sight2fold"]
let sightFoldDeg = 0

function transformCustomModelPart(stack, model, transformType, partialTick, renderer) {
    const targetDeg = renderer.scriptHasScope(stack) ? 90 : 0
    const delta = renderer.scriptFrameDeltaSeconds()
    const rate = Math.min(delta * 0.9, 1)

    sightFoldDeg = JsMath.lerp(rate, sightFoldDeg, targetDeg)
    if (Math.abs(sightFoldDeg - targetDeg) < 0.01) {
        sightFoldDeg = targetDeg
    }

    const rotation = JsMath.Axis.XP.rotationDegrees(sightFoldDeg)

    for (let i = 0; i < SIGHT_FOLD_BONES.length; i++) {
        let bone = model.getBone(SIGHT_FOLD_BONES[i])
        if (bone == null) {
            continue
        }

        bone.rotation.set(rotation)
        bone.rotationInEuler.x = sightFoldDeg * JsMath.DEG_TO_RAD
    }
}
