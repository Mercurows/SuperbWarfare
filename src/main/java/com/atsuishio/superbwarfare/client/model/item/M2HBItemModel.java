package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.machinegun.M2HBItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class M2HBItemModel extends CustomGunModel<M2HBItem> {

    @Override
    public void setCustomAnimations(M2HBItem animatable, long instanceId, AnimationState<M2HBItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone b1 = getAnimationProcessor().getBone("b1");
        CoreGeoBone b2 = getAnimationProcessor().getBone("b2");
        CoreGeoBone b3 = getAnimationProcessor().getBone("b3");
        CoreGeoBone b4 = getAnimationProcessor().getBone("b4");
        CoreGeoBone b5 = getAnimationProcessor().getBone("b5");

        int ammo = GunData.from(stack).ammo.get();
        boolean flag = GunData.from(stack).hideBulletChain.get();

        if (ammo < 5 && flag) {
            b5.setScaleX(0);
            b5.setScaleY(0);
            b5.setScaleZ(0);
        }

        if (ammo < 4 && flag) {
            b4.setScaleX(0);
            b4.setScaleY(0);
            b4.setScaleZ(0);
        }

        if (ammo < 3 && flag) {
            b3.setScaleX(0);
            b3.setScaleY(0);
            b3.setScaleZ(0);
        }

        if (ammo < 2 && flag) {
            b2.setScaleX(0);
            b2.setScaleY(0);
            b2.setScaleZ(0);
        }

        if (ammo < 1 && flag) {
            b1.setScaleX(0);
            b1.setScaleY(0);
            b1.setScaleZ(0);
        }

        CoreGeoBone shen = getAnimationProcessor().getBone("fireRootNormal");

        ClientEventHandler.handleShootAnimation(shen, 1.25f, 1.7f, 2f, 1.5f, 1.3f, 1f, 0f, 0.55f);

        CrossHairOverlay.gunRot = shen.getRotZ();

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 4, 0, 0, false);
        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 1f, 0.45f);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
