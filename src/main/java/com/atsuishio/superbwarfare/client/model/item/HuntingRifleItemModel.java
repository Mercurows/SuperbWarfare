package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.sniper.HuntingRifleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class HuntingRifleItemModel extends CustomGunModel<HuntingRifleItem> {

    @Override
    public ResourceLocation getAnimationResource(HuntingRifleItem animatable) {
        return Mod.loc("animations/hunting_rifle.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(HuntingRifleItem animatable) {
        return Mod.loc("geo/hunting_rifle.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HuntingRifleItem animatable) {
        return Mod.loc("textures/item/hunting_rifle.png");
    }

    @Override
    public ResourceLocation getLODModelResource(HuntingRifleItem animatable) {
        return Mod.loc("geo/lod/hunting_rifle.geo.json");
    }

    @Override
    public ResourceLocation getLODTextureResource(HuntingRifleItem animatable) {
        return Mod.loc("textures/item/lod/hunting_rifle.png");
    }

    @Override
    public void setCustomAnimations(HuntingRifleItem animatable, long instanceId, AnimationState<HuntingRifleItem> animationState) {
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

        gun.setPosX(1.975f * (float) zp);
        gun.setPosY(1.2f * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(4f * (float) zp + (float) (0.5f * zpz));
        gun.setRotZ((float) (0.05f * zpz));
        gun.setScaleZ(1f - (0.5f * (float) zp));

        fireRoot.setPosX((float) (0.95f * ClientEventHandler.recoilHorizon * fpz * fp));
        fireRoot.setPosY((float) (0.4f * fp + 0.44f * fr));
        fireRoot.setPosZ((float) (2.825 * fp + 0.17f * fr + 1.175 * fpz));
        fireRoot.setRotX((float) (0.01f * fp + 0.2f * fr + 0.01f * fpz));
        fireRoot.setRotY((float) (0.1f * ClientEventHandler.recoilHorizon * fpz));
        fireRoot.setRotZ((float) ((0.08f + 0.1 * fr) * ClientEventHandler.recoilHorizon));

        fireRoot.setPosX((float) (fireRoot.getPosX() * (1 - 0.4 * zt)));
        fireRoot.setPosY((float) (fireRoot.getPosY() * (1 - 0.5 * zt)));
        fireRoot.setPosZ((float) (fireRoot.getPosZ() * (1 - 0.7 * zt)));
        fireRoot.setRotX((float) (fireRoot.getRotX() * (1 - 0.87 * zt)));
        fireRoot.setRotY((float) (fireRoot.getRotY() * (1 - 0.7 * zt)));
        fireRoot.setRotZ((float) (fireRoot.getRotZ() * (1 - 0.65 * zt)));

        CrossHairOverlay.gunRot = fireRoot.getRotZ();

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        GeoBone camera = getAnimationProcessor().getBone("camera");
        GeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.82 * zt);
        float numP = (float) (1 - 0.78 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
