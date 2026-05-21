function transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks, renderer) {
    var Axis = this.Axis
    var Quaterniond = this.Quaterniond
    var Quaternionf = this.Quaternionf
    var Mth = this.Mth

    var leftWheelRot = vehicle.leftWheelRot
    var rightWheelRot = vehicle.rightWheelRot

    // 固定轮子
    var leftWheels = model.leftWheels
    for (var i = 0; i < leftWheels.size(); i++) {
        leftWheels.get(i).rotation.rotationX(1.5 * leftWheelRot)
    }
    var rightWheels = model.rightWheels
    for (var i = 0; i < rightWheels.size(); i++) {
        rightWheels.get(i).rotation.rotationX(1.5 * rightWheelRot)
    }

    // 转向轮子
    var rudderRot = Mth.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot)
    var yawRot = Axis.YP.rotation(rudderRot)
    var leftTurn = model.leftWheelsTurn
    for (var i = 0; i < leftTurn.size(); i++) {
        var pitchRot = Axis.XP.rotation(1.5 * leftWheelRot)
        var quat = new Quaterniond(yawRot).mul(new Quaterniond(pitchRot))
        leftTurn.get(i).rotation.mul(new Quaternionf(quat))
    }
    var rightTurn = model.rightWheelsTurn
    for (var i = 0; i < rightTurn.size(); i++) {
        var pitchRot = Axis.XP.rotation(1.5 * rightWheelRot)
        var quat = new Quaterniond(yawRot).mul(new Quaterniond(pitchRot))
        rightTurn.get(i).rotation.mul(new Quaternionf(quat))
    }

    var root = model.getBone("root")
    if (root != null) {
        root.visible = !renderer.getHideForTurretControllerWhileZooming()
    }

    var turret = model.getBone("turret")
    if (turret != null) {
        turret.rotation.rotationY(renderer.getTurretYRot() * Mth.DEG_TO_RAD)
        turret.visible = !(vehicle.isWreck && vehicle.hasTurret() && vehicle.sympatheticDetonated)
    }
}