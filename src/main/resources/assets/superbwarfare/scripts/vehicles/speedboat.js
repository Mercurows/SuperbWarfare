function transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks, renderer) {
    if (vehicle.isWreck()) return

    const rotation = this.JsMath.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot)
    const propeller = model.getBone("move_propeller")
    const propeller2 = model.getBone("move_propeller2")
    if (propeller != null) propeller.rotation.rotationZ(rotation)
    if (propeller2 != null) propeller2.rotation.rotationZ(-rotation)

    const turret = model.getBone("turret")
    if (turret != null) turret.visible = !renderer.getHideForTurretControllerWhileZooming()

    const steering = this.JsMath.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot)
    const control = model.getBone("move_control")
    const rudder = model.getBone("move_rudder")
    if (control != null) control.rotation.rotationZ(-4 * steering)
    if (rudder != null) rudder.rotation.rotationY(steering)
}
