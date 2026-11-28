function transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks) {
    if (vehicle.isWreck()) return

    const math = this.JsMath
    const radians = Math.PI / 180
    const pitchLeft = math.lerp(partialTicks, vehicle.flap2LRotO, vehicle.flap2LRot) * radians
    const pitchRight = math.lerp(partialTicks, vehicle.flap2RRotO, vehicle.flap2RRot) * radians
    rotateX(model, "move_wingFL", -1.5 * pitchRight)
    rotateX(model, "move_wingFR", -1.5 * pitchRight)
    rotateX(model, "move_tailWingHL", pitchLeft)
    rotateX(model, "move_tailWingHR", pitchRight)

    const tail = model.getBone("move_tailWingV")
    if (tail != null) {
        tail.rotation.rotateY(math.clamp(math.lerp(partialTicks, vehicle.flap3RotO, vehicle.flap3Rot), -20, 20) * radians)
    }

    const rotation = math.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot)
    for (var i = 1; i <= 4; i++) {
        var propeller = model.getBone("move_prop" + i)
        if (propeller != null) propeller.rotation.rotateZ(rotation)
    }

    const gun = model.getBone("move_gd")
    if (gun != null) gun.visible = !this.VehicleRender.isArmedPassengerInFirstPerson(vehicle)
}

function rotateX(model, name, angle) {
    const bone = model.getBone(name)
    if (bone != null) bone.rotation.rotateX(angle)
}
