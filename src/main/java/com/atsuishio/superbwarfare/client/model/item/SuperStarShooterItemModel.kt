package com.atsuishio.superbwarfare.client.model.item

import com.atsuishio.superbwarfare.item.gun.launcher.SuperStarShooterItem
import software.bernie.geckolib.core.animation.AnimationState

class SuperStarShooterItemModel : CustomGunModel<SuperStarShooterItem>() {

    // TODO 正确实现animation
    override fun setCustomAnimations(
        animatable: SuperStarShooterItem,
        instanceId: Long,
        animationState: AnimationState<SuperStarShooterItem>
    ) {
//        val player = localPlayer ?: return
//        val stack = player.mainHandItem
//        if (shouldCancelRender(stack, animationState)) return
//
//        val gun = animationProcessor.getBone("bone")
//        val shen = animationProcessor.getBone("gun")
//
//        val zt = ClientEventHandler.zoomTime
//        val zp = ClientEventHandler.zoomPos
//        val zpz = ClientEventHandler.zoomPosZ
//
//        gun.posX = 2.2f * zp.toFloat()
//
//        gun.posY = 0.8f * zp.toFloat() - (0.7f * zpz).toFloat()
//
//        gun.posZ = zp.toFloat() + (0.6f * zpz).toFloat()
//
//        gun.rotZ = (0.05f * zpz).toFloat()
//
//        ClientEventHandler.handleShootAnimation(shen, 1.25f, 1.7f, 2f, 2.5f, 1.3f, 1f, 0.4f, 0.55f)
//
//        CrossHairOverlay.gunRot = shen.rotZ
//
//        ClientEventHandler.gunRootMove(animationProcessor, 0f, 0f, 0f, false)
//
//        val camera = animationProcessor.getBone("camera")
//        val main = animationProcessor.getBone("0")
//
//        val numR = (1 - 0.58 * zt).toFloat()
//        val numP = (1 - 0.58 * zt).toFloat()
//
//        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP)
//        ClientEventHandler.handleReloadShake(
//            (Mth.RAD_TO_DEG * camera.rotX).toDouble(),
//            (Mth.RAD_TO_DEG * camera.rotY).toDouble(),
//            (Mth.RAD_TO_DEG * camera.rotZ).toDouble()
//        )
    }
}
