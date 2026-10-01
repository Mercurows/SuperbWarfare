
const JAVELIN_HIDE_ROOT_ZOOM_OVER = 0.7

function transformCustomModelPart(stack, model, transformType, partialTick, renderer) {
    const root = model.getBone("root")
    if (root == null) {
        return
    }

    if (!transformType.firstPerson()) {
        root.visible = true
        return
    }

    root.visible = renderer.scriptZoomTime(stack) <= JAVELIN_HIDE_ROOT_ZOOM_OVER
}
