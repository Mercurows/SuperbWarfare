function transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks, renderer) {
    const pitchWheel = model.getBone("move_shoulunx")
    const yawWheel = model.getBone("move_shouluny")
    if (pitchWheel != null && yawWheel != null) {
        pitchWheel.rotation.rotationX(-3 * renderer.turretXRot)
        yawWheel.rotation.rotationZ(6 * renderer.turretYRot)
    }

    const ammo = this.VehicleRender.getLoadedAmmo(vehicle)
    if (ammo == null) return
    const shells = model.boneGroups.shell
    for (var i = 0; i < shells.size(); i++) {
        shells.get(i).visible = i < ammo.size() && ammo.get(i) != -1
    }
}
