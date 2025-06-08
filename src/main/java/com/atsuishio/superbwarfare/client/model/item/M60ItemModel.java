package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.machinegun.M60Item;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

public class M60ItemModel extends CustomGunModel<M60Item> {

    @Override
    public ResourceLocation getAnimationResource(M60Item animatable) {
        return Mod.loc("animations/m_60.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(M60Item animatable) {
        return Mod.loc("geo/m_60.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(M60Item animatable) {
        return Mod.loc("textures/item/m_60.png");
    }

    @Override
    public ResourceLocation getLODModelResource(M60Item animatable) {
        return Mod.loc("geo/lod/m_60.geo.json");
    }

    @Override
    public ResourceLocation getLODTextureResource(M60Item animatable) {
        return Mod.loc("textures/item/lod/m_60.png");
    }

    @Override
    public void setCustomAnimations(M60Item animatable, long instanceId, AnimationState<M60Item> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        GeoBone gun = getAnimationProcessor().getBone("bone");
        GeoBone shen = getAnimationProcessor().getBone("shen");
        GeoBone tiba = getAnimationProcessor().getBone("tiba");
        GeoBone b1 = getAnimationProcessor().getBone("b1");
        GeoBone b2 = getAnimationProcessor().getBone("b2");
        GeoBone b3 = getAnimationProcessor().getBone("b3");
        GeoBone b4 = getAnimationProcessor().getBone("b4");
        GeoBone b5 = getAnimationProcessor().getBone("b5");
        GeoBone l = getAnimationProcessor().getBone("l");
        GeoBone r = getAnimationProcessor().getBone("r");

        if (isProne(player)) {
            l.setRotX(1.5f);
            r.setRotX(1.5f);
        }

        var data = GunData.from(stack);
        int ammo = data.ammo.get();
        boolean flag = data.hideBulletChain.get();

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

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getTimer().getRealtimeDeltaTicks(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        double fpz = ClientEventHandler.firePosZ * 13 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        gun.setPosX(3.74f * (float) zp);

        gun.setPosY(-0.1f * (float) zp - (float) (0.1f * zpz));

        gun.setPosZ(3.24f * (float) zp + (float) (0.3f * zpz));

        gun.setRotZ(-0.087f * (float) zp + (float) (0.05f * zpz));

        shen.setPosX((float) (0.95f * ClientEventHandler.recoilHorizon * fpz * fp));
        shen.setPosY((float) (0.15f * fp + 0.18f * fr));
        shen.setPosZ((float) (0.325 * fp + 0.34f * fr + 0.75 * fpz));
        shen.setRotX((float) (0.01f * fp + 0.05f * fr + 0.01f * fpz));
        shen.setRotY((float) (0.04f * ClientEventHandler.recoilHorizon * fpz));
        shen.setRotZ((float) ((0.08f + 0.1 * fr) * ClientEventHandler.recoilHorizon));

        shen.setPosX((float) (shen.getPosX() * (1 - 0.5 * zt)));
        shen.setPosY((float) (shen.getPosY() * (-1 + 0.4 * zt)));
        shen.setPosZ((float) (shen.getPosZ() * (1 - 0.6 * zt)));
        shen.setRotX((float) (shen.getRotX() * (1 - 0.9 * zt)));
        shen.setRotY((float) (shen.getRotY() * (1 - 0.9 * zt)));
        shen.setRotZ((float) (shen.getRotZ() * (1 - 0.9 * zt)));

        CrossHairOverlay.gunRot = shen.getRotZ();

        tiba.setRotZ((float) (-0.25f * fp + 0.4 * fr));

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        GeoBone camera = getAnimationProcessor().getBone("camera");
        GeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.88 * zt);
        float numP = (float) (1 - 0.28 * zt);

        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 1f, 0.45f);
        GeoBone shell = getAnimationProcessor().getBone("shell");

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
            shell.setScaleX(0);
            shell.setScaleY(0);
            shell.setScaleZ(0);
        } else {
            shell.setScaleX(1);
            shell.setScaleY(1);
            shell.setScaleZ(1);
        }
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
