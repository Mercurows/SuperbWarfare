function transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks, renderer) {
    const root = model.getBone("root")
    if (root != null) {
        root.visible = !(renderer.getHideForTurretControllerWhileZooming() && vehicle.getWeaponIndex(0) === 2)
    }

    if (vehicle.isWreck()) return

    const math = this.JsMath
    const radians = Math.PI / 180
    rotateX(model, "move_wingLR", 1.5 * math.lerp(partialTicks, vehicle.flap1LRotO, vehicle.flap1LRot) * radians)
    rotateX(model, "move_wingRR", 1.5 * math.lerp(partialTicks, vehicle.flap1RRotO, vehicle.flap1RRot) * radians)
    rotateX(model, "move_wingLR2", 1.5 * math.lerp(partialTicks, vehicle.flap1L2RotO, vehicle.flap1L2Rot) * radians)
    rotateX(model, "move_wingRR2", 1.5 * math.lerp(partialTicks, vehicle.flap1R2RotO, vehicle.flap1R2Rot) * radians)
    rotateX(model, "move_wingLB", math.lerp(partialTicks, vehicle.flap2LRotO, vehicle.flap2LRot) * radians)
    rotateX(model, "move_wingRB", math.lerp(partialTicks, vehicle.flap2RRotO, vehicle.flap2RRot) * radians)

    const yaw = math.clamp(math.lerp(partialTicks, vehicle.flap3RotO, vehicle.flap3Rot), -20, 20) * radians
    const tailLeft = model.getBone("move_weiyiL")
    const tailRight = model.getBone("move_weiyiR")
    if (tailLeft != null) tailLeft.rotation.rotateY(yaw)
    if (tailRight != null) tailRight.rotation.rotateY(yaw)

    const rotation = math.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot)
    const wheel = model.getBone("move_qianzhou")
    const wheel2 = model.getBone("move_qianzhou2")
    if (wheel != null) wheel.rotation.rotateZ(rotation)
    if (wheel2 != null) wheel2.rotation.rotateZ(rotation)
}

function rotateX(model, name, angle) {
    const bone = model.getBone(name)
    if (bone != null) bone.rotation.rotateX(angle)
}
