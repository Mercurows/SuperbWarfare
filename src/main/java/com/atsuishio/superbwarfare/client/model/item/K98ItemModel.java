package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.sniper.K98Item;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class K98ItemModel extends CustomGunModel<K98Item> {

    @Override
    public void setCustomAnimations(K98Item animatable, long instanceId, AnimationState<K98Item> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone shen = getAnimationProcessor().getBone("shen");
        CoreGeoBone clip = getAnimationProcessor().getBone("mag");

        var data = GunData.from(stack);
        if (data.reload.prepareTimer.get() > 11 && data.currentAvailableShots(player) == 1) {
            clip.setScaleX(0);
            clip.setScaleY(0);
            clip.setScaleZ(0);
        } else {
            clip.setScaleX(1);
            clip.setScaleY(1);
            clip.setScaleZ(1);
        }

        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        gun.setPosX(2.11f * (float) zp);
        gun.setPosY(1.52f * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(10f * (float) zp + (float) (0.3f * zpz));
        gun.setScaleZ(1f - (0.7f * (float) zp));

        ClientEventHandler.handleShootAnimation(shen, 0.5f, 2f, 3f, 2.5f, 0.3f, 0.5f, 0.4f, 0.45f);

        CrossHairOverlay.gunRot = shen.getRotZ();

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 0, 0, 0, false);

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");
        CoreGeoBone body = getAnimationProcessor().getBone("roll");

        float numR = (float) (1 - 0.52 * zt);
        float numP = (float) (1 - 0.58 * zt);

        if (data.reload.time() > 0 || data.reloading()) {
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
