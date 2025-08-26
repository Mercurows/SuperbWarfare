package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.rifle.InsidiousItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class InsidiousItemModel extends CustomGunModel<InsidiousItem> {

    public static float fireRotY = 0f;
    public static float fireRotZ = 0f;

    @Override
    public ResourceLocation getAnimationResource(InsidiousItem animatable) {
        return Mod.loc("animations/insidious.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(InsidiousItem animatable) {
        return Mod.loc("geo/insidious.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(InsidiousItem animatable) {
        return Mod.loc("textures/item/insidious.png");
    }

    @Override
    public ResourceLocation getLODModelResource(InsidiousItem animatable) {
        return Mod.loc("geo/lod/insidious.geo.json");
    }

    @Override
    public ResourceLocation getLODTextureResource(InsidiousItem animatable) {
        return Mod.loc("textures/item/lod/insidious.png");
    }

    @Override
    public void setCustomAnimations(InsidiousItem animatable, long instanceId, AnimationState<InsidiousItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        GeoBone gun = getAnimationProcessor().getBone("bone");

        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        gun.setPosX(4.23f * (float) zp);
        gun.setPosY(1.28f * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(6.2f * (float) zp + (float) (0.5f * zpz));
        gun.setScaleZ(1f - (0.82f * (float) zp));

        GeoBone shen = getAnimationProcessor().getBone("fireRootNormal");

        ClientEventHandler.handleShootAnimation(shen, 1, -2, 1, 1, 1, 1, 0.75f, 0.8f);

        CrossHairOverlay.gunRot = shen.getRotZ();
        ClientEventHandler.gunRootMove(getAnimationProcessor());

        GeoBone camera = getAnimationProcessor().getBone("camera");
        GeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.98 * zt);
        float numP = (float) (1 - 0.92 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
