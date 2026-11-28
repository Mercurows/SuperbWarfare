function transformCustomModelPart(vehicle, model) {
    const sight = model.getBone("move_guanmiao")
    if (sight != null) sight.visible = !this.VehicleRender.isTurretControllerInFirstPerson(vehicle)

    const missile = model.getBone("move_missile")
    if (missile != null) missile.visible = this.VehicleRender.hasLoadedMissile(vehicle)
}
