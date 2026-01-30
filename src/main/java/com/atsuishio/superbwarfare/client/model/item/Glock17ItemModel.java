package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.handgun.Glock17Item;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class Glock17ItemModel extends CustomGunModel<Glock17Item> {

    @Override
    public void setCustomAnimations(Glock17Item animatable, long instanceId, AnimationState<Glock17Item> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone bullet = getAnimationProcessor().getBone("bullet");

        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        gun.setPosX(1.23f * (float) zp);
        gun.setPosY(1.43f * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(7f * (float) zp + (float) (0.3f * zpz));
        gun.setScaleZ(1f - (0.55f * (float) zp));

        CoreGeoBone body = getAnimationProcessor().getBone("gun");

        ClientEventHandler.handleShootAnimation(body, 1.25f, -2f, 1.35f, 2.5f, 1.3f, 1f, 0.2f, 1.2f);

        CoreGeoBone huatao = getAnimationProcessor().getBone("huatao");
        huatao.setPosZ(1.5f * (float) ClientEventHandler.boltMove);
        var data = GunData.from(stack);
        if (data.holdOpen.get()) {
            huatao.setPosZ(1.5f);
        }

        CrossHairOverlay.gunRot = body.getRotZ();

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 4, 2, 3, false);

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.12 * zt);
        float numP = (float) (1 - 0.68 * zt);

        if (data.reload.time() > 0) {
            main.setRotX(numR * main.getRotX());
            main.setRotY(numR * main.getRotY());
            main.setRotZ(numR * main.getRotZ());
            main.setPosX(numP * main.getPosX());
            main.setPosY(numP * main.getPosY());
            main.setPosZ(numP * main.getPosZ());
            camera.setRotX(numR * camera.getRotX());
            camera.setRotY(numR * camera.getRotY());
            camera.setRotZ(numR * camera.getRotZ());
        }

        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 0.7f, 1f);

        CoreGeoBone shell = getAnimationProcessor().getBone("shell");
        CoreGeoBone barrel = getAnimationProcessor().getBone("guan");

        if (data.holdOpen.get()) {
            barrel.setRotX(4 * Mth.DEG_TO_RAD);
            bullet.setScaleX(0);
            bullet.setScaleY(0);
            bullet.setScaleZ(0);

            shell.setScaleX(0);
            shell.setScaleY(0);
            shell.setScaleZ(0);
        } else {
            barrel.setRotX(0);
            bullet.setScaleX(1);
            bullet.setScaleY(1);
            bullet.setScaleZ(1);

            shell.setScaleX(1);
            shell.setScaleY(1);
            shell.setScaleZ(1);
        }
    }
}
