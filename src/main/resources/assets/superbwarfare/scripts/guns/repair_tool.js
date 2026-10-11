const FLAME_BONE = "flame"
const FLAME_FRAME_COUNT = 8

function transformCustomModelPart(stack, model, transformType, partialTick, renderer) {
    const firstPerson = transformType.firstPerson()
    const frame = firstPerson ? Math.floor(renderer.scriptGameTime()) % FLAME_FRAME_COUNT : -1

    const flame = model.getBone(FLAME_BONE)
    if (flame != null) {
        flame.visible = firstPerson
    }

    for (let i = 0; i < FLAME_FRAME_COUNT; i++) {
        let bone = model.getBone(FLAME_BONE + "_" + i)
        if (bone == null) {
            continue
        }

        bone.visible = i === frame
    }
}
