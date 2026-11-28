function transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks, renderer) {
    const math = this.JsMath
    const pitch = math.clamp(-renderer.turretXRot, vehicle.turretMinPitch, vehicle.turretMaxPitch) * Math.PI / 180
    const angle = vehicle.lockTurret ? 0 : pitch

    const barrel = model.getBone("barrel")
    if (barrel != null) barrel.rotation.rotationX(angle)

    const sine = Math.sin(pitch)
    const cosine = Math.cos(pitch)
    const hydraulicAngle = Math.atan2(11.8113, -14.0761) - Math.atan2(
        32.1847 * sine + 9.4012 * cosine + 2.4101,
        -32.1847 * cosine + 9.4012 * sine + 18.1086
    )
    const hydraulicRod = model.getBone("move_yeyagan")
    const hydraulicCylinder = model.getBone("move_yeya")
    if (hydraulicRod != null) hydraulicRod.rotation.rotationX(hydraulicAngle)
    if (hydraulicCylinder != null) hydraulicCylinder.rotation.rotationX(hydraulicAngle - angle)

    const control = model.getBone("move_control")
    if (control != null) control.rotation.rotationY(12 * math.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot))

    const hideGun = this.VehicleRender.isSeatInFirstPerson(vehicle, 2)
    renderer.hideFlare = hideGun
    const gun = model.getBone("move_hmg")
    if (gun != null) gun.visible = !hideGun
}
