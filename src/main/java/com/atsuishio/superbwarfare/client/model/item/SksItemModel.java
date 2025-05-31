package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.rifle.SksItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class SksItemModel extends CustomGunModel<SksItem> {

    @Override
    public ResourceLocation getAnimationResource(SksItem animatable) {
        return Mod.loc("animations/sks.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(SksItem animatable) {
        return Mod.loc("geo/sks.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SksItem animatable) {
        return Mod.loc("textures/item/sks.png");
    }

    @Override
    public void setCustomAnimations(SksItem animatable, long instanceId, AnimationState<SksItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone bolt = getAnimationProcessor().getBone("bolt");
        CoreGeoBone shuan = getAnimationProcessor().getBone("bolt2");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        double fpz = ClientEventHandler.firePosZ * 13 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        gun.setPosX(1.53f * (float) zp);
        gun.setPosY(0.34f * (float) zp - (float) (0.6f * zpz));
        gun.setPosZ(2.5f * (float) zp + (float) (0.5f * zpz));
        gun.setRotZ((float) (0.05f * zpz));

        CoreGeoBone shen = getAnimationProcessor().getBone("shen");

        shen.setPosX((float) (0.95f * ClientEventHandler.recoilHorizon * fpz * fp));
        shen.setPosY((float) (0.2f * fp + 0.24f * fr));
        shen.setPosZ((float) (0.825 * fp + 0.34f * fr + 1.35 * fpz));
        shen.setRotX((float) (0.01f * fp + 0.05f * fr + 0.01f * fpz));
        shen.setRotY((float) (0.1f * ClientEventHandler.recoilHorizon * fpz));
        shen.setRotZ((float) ((0.08f + 0.1 * fr) * ClientEventHandler.recoilHorizon));

        shen.setPosX((float) (shen.getPosX() * (1 - 0.5 * zt)));
        shen.setPosY((float) (shen.getPosY() * (-1 + 0.5 * zt)));
        shen.setPosZ((float) (shen.getPosZ() * (1 - 0.6 * zt)));
        shen.setRotX((float) (shen.getRotX() * (1 - 0.9 * zt)));
        shen.setRotY((float) (shen.getRotY() * (1 - 0.9 * zt)));
        shen.setRotZ((float) (shen.getRotZ() * (1 - 0.9 * zt)));

        CrossHairOverlay.gunRot = shen.getRotZ();

        shuan.setPosZ(2f * (float) fp);

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.92 * zt);
        float numP = (float) (1 - 0.88 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());

        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 0.7f, 1.2f);
        CoreGeoBone shell = getAnimationProcessor().getBone("shell");

        if (GunData.from(stack).holdOpen.get()) {
            shell.setScaleX(0);
            shell.setScaleY(0);
            shell.setScaleZ(0);
            bolt.setPosZ(2.5f);
        } else {
            shell.setScaleX(1);
            shell.setScaleY(1);
            shell.setScaleZ(1);
        }
    }
}
