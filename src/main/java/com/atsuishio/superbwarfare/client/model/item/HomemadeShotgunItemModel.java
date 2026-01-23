package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.shotgun.HomemadeShotgunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class HomemadeShotgunItemModel extends CustomGunModel<HomemadeShotgunItem> {

    @Override
    public void setCustomAnimations(HomemadeShotgunItem animatable, long instanceId, AnimationState<HomemadeShotgunItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone fireRoot = getAnimationProcessor().getBone("fireRoot");

        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        gun.setPosX(3.725f * (float) zp);
        gun.setPosY(1.5f * (float) zp - (float) (0.4f * zpz));
        gun.setPosZ(1.2f * (float) zp + (float) (0.3f * zpz));
        gun.setRotZ((float) (0.05f * zpz));

        ClientEventHandler.handleShootAnimation(fireRoot, 1.25f, -2f, 2.5f, 4.5f, 1.3f, 1f, 0.2f, 0.55f);

        CrossHairOverlay.gunRot = fireRoot.getRotZ();

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 1, 0, 0, false);

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.42 * zt);
        float numP = (float) (1 - 0.48 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
