package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.sniper.MosinNagantItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class MosinNagantItemModel extends CustomGunModel<MosinNagantItem> {

    @Override
    public void setCustomAnimations(MosinNagantItem animatable, long instanceId, AnimationState<MosinNagantItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        GeoBone gun = getAnimationProcessor().getBone("bone");
        GeoBone shen = getAnimationProcessor().getBone("shen");
        GeoBone pu = getAnimationProcessor().getBone("pu");
        GeoBone bone15 = getAnimationProcessor().getBone("bone15");
        GeoBone bone16 = getAnimationProcessor().getBone("bone16");
        GeoBone qiangshen = getAnimationProcessor().getBone("qiangshen");

        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        gun.setPosX(2.105f * (float) zp);
        gun.setPosY(0.766f * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(12.95f * (float) zp + (float) (0.3f * zpz));
        gun.setScaleZ(1f - (0.9f * (float) zp));

        pu.setScaleZ(1f - (0.5f * (float) zp));
        bone16.setScaleZ(1f - (0.93f * (float) zp));
        bone15.setScaleX(1f - (0.2f * (float) zp));

        if (gun.getPosX() > 1.4) {
            qiangshen.setScaleX(0);
            qiangshen.setScaleY(0);
            qiangshen.setScaleZ(0);
        } else {
            qiangshen.setScaleX(1);
            qiangshen.setScaleY(1);
            qiangshen.setScaleZ(1);
        }

        ClientEventHandler.handleShootAnimation(shen, 0.5f, 2f, 3f, 2.5f, 0.3f, 0.5f, 0.4f, 0.45f);

        CrossHairOverlay.gunRot = shen.getRotZ();
        ClientEventHandler.gunRootMove(getAnimationProcessor(), 0, 0, 0, false);

        GeoBone camera = getAnimationProcessor().getBone("camera");
        GeoBone main = getAnimationProcessor().getBone("0");
        GeoBone body = getAnimationProcessor().getBone("roll");

        float numR = (float) (1 - 0.97 * zt);
        float numP = (float) (1 - 0.81 * zt);

        var data =GunData.from(stack);
        if (data.reloading() || data.bolt.actionTimer.get() > 0) {
            main.setRotX(numR * main.getRotX());
            main.setRotY(numR * main.getRotY());
            main.setRotZ(numR * main.getRotZ());
            main.setPosX(numP * main.getPosX());
            main.setPosY(numP * main.getPosY());
            main.setPosZ(numP * main.getPosZ());
            body.setRotX(numR * body.getRotX());
            body.setRotY(numR * body.getRotY());
            body.setRotZ(numR * body.getRotZ());
            body.setPosX(numP * body.getPosX());
            body.setPosY(numP * body.getPosY());
            body.setPosZ(numP * body.getPosZ());
            camera.setRotX(numR * camera.getRotX());
            camera.setRotY(numR * camera.getRotY());
            camera.setRotZ(numR * camera.getRotZ());
        }
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
