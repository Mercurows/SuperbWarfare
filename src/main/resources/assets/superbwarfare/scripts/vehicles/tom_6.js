function renderCustomPart(vehicle, model, poseStack, entityYaw, partialTicks, buffer, packedLight) {
    const render = this.VehicleRender
    if (!render.hasLoadedWeapon(vehicle, "MelonBomb")) return

    const scale = 0.85
    render.renderBlock("minecraft:melon", poseStack, buffer, packedLight, 0, 0.7 * scale, 0, scale)
}
