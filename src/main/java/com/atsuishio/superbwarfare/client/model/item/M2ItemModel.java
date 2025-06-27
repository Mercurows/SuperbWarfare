package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.machinegun.M2HBItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class M2ItemModel extends CustomGunModel<M2HBItem> {

    public static float fireRotY = 0f;
    public static float fireRotZ = 0f;
    @Override
    public ResourceLocation getAnimationResource(M2HBItem animatable) {
        return Mod.loc("animations/m_2.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(M2HBItem animatable) {
        return Mod.loc("geo/m_2_hb.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(M2HBItem animatable) {
        return Mod.loc("textures/item/m_2_hb.png");
    }

    @Override
    public ResourceLocation getLODModelResource(M2HBItem animatable) {
        return Mod.loc("geo/lod/m_2_hb.geo.json");
    }

    @Override
    public ResourceLocation getLODTextureResource(M2HBItem animatable) {
        return Mod.loc("textures/item/lod/m_2_hb.png");
    }

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

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);

        double fpz = ClientEventHandler.firePosZ * 4 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;


        CoreGeoBone shen = getAnimationProcessor().getBone("fireRootNormal");

        fireRotY = (float) Mth.lerp(0.3f * times, fireRotY, 0.6f * ClientEventHandler.recoilHorizon * fpz);
        fireRotZ = (float) Mth.lerp(2f * times, fireRotZ, (0.4f + 0.5f * fpz) * ClientEventHandler.recoilHorizon);

        shen.setPosX(-0.7f * (float) (ClientEventHandler.recoilHorizon * (0.5 + 0.4 * ClientEventHandler.fireSpread)));
        shen.setPosY((float) (0.4f * fp + 0.44f * fr));
        shen.setPosZ((float) (2.825 * fp + 0.24f * fr + 1.25 * fpz));
        shen.setRotX((float) (0.01f * fp + 0.08f * fr + 0.01f * fpz));
        shen.setRotY(fireRotY);
        shen.setRotZ(fireRotZ);

        CrossHairOverlay.gunRot = shen.getRotZ();

        ClientEventHandler.gunRootMove(getAnimationProcessor());
        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 1f, 0.45f);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
