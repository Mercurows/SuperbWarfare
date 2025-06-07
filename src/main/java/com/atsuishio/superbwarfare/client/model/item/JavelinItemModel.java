package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.launcher.JavelinItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class JavelinItemModel extends CustomGunModel<JavelinItem> {

    @Override
    public ResourceLocation getAnimationResource(JavelinItem animatable) {
        return Mod.loc("animations/javelin.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(JavelinItem animatable) {
        return Mod.loc("geo/javelin.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(JavelinItem animatable) {
        return Mod.loc("textures/item/javelin.png");
    }

    @Override
    public ResourceLocation getLODModelResource(JavelinItem animatable) {
        return Mod.loc("geo/lod/javelin.geo.json");
    }

    @Override
    public ResourceLocation getLODTextureResource(JavelinItem animatable) {
        return Mod.loc("textures/item/lod/javelin.png");
    }

    @Override
    public void setCustomAnimations(JavelinItem animatable, long instanceId, AnimationState<JavelinItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        GeoBone gun = getAnimationProcessor().getBone("bone");
        GeoBone shen = getAnimationProcessor().getBone("shen");
        GeoBone javelin = getAnimationProcessor().getBone("javelin");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getTimer().getRealtimeDeltaTicks(), 0.8);
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        double fpz = ClientEventHandler.firePosZ * 13 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        gun.setPosX(1.66f * (float) zp + (float) (0.2f * zpz));
        gun.setPosY(5.5f * (float) zp + (float) (0.8f * zpz));
        gun.setPosZ(15.9f * (float) zp);
        gun.setScaleZ(1f - (0.8f * (float) zp));
        gun.setRotZ(-4.75f * Mth.DEG_TO_RAD * (float) zp + (float) (0.02f * zpz));

        javelin.setHidden(zp > 0.8);

        shen.setPosX((float) (0.75f * ClientEventHandler.recoilHorizon * fpz * fp));
        shen.setPosY((float) (-0.03f * fp - 0.06f * fr));
        shen.setPosZ((float) (0.725 * fp + 0.34f * fr + 0.95 * fpz));
        shen.setRotX((float) (0.03f * fp + 0.02f * fr + 0.02f * fpz));
        shen.setRotY((float) (0.07f * ClientEventHandler.recoilHorizon * fpz));
        shen.setRotZ((float) ((0.08f + 0.1 * fr) * ClientEventHandler.recoilHorizon));

        CrossHairOverlay.gunRot = shen.getRotZ();

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        GeoBone camera = getAnimationProcessor().getBone("camera");
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
