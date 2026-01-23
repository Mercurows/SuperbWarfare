package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.sniper.HuntingRifleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class HuntingRifleItemModel extends CustomGunModel<HuntingRifleItem> {

    @Override
    public void setCustomAnimations(HuntingRifleItem animatable, long instanceId, AnimationState<HuntingRifleItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone fireRoot = getAnimationProcessor().getBone("fireRoot");

        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        gun.setPosX(1.975f * (float) zp);
        gun.setPosY(1.2f * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(4f * (float) zp + (float) (0.5f * zpz));
        gun.setRotZ((float) (0.05f * zpz));
        gun.setScaleZ(1f - (0.5f * (float) zp));

        ClientEventHandler.handleShootAnimation(fireRoot, 0.5f, 2f, 3f, 2.5f, 0.3f, 0.5f, 0.4f, 0.45f);

        CrossHairOverlay.gunRot = fireRoot.getRotZ();

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 0, 0, 0, false);

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.82 * zt);
        float numP = (float) (1 - 0.78 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
