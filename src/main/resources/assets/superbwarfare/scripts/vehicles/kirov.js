function transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks, renderer) {
    if (vehicle.isWreck()) return

    const math = this.JsMath
    rotatePropeller(model, "move_prop1", math.lerp(partialTicks, vehicle.propellerO, vehicle.propeller))
    rotatePropeller(model, "move_prop2", math.lerp(partialTicks, vehicle.propellerLO, vehicle.propellerL))
    rotatePropeller(model, "move_prop3", math.lerp(partialTicks, vehicle.propellerRO, vehicle.propellerR))
    const vertical = math.lerp(partialTicks, vehicle.propellerVO, vehicle.propellerV)
    rotatePropeller(model, "move_prop4", vertical)
    rotatePropeller(model, "move_prop5", vertical)

    const turret = model.getBone("move_turret_right")
    if (turret != null) {
        turret.rotation.rotationY(renderer.turretYRot * Math.PI / 180)
        turret.visible = true
    }

    const control = model.getBone("move_controlP")
    if (control != null) control.rotation.rotationX(math.clamp(-48 * vehicle.power, -20, 20) * Math.PI / 180)

    const rudder = model.getBone("move_rudder")
    if (rudder != null) rudder.rotation.rotationZ(12 * math.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot))
}

function rotatePropeller(model, name, angle) {
    const bone = model.getBone(name)
    if (bone != null) bone.rotation.rotateZ(angle)
}
