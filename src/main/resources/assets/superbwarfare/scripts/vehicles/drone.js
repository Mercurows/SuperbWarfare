const propellerBones = ["move_wingFL", "move_wingFR", "move_wingBL", "move_wingBR"]

function transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks) {
    if (vehicle.isWreck()) return

    const rotation = this.JsMath.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot)
    for (var i = 0; i < propellerBones.length; i++) {
        var bone = model.getBone(propellerBones[i])
        if (bone != null) bone.rotation.rotationY(rotation)
    }
}
