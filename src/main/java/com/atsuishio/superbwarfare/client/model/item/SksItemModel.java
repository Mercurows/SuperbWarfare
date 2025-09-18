package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.rifle.SksItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class SksItemModel extends CustomGunModel<SksItem> {

    @Override
    public ResourceLocation getAnimationResource(SksItem animatable) {
        return Mod.loc("animations/sks.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(SksItem animatable) {
        return Mod.loc("geo/sks.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SksItem animatable) {
        return Mod.loc("textures/item/sks.png");
    }

    @Override
    public ResourceLocation getLODModelResource(SksItem animatable) {
        return Mod.loc("geo/lod/sks.geo.json");
    }

    @Override
    public ResourceLocation getLODTextureResource(SksItem animatable) {
        return Mod.loc("textures/item/lod/sks.png");
    }

    @Override
    public void setCustomAnimations(SksItem animatable, long instanceId, AnimationState<SksItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        GeoBone gun = getAnimationProcessor().getBone("bone");
        GeoBone bolt = getAnimationProcessor().getBone("bolt");
        GeoBone shuan = getAnimationProcessor().getBone("bolt2");

        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;
        double fp = ClientEventHandler.firePos;

        gun.setPosX(1.53f * (float) zp);
        gun.setPosY(0.34f * (float) zp - (float) (0.6f * zpz));
        gun.setPosZ(2.5f * (float) zp + (float) (0.5f * zpz));
        gun.setRotZ((float) (0.05f * zpz));

        var data = GunData.from(stack);

        GeoBone shen = getAnimationProcessor().getBone("shen");

        ClientEventHandler.handleShootAnimation(shen, 1, -1, 1, 1, 1, 1, 0.5f, 0.8f);

        CrossHairOverlay.gunRot = shen.getRotZ();

        shuan.setPosZ(2f * (float) fp);

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 0, 0, 0, false);

        GeoBone camera = getAnimationProcessor().getBone("camera");
        GeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.92 * zt);
        float numP = (float) (1 - 0.88 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());

        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 0.7f, 1.2f);
        GeoBone shell = getAnimationProcessor().getBone("shell");

        if (data.holdOpen.get()) {
            shell.setScaleX(0);
            shell.setScaleY(0);
            shell.setScaleZ(0);
            bolt.setPosZ(2.5f);
        } else {
            shell.setScaleX(1);
            shell.setScaleY(1);
            shell.setScaleZ(1);
        }
    }
}
