package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.shotgun.Aa12Item;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class Aa12ItemModel extends CustomGunModel<Aa12Item> {

    @Override
    public void setCustomAnimations(Aa12Item animatable, long instanceId, AnimationState<Aa12Item> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone shen = getAnimationProcessor().getBone("shen");

        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        gun.setPosX(2.105f * (float) zp);
        gun.setPosY(0.17f * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(0.1f * (float) zp + (float) (0.3f * zpz));
        gun.setRotZ((float) (0.02f * zpz));
        gun.setScaleZ(1f - (0.4f * (float) zp));

        ClientEventHandler.handleShootAnimation(shen, 0.5f, 0.5f, 1f, 1f, 1, 1f, 0.4f, 0.55f);

        CrossHairOverlay.gunRot = shen.getRotZ();

        shen.setPosX(0.2f * (float) (ClientEventHandler.recoilHorizon * (0.5 + 0.4 * ClientEventHandler.fireSpread)));

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 0, 0, 0, false);

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");
        float numR = (float) (1 - 0.82 * zt);
        float numP = (float) (1 - 0.68 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 1f, 0.55f);
    }
}
