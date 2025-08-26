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
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

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
    public ResourceLocation getLODModelResource(Glock18Item animatable) {
        return Mod.loc("geo/lod/glock_17.geo.json");
    }

    @Override
    public ResourceLocation getLODTextureResource(Glock18Item animatable) {
        return Mod.loc("textures/item/lod/glock_17.png");
    }

    @Override
    public void setCustomAnimations(Glock18Item animatable, long instanceId, AnimationState<Glock18Item> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        GeoBone gun = getAnimationProcessor().getBone("bone");
        GeoBone bullet = getAnimationProcessor().getBone("bullet");
        GeoBone switch_ = getAnimationProcessor().getBone("kuaimanji");

        var data = GunData.from(stack);
        var mode = data.fireMode.get();
        if (mode == FireMode.SEMI) {
            switch_.setRotX(35 * Mth.DEG_TO_RAD);
        }
        if (mode == FireMode.AUTO) {
            switch_.setRotX(0);
        }

        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        gun.setPosX(1.23f * (float) zp);

        gun.setPosY(1.43f * (float) zp - (float) (0.2f * zpz));

        gun.setPosZ(7f * (float) zp + (float) (0.3f * zpz));

        gun.setScaleZ(1f - (0.55f * (float) zp));

        GeoBone body = getAnimationProcessor().getBone("gun");

        ClientEventHandler.handleShootAnimation(body, 1.25f, -2.5f, 1.35f, 4.5f, 1.3f, 1f, 0.2f, 1);

        CrossHairOverlay.gunRot = body.getRotZ();

        GeoBone huatao = getAnimationProcessor().getBone("huatao");
        huatao.setPosZ(1.5f * (float) ClientEventHandler.firePos);
        if (GunData.from(stack).holdOpen.get()) {
            huatao.setPosZ(1.5f);
        }

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        GeoBone camera = getAnimationProcessor().getBone("camera");
        GeoBone main = getAnimationProcessor().getBone("0");

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

        GeoBone shell = getAnimationProcessor().getBone("shell");
        GeoBone barrel = getAnimationProcessor().getBone("guan");
        if (GunData.from(stack).holdOpen.get()) {
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
