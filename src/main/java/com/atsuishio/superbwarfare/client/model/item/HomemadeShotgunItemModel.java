package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.shotgun.HomemadeShotgunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class HomemadeShotgunItemModel extends CustomGunModel<HomemadeShotgunItem> {

    @Override
    public ResourceLocation getAnimationResource(HomemadeShotgunItem animatable) {
        return Mod.loc("animations/homemade_shotgun.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(HomemadeShotgunItem animatable) {
        return Mod.loc("geo/homemade_shotgun.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HomemadeShotgunItem animatable) {
        return Mod.loc("textures/item/homemade_shotgun.png");
    }

    @Override
    public ResourceLocation getLODModelResource(HomemadeShotgunItem animatable) {
        return Mod.loc("geo/lod/homemade_shotgun.geo.json");
    }

    @Override
    public ResourceLocation getLODTextureResource(HomemadeShotgunItem animatable) {
        return Mod.loc("textures/item/homemade_shotgun.png");
    }

    @Override
    public void setCustomAnimations(HomemadeShotgunItem animatable, long instanceId, AnimationState<HomemadeShotgunItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        GeoBone gun = getAnimationProcessor().getBone("bone");
        GeoBone fireRoot = getAnimationProcessor().getBone("fireRoot");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getTimer().getRealtimeDeltaTicks(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        double fpz = ClientEventHandler.firePosZ * 7 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        gun.setPosX(3.725f * (float) zp);
        gun.setPosY(1.5f * (float) zp - (float) (0.4f * zpz));
        gun.setPosZ(1.2f * (float) zp + (float) (0.3f * zpz));
        gun.setRotZ((float) (0.05f * zpz));

        fireRoot.setPosX((float) (0.95f * ClientEventHandler.recoilHorizon * fpz * fp));
        fireRoot.setPosY((float) (0.4f * fp + 0.44f * fr));
        fireRoot.setPosZ((float) (5.825 * fp + 0.34f * fr + 2.35 * fpz));
        fireRoot.setRotX((float) (0.01f * fp + 0.15f * fr + 0.01f * fpz));
        fireRoot.setRotY((float) (0.1f * ClientEventHandler.recoilHorizon * fpz));
        fireRoot.setRotZ((float) ((0.08f + 0.1 * fr) * ClientEventHandler.recoilHorizon));

        CrossHairOverlay.gunRot = fireRoot.getRotZ();

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        GeoBone camera = getAnimationProcessor().getBone("camera");
        GeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.42 * zt);
        float numP = (float) (1 - 0.48 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
