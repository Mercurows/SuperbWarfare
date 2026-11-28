function transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks) {
    if (vehicle.isWreck()) return

    const rotation = this.JsMath.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot)
    const main = model.getBone("move_propeller")
    const tail = model.getBone("move_tailPropeller")
    if (main != null) main.rotation.rotateY(-rotation)
    if (tail != null) tail.rotation.rotateX(6 * rotation)
}
