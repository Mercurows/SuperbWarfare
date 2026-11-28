function transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks) {
    const steering = this.JsMath.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot)
    const control = model.getBone("move_control")
    if (control != null) control.rotation.rotationZ(8 * steering)

    const head = model.getBone("move_head")
    if (head != null) {
        const pitch = -5 * vehicle.getAcceleration()
        const roll = 0.5 * steering * this.VehicleRender.getHorizontalSpeed(vehicle)
        head.rotation.set(head.definition().bindRotation())
        head.rotation.rotateX(pitch)
        head.rotation.rotateZ(roll)
    }

    const ammo = this.VehicleRender.getLoadedAmmo(vehicle)
    if (ammo != null) {
        const shells = model.boneGroups.shell
        for (var i = 0; i < shells.size(); i++) {
            shells.get(i).visible = i < ammo.size() && ammo.get(i) != -1
        }
    }

    const sight = model.getBone("move_guanmiao")
    if (sight != null) sight.visible = !this.VehicleRender.isTurretControllerInFirstPerson(vehicle)
}
