package com.atsuishio.superbwarfare.client.model.item

import com.atsuishio.superbwarfare.client.animation.AnimationHelper
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.item.gun.launcher.SuperStarShooterItem
import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import software.bernie.geckolib.core.animation.AnimationState

class SuperStarShooterItemModel : CustomGunModel<SuperStarShooterItem>() {

    override fun setCustomAnimations(
        animatable: SuperStarShooterItem,
        instanceId: Long,
        animationState: AnimationState<SuperStarShooterItem>
    ) {
        val player = Minecraft.getInstance().player ?: return
        val stack = player.mainHandItem
        if (shouldCancelRender(stack, animationState)) return

        val gun = animationProcessor.getBone("bone")
        val fireRoot = animationProcessor.getBone("fireRoot")

        val zt = ClientEventHandler.zoomTime
        val zp = ClientEventHandler.zoomPos
        val zpz = ClientEventHandler.zoomPosZ

        gun.posX = -0.046f * zp.toFloat()
        gun.posY = 4.1f * zp.toFloat() - (0.2f * zpz).toFloat()
        gun.posZ = 8f * zp.toFloat() + (0.5f * zpz).toFloat()
        gun.scaleZ = 1f - (0.5f * zp.toFloat())

        ClientEventHandler.handleShootAnimation(fireRoot, 1.25f, 2f, 3f, 0.5f, 0.3f, 0.1f, 0.4f, 0.55f)

        CrossHairOverlay.gunRot = fireRoot.rotZ

        ClientEventHandler.gunRootMove(animationProcessor, 0f, 0f, 0f, false)

        val camera = animationProcessor.getBone("camera")
        val main = animationProcessor.getBone("0")

        val numR = (1 - 0.82 * zt).toFloat()
        val numP = (1 - 0.78 * zt).toFloat()

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP)
        ClientEventHandler.handleReloadShake((Mth.RAD_TO_DEG * camera.rotX).toDouble(), (Mth.RAD_TO_DEG * camera.rotY).toDouble(), (Mth.RAD_TO_DEG * camera.rotZ).toDouble())
    }
}
