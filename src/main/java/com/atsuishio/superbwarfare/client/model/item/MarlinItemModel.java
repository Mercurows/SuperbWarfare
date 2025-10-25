package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.rifle.MarlinItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class MarlinItemModel extends CustomGunModel<MarlinItem> {

    @Override
    public void setCustomAnimations(MarlinItem animatable, long instanceId, AnimationState<MarlinItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone shen = getAnimationProcessor().getBone("shen");
        CoreGeoBone jichui = getAnimationProcessor().getBone("jichui");

        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        gun.setPosX(1.712f * (float) zp);
        gun.setPosY(1.06f * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(3f * (float) zp + (float) (0.2f * zpz));
        gun.setRotZ((float) (0.02f * zpz));
        gun.setScaleZ(1f - (0.2f * (float) zp));

        ClientEventHandler.handleShootAnimation(shen, 1.25f, 1.7f, 2f, 2.5f, 1.3f, 1f, 0.4f, 0.55f);

        CrossHairOverlay.gunRot = shen.getRotZ();

        if (GunData.from(stack).closeStrike.get()) {
            jichui.setRotX(-0.52f);
        }

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 0, -1, 2, false);

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.55 * zt);
        float numP = (float) (1 - 0.88 * zt);

        if (GunData.from(stack).reloading()) {
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
    }
}
