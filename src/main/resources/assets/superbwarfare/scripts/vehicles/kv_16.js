function transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks, renderer) {
    const root = model.getBone("root")
    if (root != null) {
        root.visible = !(renderer.getHideForTurretControllerWhileZooming() && vehicle.getWeaponIndex(0) === 1)
    }

    if (vehicle.isWreck()) return

    const propeller = model.getBone("move_propeller")
    if (propeller != null) {
        propeller.rotation.rotateZ(this.JsMath.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot))
    }
}
