package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.launcher.JavelinItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class JavelinItemModel extends CustomGunModel<JavelinItem> {

    @Override
    public void setCustomAnimations(JavelinItem animatable, long instanceId, AnimationState<JavelinItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone shen = getAnimationProcessor().getBone("shen");

        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        gun.setPosX(1.66f * (float) zp + (float) (0.2f * zpz));
        gun.setPosY(5.5f * (float) zp + (float) (0.8f * zpz));
        gun.setPosZ(15.9f * (float) zp);
        gun.setScaleZ(1f - (0.8f * (float) zp));
        gun.setRotZ(-4.75f * Mth.DEG_TO_RAD * (float) zp + (float) (0.02f * zpz));

        ClientEventHandler.handleShootAnimation(shen, 1.25f, 1.7f, 2f, 2.5f, 1.3f, 1f, 0.4f, 0.55f);

        CrossHairOverlay.gunRot = shen.getRotZ();

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 4, 0, 2, false);

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
