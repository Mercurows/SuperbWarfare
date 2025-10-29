package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.special.BocekItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class BocekItemModel extends CustomGunModel<BocekItem> {

    public static float rightHandPosZ;

    @Override
    public void setCustomAnimations(BocekItem animatable, long instanceId, AnimationState<BocekItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        GeoBone gun = getAnimationProcessor().getBone("bone");
        GeoBone shen = getAnimationProcessor().getBone("shen");
        GeoBone dRing = getAnimationProcessor().getBone("D_ring");
        GeoBone rightHand = getAnimationProcessor().getBone("safang");
        GeoBone leftHand = getAnimationProcessor().getBone("lh");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), 0.8);

        double pp = ClientEventHandler.bowPullPos;
        double pp2 = 1 - ClientEventHandler.bowPullPos;
        double zp = ClientEventHandler.zoomPos;
        double zp2 = 1 - ClientEventHandler.zoomPos;

        gun.setPosX((float) (0.2 * zp2 - 3 * pp2 * zp - 0.35 * pp + 0.35 * zp));
        gun.setPosY((float) (11f * zp + 3 * zp2 - 1 * pp2 * zp - 0.5 * zp));
        gun.setPosZ((float) (1.5f * zp + 2 * pp2));
        gun.setRotZ((float) (-45 * Mth.DEG_TO_RAD * zp2 + -5 * Mth.DEG_TO_RAD * pp2 * zp));
        gun.setScaleZ((float) (1f - (0.2f * zp)));

        leftHand.setRotY((float) (17.5 * Mth.DEG_TO_RAD * pp));

        if (ClientEventHandler.bowPull) {
            rightHandPosZ = dRing.getPosZ();
        } else {
            rightHandPosZ = Mth.lerp(0.06f * times, rightHandPosZ, 0);
        }

        GeoBone wing0 = getAnimationProcessor().getBone("wing0");
        GeoBone wing1 = getAnimationProcessor().getBone("wing1");
        GeoBone wing2 = getAnimationProcessor().getBone("wing2");
        GeoBone wing1Root = getAnimationProcessor().getBone("wing1Root");
        GeoBone wing2Root = getAnimationProcessor().getBone("wing2Root");

        float m = (float) Math.min(zp, pp);

        wingControl(wing0, m);
        wingControl(wing1, m);
        wingControl(wing2, m);
        wingControl(wing1Root, m);
        wingControl(wing2Root, m);

        GeoBone shake = getAnimationProcessor().getBone("shake");

        shake.setPosX((float) (shake.getPosX() * pp));
        shake.setPosY((float) (shake.getPosY() * pp));
        shake.setPosZ((float) (shake.getPosZ() * pp));

        rightHand.setPosZ(rightHandPosZ);

        CrossHairOverlay.gunRot = shen.getRotZ();
        ClientEventHandler.gunRootMove(getAnimationProcessor(), 0, 0, 0, true);

        GeoBone camera = getAnimationProcessor().getBone("camera");
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());

        ClientEventHandler.handleShootAnimation(shen, 0, 0, 0, 0, 0, 0, 0, 1f);
    }

    public static void wingControl(GeoBone GeoBone, float m) {
        GeoBone.setRotX(GeoBone.getRotX() * m);
        GeoBone.setRotY(GeoBone.getRotY() * m);
        GeoBone.setRotZ(GeoBone.getRotZ() * m);
    }
}
