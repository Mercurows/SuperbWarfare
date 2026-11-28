function transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks) {
    const steeringWheel = model.getBone("move_control")
    if (steeringWheel != null) {
        steeringWheel.rotation.rotationY(12 * this.JsMath.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot))
    }
}
