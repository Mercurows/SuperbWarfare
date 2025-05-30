package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.FireMode;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.handgun.Glock18Item;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class Glock18ItemModel extends CustomGunModel<Glock18Item> {

    public static float fireRotY = 0f;
    public static float fireRotZ = 0f;

    @Override
    public ResourceLocation getAnimationResource(Glock18Item animatable) {
        return Mod.loc("animations/glock_17.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Glock18Item animatable) {
        return Mod.loc("geo/glock18.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Glock18Item animatable) {
        return Mod.loc("textures/item/glock_17.png");
    }

    @Override
    public void setCustomAnimations(Glock18Item animatable, long instanceId, AnimationState<Glock18Item> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone slide = getAnimationProcessor().getBone("huatao");
        CoreGeoBone bullet = getAnimationProcessor().getBone("bullet");
        CoreGeoBone switch_ = getAnimationProcessor().getBone("kuaimanji");

        var data = GunData.from(stack);
        var mode = data.fireMode.get();
        if (mode == FireMode.SEMI) {
            switch_.setRotX(35 * Mth.DEG_TO_RAD);
        }
        if (mode == FireMode.AUTO) {
            switch_.setRotX(0);
        }

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;
        double fpz = ClientEventHandler.firePosZ * 13 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        gun.setPosX(1.23f * (float) zp);

        gun.setPosY(1.43f * (float) zp - (float) (0.2f * zpz));

        gun.setPosZ(7f * (float) zp + (float) (0.3f * zpz));

        gun.setScaleZ(1f - (0.55f * (float) zp));

        CoreGeoBone body = getAnimationProcessor().getBone("gun");

        fireRotY = (float) Mth.lerp(0.5f * times, fireRotY, 0.3f * ClientEventHandler.recoilHorizon * fpz);
        fireRotZ = (float) Mth.lerp(2f * times, fireRotZ, (0.4f + 0.5 * fpz) * ClientEventHandler.recoilHorizon);

        body.setPosX(-0.4f * (float) (ClientEventHandler.recoilHorizon * (0.5 + 0.4 * ClientEventHandler.fireSpread)));
        body.setPosY((float) (0.15f * fp + 0.18f * fr));
        body.setPosZ((float) (1.935 * fp + 0.16f * fr + 0.925 * fpz));
        body.setRotX((float) (0.08f * fp + 0.1f * fr + 0.35f * fpz));
        body.setRotY(fireRotY);
        body.setRotZ(fireRotZ);

        body.setPosX((float) (body.getPosX() * (1 - 0.4 * zt)));
        body.setPosY((float) (body.getPosY() * (-1 + 0.5 * zt)));
        body.setPosZ((float) (body.getPosZ() * (1 - 0.3 * zt)));
        body.setRotX((float) (body.getRotX() * (1 - 0.8 * zt)));
        body.setRotY((float) (body.getRotY() * (1 - 0.7 * zt)));
        body.setRotZ((float) (body.getRotZ() * (1 - 0.65 * zt)));

        CrossHairOverlay.gunRot = body.getRotZ();

        slide.setPosZ(1.5f * (float) fp);

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.12 * zt);
        float numP = (float) (1 - 0.68 * zt);

        if (GunData.from(stack).reload.time() > 0) {
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
        if (GunData.from(stack).holdOpen.get()) {
            slide.setPosZ(1.5f);
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
