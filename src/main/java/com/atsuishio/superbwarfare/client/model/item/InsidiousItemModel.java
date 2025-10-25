package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.rifle.InsidiousItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class InsidiousItemModel extends CustomGunModel<InsidiousItem> {

    @Override
    public void setCustomAnimations(InsidiousItem animatable, long instanceId, AnimationState<InsidiousItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");

        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        gun.setPosX(4.23f * (float) zp);
        gun.setPosY(1.28f * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(6.2f * (float) zp + (float) (0.5f * zpz));
        gun.setScaleZ(1f - (0.82f * (float) zp));

        CoreGeoBone shen = getAnimationProcessor().getBone("fireRootNormal");

        ClientEventHandler.handleShootAnimation(shen, 1, -2, 1, 1, 1, 1, 0.75f, 0.8f);

        CrossHairOverlay.gunRot = shen.getRotZ();
        ClientEventHandler.gunRootMove(getAnimationProcessor(), 5, 0, 0, false);

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.98 * zt);
        float numP = (float) (1 - 0.92 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
