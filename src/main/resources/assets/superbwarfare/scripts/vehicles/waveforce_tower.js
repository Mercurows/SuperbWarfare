function transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks, renderer) {
    var Axis = this.Axis
    var Quaterniond = this.Quaterniond
    var Quaternionf = this.Quaternionf
    var Mth = this.Mth

    var glow = model.getBone("glow")
    var scale = Math.min(Mth.lerp(partialTicks, vehicle.laserScaleO, vehicle.laserScale), 1.2)

    glow.xScale = scale
    glow.yScale = scale
    glow.zScale = scale

    var glow2 = model.getBone("glow2")

    glow2.z = -16 * vehicle.laserLength - 2.5

    glow2.xScale = scale
    glow2.yScale = scale
    glow2.zScale = scale

    var charge = model.getBone("charge")
    var energy = vehicle.chargeProgress
    var energyRate0 = renderer.getEnergy0()
    charge.zScale = Mth.lerp(partialTicks, energyRate0, energy)
    renderer.setEnergy0(energy)

    for (let i = 1; i <= 7; i++) {
        var boneName = "light_on" + i
        var bone = model.getBone(boneName)

        if (bone != null) {
            bone.visible = energy >= (i / 7.0)
        }
    }
}