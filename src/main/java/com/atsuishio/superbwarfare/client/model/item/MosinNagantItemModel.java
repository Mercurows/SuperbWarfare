package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.sniper.MosinNagantItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class MosinNagantItemModel extends CustomGunModel<MosinNagantItem> {

    @Override
    public ResourceLocation getAnimationResource(MosinNagantItem animatable) {
        return Mod.loc("animations/mosin_nagant.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(MosinNagantItem animatable) {
        return Mod.loc("geo/mosin_nagant.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MosinNagantItem animatable) {
        return Mod.loc("textures/item/mosin_nagant.png");
    }

    @Override
    public void setCustomAnimations(MosinNagantItem animatable, long instanceId, AnimationState<MosinNagantItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone shen = getAnimationProcessor().getBone("shen");
        CoreGeoBone pu = getAnimationProcessor().getBone("pu");
        CoreGeoBone bone15 = getAnimationProcessor().getBone("bone15");
        CoreGeoBone bone16 = getAnimationProcessor().getBone("bone16");
        CoreGeoBone qiangshen = getAnimationProcessor().getBone("qiangshen");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        double fpz = ClientEventHandler.firePosZ * 7 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

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

        shen.setPosX((float) (0.95f * ClientEventHandler.recoilHorizon * fpz * fp));
        shen.setPosY((float) (0.4f * fp + 0.44f * fr));
        shen.setPosZ((float) (2.825 * fp + 0.17f * fr + 1.175 * fpz));
        shen.setRotX((float) (0.01f * fp + 0.15f * fr + 0.01f * fpz));
        shen.setRotY((float) (0.1f * ClientEventHandler.recoilHorizon * fpz));
        shen.setRotZ((float) ((0.08f + 0.1 * fr) * ClientEventHandler.recoilHorizon));

        shen.setPosX((float) (shen.getPosX() * (1 - 0.4 * zt)));
        shen.setPosY((float) (shen.getPosY() * (1 - 0.5 * zt)));
        shen.setPosZ((float) (shen.getPosZ() * (1 - 0.6 * zt)));
        shen.setRotX((float) (shen.getRotX() * (1 - 0.87 * zt)));
        shen.setRotY((float) (shen.getRotY() * (1 - 0.7 * zt)));
        shen.setRotZ((float) (shen.getRotZ() * (1 - 0.65 * zt)));

        CrossHairOverlay.gunRot = shen.getRotZ();
        ClientEventHandler.gunRootMove(getAnimationProcessor());

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");
        CoreGeoBone body = getAnimationProcessor().getBone("roll");

        float numR = (float) (1 - 0.97 * zt);
        float numP = (float) (1 - 0.81 * zt);

        if (GunData.from(stack).reloading() || GunData.from(stack).bolt.actionTimer.get() > 0) {
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
