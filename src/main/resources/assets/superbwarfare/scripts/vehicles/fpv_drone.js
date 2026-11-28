const propellerBones = ["move_wingFL", "move_wingFR", "move_wingBL", "move_wingBR"]

function transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks) {
    if (vehicle.isWreck()) return

    const rotation = this.JsMath.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot)
    for (var i = 0; i < propellerBones.length; i++) {
        var name = propellerBones[i]
        var bone = model.getBone(name)
        if (bone != null) {
            var reversed = name === "move_wingFR" || name === "move_wingBL"
            bone.rotation.rotationY(reversed ? -rotation : rotation)
        }
    }
}
