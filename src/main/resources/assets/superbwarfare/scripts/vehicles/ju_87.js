function transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks) {
    const root = model.getBone("root")
    if (root != null) {
        const weapon = vehicle.getWeaponIndex(0)
        root.visible = !(this.VehicleRender.isPilotZooming(vehicle) && (weapon === 1 || weapon === 2))
    }

    if (vehicle.isWreck()) return

    const math = this.JsMath
    const radians = Math.PI / 180
    const left = math.lerp(partialTicks, vehicle.flap2LRotO, vehicle.flap2LRot) * radians
    const right = math.lerp(partialTicks, vehicle.flap2RRotO, vehicle.flap2RRot) * radians
    rotateX(model, "move_wingLR", 1.5 * left)
    rotateX(model, "move_wingLR2", 1.5 * left)
    rotateX(model, "move_wingLR3", 1.5 * left)
    rotateX(model, "move_wingRR", 1.5 * right)
    rotateX(model, "move_wingRR2", 1.5 * right)
    rotateX(model, "move_wingRR3", 1.5 * right)
    rotateX(model, "move_wingLB", left)
    rotateX(model, "move_wingRB", right)
    rotateX(model, "move_breakerL", 2 * vehicle.planeBreak * radians)
    rotateX(model, "move_breakerR", 2 * vehicle.planeBreak * radians)

    const tail = model.getBone("move_tailWing")
    if (tail != null) {
        tail.rotation.rotateY(math.clamp(math.lerp(partialTicks, vehicle.flap3RotO, vehicle.flap3Rot), -20, 20) * radians)
    }

    const propeller = model.getBone("move_propeller")
    const propeller2 = model.getBone("move_propeller2")
    const propeller3 = model.getBone("move_propeller3")
    const sideRotation = math.lerp(partialTicks, vehicle.smallPropellerO, vehicle.smallPropeller)
    if (propeller != null) propeller.rotation.rotateZ(-math.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot))
    if (propeller2 != null) propeller2.rotation.rotateZ(-sideRotation)
    if (propeller3 != null) propeller3.rotation.rotateZ(sideRotation)
}

function rotateX(model, name, angle) {
    const bone = model.getBone(name)
    if (bone != null) bone.rotation.rotateX(angle)
}
