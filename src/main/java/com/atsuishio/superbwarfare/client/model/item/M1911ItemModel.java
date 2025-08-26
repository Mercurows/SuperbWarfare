package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.handgun.M1911Item;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class M1911ItemModel extends CustomGunModel<M1911Item> {

    @Override
    public ResourceLocation getAnimationResource(M1911Item animatable) {
        return Mod.loc("animations/glock_17.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(M1911Item animatable) {
        return Mod.loc("geo/m_1911.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(M1911Item animatable) {
        return Mod.loc("textures/item/m_1911.png");
    }

    @Override
    public ResourceLocation getLODModelResource(M1911Item animatable) {
        return Mod.loc("geo/lod/m_1911.geo.json");
    }

    @Override
    public ResourceLocation getLODTextureResource(M1911Item animatable) {
        return Mod.loc("textures/item/lod/m_1911.png");
    }

    @Override
    public void setCustomAnimations(M1911Item animatable, long instanceId, AnimationState<M1911Item> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        GeoBone gun = getAnimationProcessor().getBone("bone");
        GeoBone bullet = getAnimationProcessor().getBone("bullet");
        GeoBone hammer = getAnimationProcessor().getBone("hammer");

        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;
        double fp = ClientEventHandler.firePos;

        gun.setPosX(1.23f * (float) zp);

        gun.setPosY(1.3f * (float) zp - (float) (0.2f * zpz));

        gun.setPosZ(7f * (float) zp + (float) (0.3f * zpz));

        gun.setScaleZ(1f - (0.55f * (float) zp));

        GeoBone body = getAnimationProcessor().getBone("gun");

        ClientEventHandler.handleShootAnimation(body, 1.25f, -2f, 1.6f, 5f, 1.3f, 1f, 0.2f, 1);

        CrossHairOverlay.gunRot = body.getRotZ();
        hammer.setRotX(60 * Mth.DEG_TO_RAD + (120 * Mth.DEG_TO_RAD * (float) fp));

        GeoBone huatao = getAnimationProcessor().getBone("huatao");
        huatao.setPosZ(2.75f * (float) ClientEventHandler.firePos);
        if (GunData.from(stack).holdOpen.get()) {
            huatao.setPosZ(1.5f);
        }

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 2, 2, 3, false);

        GeoBone camera = getAnimationProcessor().getBone("camera");
        GeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.12 * zt);
        float numP = (float) (1 - 0.68 * zt);

        var data = GunData.from(stack);
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

        if (GunData.from(stack).holdOpen.get()) {
            bullet.setScaleX(0);
            bullet.setScaleY(0);
            bullet.setScaleZ(0);

            shell.setScaleX(0);
            shell.setScaleY(0);
            shell.setScaleZ(0);
        } else {
            bullet.setScaleX(1);
            bullet.setScaleY(1);
            bullet.setScaleZ(1);

            shell.setScaleX(1);
            shell.setScaleY(1);
            shell.setScaleZ(1);
        }
    }
}
